package TRFM;

import TRFM.AssetModels.BondPosition;
import TRFM.AssetModels.OptionPricingService;
import TRFM.AssetModels.PortfolioOption;
import TRFM.AssetModels.StockPosition;
import TRFM.Interpolation.CubicSplineVolSurfaceInterpolator;
import TRFM.Interpolation.VolInterpolationMethod;
import TRFM.Interpolation.VolSurfaceInterpolator;
import TRFM.VolatilityModels.*;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class RiskFactorPipelineApp
{

    public static void main(String[] args) throws Exception
    {
        runOneDayVarDemo();
    }


    private static VolatilityModel createVolatilityModel(VolatilityModelChoice volModelChoice, boolean useStudent, double lambda)
    {
        GarchDistribution distribution = useStudent ? GarchDistribution.STUDENT_T : GarchDistribution.NORMAL;

        if (volModelChoice == VolatilityModelChoice.GJR)
        {
            return new GjrGarchVolatilityModel(distribution);
        }

        if (volModelChoice == VolatilityModelChoice.GARCH)
        {
            return new GarchVolatilityModel(distribution);
        }

        if (volModelChoice == VolatilityModelChoice.EWMA)
        {
            return new EwmaVolatilityModel(lambda);
        }
        System.err.println("Unknown volatility model: " + volModelChoice);
        return new EwmaVolatilityModel(lambda);
    }

    private static void exportVolSlicesCsv(String fileName, double[] currentNodeVols, VolSurfaceConfig volSurfaceConfig,
                                           double[] maturityDaysList, double moneynessMin, double moneynessMax,
                                           double moneynessStep) throws IOException
    {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName)))
        {
            pw.println("maturityDays,moneynessPct,bilinearVol,cubicSplineVol,isNode,nodeVol");
            List<String> nodeCols = volSurfaceConfig.getNodeColumns();

            for (double maturityDays : maturityDaysList)
            {
                for (double m = moneynessMin; m <= moneynessMax + 1e-12; m += moneynessStep)
                {
                    double bilinear = VolSurfaceInterpolator.interpolateVol(currentNodeVols, volSurfaceConfig, m, maturityDays);
                    double cubic = CubicSplineVolSurfaceInterpolator.interpolateVol(currentNodeVols, volSurfaceConfig, m, maturityDays);

                    boolean isNode = false;
                    double nodeVol = Double.NaN;

                    for (int i = 0; i < nodeCols.size(); i++)
                    {
                        String col = nodeCols.get(i);
                        String[] parts = col.split("_");
                        if (parts.length < 3) continue;

                        String tenorLabel = parts[1].toLowerCase(Locale.ROOT);
                        double nodeDays = tenorLabelToDays(tenorLabel);
                        double nodeMoneyness = Double.parseDouble(parts[2]);

                        if (Math.abs(nodeDays - maturityDays) < 1e-8 && Math.abs(nodeMoneyness - m) < 1e-8)
                        {
                            isNode = true;
                            nodeVol = currentNodeVols[i];
                            break;
                        }
                    }

                    pw.printf(Locale.US, "%.6f,%.6f,%.10f,%.10f,%s,%.10f%n", maturityDays, m, bilinear, cubic, Boolean.toString(isNode), nodeVol);
                }
            }
        }

        System.out.println("Exported vol slices CSV to: " + fileName);
    }

    private static double tenorLabelToDays(String tenorLabel)
    {
        if (tenorLabel.endsWith("d"))
        {
            return Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
        }
        if (tenorLabel.endsWith("m"))
        {
            double months = Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
            return months * 365.0 / 12.0;
        }
        if (tenorLabel.endsWith("y"))
        {
            double years = Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
            return years * 365.0;
        }

        throw new IllegalArgumentException("Unknown tenor label: " + tenorLabel);
    }

    private static class ScenarioErrorRow
    {
        private final int scenarioIndex;
        private final FhsScenario scenario;
        private final double taylorPnL;
        private final double fullPnL;
        private final double error;
        private final double absError;

        private ScenarioErrorRow(int scenarioIndex,
                                 FhsScenario scenario,
                                 double taylorPnL,
                                 double fullPnL,
                                 double error,
                                 double absError)
        {
            this.scenarioIndex = scenarioIndex;
            this.scenario = scenario;
            this.taylorPnL = taylorPnL;
            this.fullPnL = fullPnL;
            this.error = error;
            this.absError = absError;
        }
    }

    private static class InstrumentTaylorBreakdown
    {
        private final String instrumentId;
        private final double deltaPnL;
        private final double gammaPnL;
        private final double fxPnL;
        private final double linearFactorPnL;
        private final double quadraticFactorPnL;
        private final double crossFactorPnL;
        private final double totalPnL;

        private InstrumentTaylorBreakdown(String instrumentId, double deltaPnL, double gammaPnL, double fxPnL, double linearFactorPnL,
                                          double quadraticFactorPnL, double crossFactorPnL, double totalPnL)
        {
            this.instrumentId = instrumentId;
            this.deltaPnL = deltaPnL;
            this.gammaPnL = gammaPnL;
            this.fxPnL = fxPnL;
            this.linearFactorPnL = linearFactorPnL;
            this.quadraticFactorPnL = quadraticFactorPnL;
            this.crossFactorPnL = crossFactorPnL;
            this.totalPnL = totalPnL;
        }
    }



    private static void runOneDayVarDemo() {
        int nDays = 500;
        int nScenarios = 5000;
        double spot = 100.0;
        double quantity = 100.0;

        // Synthetic daily log returns
        Random random = new Random(42L);
        double[][] returns = new double[nDays][1];
        List<LocalDate> dates = new ArrayList<>();

        for (int i = 0; i < nDays; i++) {
            returns[i][0] = 0.012 * random.nextGaussian();
            dates.add(LocalDate.of(2024, 1, 1).plusDays(i));
        }

        List<String> factors = List.of(FactorIds.spot("DEMO"));

        RiskFactorMatrix matrix =
                new RiskFactorMatrix(dates, factors, returns);

        // EWMA volatility calibration
        VolatilityModel model = new EwmaVolatilityModel(0.94);
        var volResult = model.estimate(matrix);
        double[] nextSigma =
                model.forecastNextSigma(matrix, volResult);

        ResidualMatrix residuals =
                ResidualBuilder.build(matrix, volResult);

        // State-independent filtered historical simulation
        FhsScenarioGenerator generator =
                new FhsScenarioGenerator(residuals, 80L);

        List<FhsScenario> scenarios =
                generator.drawScenarios(nextSigma, nScenarios);

        // A simple portfolio of 100 shares
        OptionGreekRow position = new OptionGreekRow(
                "DEMO_STOCK", "DEMO", FxIds.NO_FX_ID,
                quantity, 1.0, 0.0, 0.0,
                Map.of(), Map.of(), Map.of()
        );

        Portfolio portfolio = new Portfolio(List.of(position));

        double[] pnls = PortfolioPnLCalculator.computePnLDistribution(
                portfolio, factors, scenarios,
                Map.of("DEMO", spot), Map.of()
        );

        VarResult var95 = VarCalculator.computeHistoricalVar(pnls, 0.95);
        VarResult var99 = VarCalculator.computeHistoricalVar(pnls, 0.99);

        System.out.println("=== 1-DAY VaR DEMO ===");
        System.out.println("Synthetic data: " + nDays + " observations");
        System.out.println("Portfolio value: " + (spot * quantity));
        System.out.printf("1-day VaR 95%%: %.2f%n", var95.getVar());
        System.out.printf("1-day VaR 99%%: %.2f%n", var99.getVar());
    }


}

/** ---------------- RAW INPUT ---------------- */

// See MarketObservation


class Portfolio
{
    private final List<OptionGreekRow> positions;

    public Portfolio(List<OptionGreekRow> positions)
    {
        this.positions = positions;
    }

    public List<OptionGreekRow> getPositions()
    {
        return positions;
    }

    public int size()
    {
        return positions.size();
    }
}

class PortfolioPnLCalculator
{

    public static double computeScenarioPnL(Portfolio portfolio, List<String> factorNames, double[] scenarioShock, Map<String,
            Double> currentSpotById, Map<String, Double> currentFxById)
    {
        if (factorNames.size() != scenarioShock.length)
        {
            throw new IllegalArgumentException("Factor names och scenarioShock har olika dimension.");
        }

        Map<String, Double> shockByFactor = new HashMap<>();
        for (int i = 0; i < factorNames.size(); i++)
        {
            shockByFactor.put(factorNames.get(i), scenarioShock[i]);
        }

        double portfolioPnL = 0.0;

        for (OptionGreekRow row : portfolio.getPositions())
        {
            double pnl = 0.0;

            String spotFactor = FactorIds.spot(row.getSpotId());

            double dFx = 0.0;
            String fxFactor = null;

            if (!FxIds.isNoFx(row.getFxId()))
            {
                fxFactor = FactorIds.fx(row.getFxId());
                double fxLogRet = shockByFactor.getOrDefault(fxFactor, 0.0);
                double currentFx = currentFxById.getOrDefault(row.getFxId(), 1.0);
                dFx = currentFx * (Math.exp(fxLogRet) - 1.0);

                pnl += row.getFxDelta() * dFx; // FX delta part calc
            }

            double spotLogRet = shockByFactor.getOrDefault(spotFactor, 0.0);
            double currentSpot = currentSpotById.getOrDefault(row.getSpotId(), 0.0);
            double dS = currentSpot * (Math.exp(spotLogRet) - 1.0);

            pnl += row.getDelta() * dS; // delta part calc
            pnl += 0.5 * row.getGamma() * dS * dS; // gamma part calc

            for (String factor : factorNames)
            {
                if (factor.equals(spotFactor) || factor.equals(fxFactor))
                {
                    continue;
                }

                double shock = shockByFactor.getOrDefault(factor, 0.0);

                pnl += row.getSensitivity(factor) * shock;
                pnl += 0.5 * row.getSecondOrderSensitivity(factor) * shock * shock;
                pnl += row.getSpotCrossSensitivity(factor) * dS * shock;
            }

            portfolioPnL += row.getQuantity() * pnl;
        }

        return portfolioPnL;
    }



    public static double[] computePnLDistribution(Portfolio portfolio, List<String> factorNames, List<FhsScenario> scenarios,
                                                  Map<String, Double> currentSpotById, Map<String, Double> currentFxById)
    {
        double[] pnls = new double[scenarios.size()];

        for (int i = 0; i < scenarios.size(); i++)
        {
            pnls[i] = computeScenarioPnL(portfolio, factorNames, scenarios.get(i).getSimulatedShock(), currentSpotById, currentFxById);
        }

        return pnls;
    }
}

class VarResult
{
    private final double confidenceLevel;
    private final double var;
    private final double[] pnlDistribution;

    public VarResult(double confidenceLevel, double var, double[] pnlDistribution)
    {
        this.confidenceLevel = confidenceLevel;
        this.var = var;
        this.pnlDistribution = pnlDistribution;
    }

    public double getConfidenceLevel()
    {
        return confidenceLevel;
    }

    public double getVar()
    {
        return var;
    }

    public double[] getPnlDistribution()
    {
        return pnlDistribution;
    }
}

class VarCalculator
{

    /**
     * Beräknar VaR från PnL-fördelning.
     * confidenceLevel = 0.99 betyder 99% VaR = negativ 1%-kvantil av PnL.
     */
    public static VarResult computeHistoricalVar(double[] pnlDistribution, double confidenceLevel) {
        if (pnlDistribution.length == 0)
        {
            throw new IllegalArgumentException("PnL-fördelningen är tom.");
        }

        double[] sorted = Arrays.copyOf(pnlDistribution, pnlDistribution.length);
        Arrays.sort(sorted);

        double alpha = 1.0 - confidenceLevel;
        int index = (int) Math.floor(alpha * sorted.length);

        if (index < 0) index = 0;
        if (index >= sorted.length) index = sorted.length - 1;

        double quantilePnL = sorted[index];

        // VaR rapporteras positivt som förlustmått
        double var = -quantilePnL;

        return new VarResult(confidenceLevel, var, pnlDistribution);
    }
}

class MixedPortfolio
{
    private final List<PortfolioOption> options;
    private final List<StockPosition> stocks;
    private final List<BondPosition> bonds;

    public MixedPortfolio(List<PortfolioOption> options, List<StockPosition> stocks)
    {
        this(options, stocks, Collections.emptyList());
    }

    public MixedPortfolio(List<PortfolioOption> options, List<StockPosition> stocks, List<BondPosition> bonds)
    {
        this.options = options == null ? Collections.emptyList() : options;
        this.stocks = stocks == null ? Collections.emptyList() : stocks;
        this.bonds = bonds == null ? Collections.emptyList() : bonds;
    }

    public List<PortfolioOption> getOptions()
    {
        return options;
    }

    public List<StockPosition> getStocks()
    {
        return stocks;
    }

    public List<BondPosition> getBonds()
    {
        return bonds;
    }




}




