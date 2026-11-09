package TRFM;

import TRFM.AssetModels.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

public class FullRepricingModel {}

class ScenarioMarketState
{
    private final Map<String, Double> spotById;
    private final Map<String, Double> fxById;
    private final Map<String, Double> rateByColumn;
    private final Map<String, double[]> nodeVolsBySurfaceId;

    public ScenarioMarketState(Map<String, Double> spotById, Map<String, Double> fxById, Map<String, Double> rateByColumn, Map<String, double[]> nodeVolsBySurfaceId)
    {
        this.spotById = spotById;
        this.fxById = fxById;
        this.rateByColumn = rateByColumn;
        this.nodeVolsBySurfaceId = nodeVolsBySurfaceId;
    }

    public double getSpot(String spotId)
    {
        Double v = spotById.get(spotId);
        if (v == null)
        {
            throw new IllegalArgumentException("Saknar spot för " + spotId);
        }
        return v;
    }

    public double getFx(String fxId)
    {
        if (FxIds.isNoFx(fxId))
        {
            return 1.0;
        }

        Double v = fxById.get(fxId);
        if (v == null)
        {
            throw new IllegalArgumentException("Saknar FX för " + fxId);
        }
        return v;
    }

    public double getRate(String rateColumn)
    {
        Double v = rateByColumn.get(rateColumn);
        if (v == null)
        {
            throw new IllegalArgumentException("Saknar rate för " + rateColumn);
        }
        return v;
    }

    public double[] getNodeVols(String volSurfaceId)
    {
        double[] v = nodeVolsBySurfaceId.get(volSurfaceId);
        if (v == null)
        {
            throw new IllegalArgumentException("Saknar node vols för " + volSurfaceId);
        }
        return Arrays.copyOf(v, v.length);
    }

    public Map<String, Double> getRateByColumn()
    {
        return rateByColumn;
    }

}

class ScenarioMarketShocker
{
    public static ScenarioMarketState shockMarket(ScenarioMarketState baseState,
                                                  List<String> factorNames,
                                                  double[] scenarioShock,
                                                  PipelineConfig config,
                                                  PcaModelRegistry pcaRegistry)
    {
        if (factorNames.size() != scenarioShock.length)
        {
            throw new IllegalArgumentException("factorNames och scenarioShock har olika dimension.");
        }

        Map<String, Double> shockByFactor = new LinkedHashMap<>();
        for (int i = 0; i < factorNames.size(); i++)
        {
            shockByFactor.put(factorNames.get(i), scenarioShock[i]);
        }

        Map<String, Double> shockedSpots = new LinkedHashMap<>();
        for (SpotRiskConfig spotCfg : config.getSpots())
        {
            double baseSpot = baseState.getSpot(spotCfg.getSpotId());
            double spotShock = shockByFactor.getOrDefault(FactorIds.spot(spotCfg.getSpotId()), 0.0);
            double shockedSpot = baseSpot * Math.exp(spotShock);
            shockedSpots.put(spotCfg.getSpotId(), shockedSpot);
        }

        Map<String, Double> shockedFxs = new LinkedHashMap<>();
        for (FxRiskConfig fxCfg : config.getFxs())
        {
            double baseFx = baseState.getFx(fxCfg.getFxId());
            double fxShock = shockByFactor.getOrDefault(FactorIds.fx(fxCfg.getFxId()), 0.0);
            double shockedFx = baseFx * Math.exp(fxShock);
            shockedFxs.put(fxCfg.getFxId(), shockedFx);
        }

        Map<String, Double> shockedRates = new LinkedHashMap<>();
        for (RateCurveConfig rateCurveCfg : config.getRateCurves())
        {
            for (String rateCol : rateCurveCfg.getRateColumns())
            {
                double baseRate = baseState.getRate(rateCol);
                double rateShock = shockByFactor.getOrDefault(rateCurveCfg.getFactorId(rateCol), 0.0);
                shockedRates.put(rateCol, baseRate + rateShock);
            }
        }

        Map<String, double[]> shockedNodeVolsBySurfaceId = new LinkedHashMap<>();
        for (VolSurfaceConfig volSurfaceCfg : config.getVolSurfaces())
        {
            double[] baseNodeVols = baseState.getNodeVols(volSurfaceCfg.getVolSurfaceId());
            PcaResult pca = pcaRegistry.get(volSurfaceCfg.getVolSurfaceId());

            double[] shockedNodeVols = shockNodeVolsFromPcShocks(baseNodeVols, volSurfaceCfg, pca, shockByFactor);
            shockedNodeVolsBySurfaceId.put(volSurfaceCfg.getVolSurfaceId(), shockedNodeVols);
        }

        return new ScenarioMarketState(shockedSpots, shockedFxs, shockedRates, shockedNodeVolsBySurfaceId);
    }

    private static double[] shockNodeVolsFromPcShocks(double[] baseNodeVols, VolSurfaceConfig volSurfaceConfig,
                                                      PcaResult pca,
                                                      Map<String, Double> shockByFactor)
    {
        double[][] loadings = pca.getLoadings();
        double[] stdDevs = pca.getStdDevs();

        int m = baseNodeVols.length;
        int k = loadings[0].length;

        if (loadings.length != m)
        {
            throw new IllegalArgumentException("Dimension mismatch mellan volyta och PCA.");
        }

        double[] shocked = new double[m];

        for (int j = 0; j < m; j++)
        {
            double totalLogVolShock = 0.0;

            for (int pc = 0; pc < k; pc++)
            {
                String factorName = FactorIds.volPc(volSurfaceConfig.getVolSurfaceId(), pc + 1);
                double pcShock = shockByFactor.getOrDefault(factorName, 0.0);

                totalLogVolShock += pcShock * loadings[j][pc] * stdDevs[j];
            }

            shocked[j] = baseNodeVols[j] * Math.exp(totalLogVolShock);
        }

        return shocked;
    }


    private static Map<String, Double> reconstructRateShocksFromPc(PcaResult ratePca,
                                                                   PipelineConfig config,
                                                                   Map<String, Double> shockByFactor)
    {
        Map<String, Double> result = new LinkedHashMap<>();

        String curveId = config.getRateCurveId();
        int k = ratePca.getLoadings()[0].length;
        double[][] loadings = ratePca.getLoadings();
        double[] stdDevs = ratePca.getStdDevs();

        for (int j = 0; j < ratePca.getInputColumns().length; j++)
        {
            double dr = 0.0;

            for (int pc = 0; pc < k; pc++)
            {
                double pcShock = shockByFactor.getOrDefault(FactorIds.ratePc(curveId, pc + 1), 0.0);
                dr += pcShock * stdDevs[j] * loadings[j][pc];
            }

            result.put(ratePca.getInputColumns()[j], dr);
        }

        return result;
    }

}

class FullRepricingPnLCalculator
{
    public static double computeScenarioPnL(OptionPortfolio optionPortfolio, ScenarioMarketState baseState,
                                            ScenarioMarketState shockedState, PipelineConfig config)
    {
        double totalPnL = 0.0;

        for (PortfolioOption option : optionPortfolio.getPositions())
        {
            double baseSpot = baseState.getSpot(option.getSpotId());
            double shockedSpot = shockedState.getSpot(option.getSpotId());

            double[] baseNodeVols = baseState.getNodeVols(option.getVolSurfaceId());
            double[] shockedNodeVols = shockedState.getNodeVols(option.getVolSurfaceId());

            VolSurfaceConfig volSurfaceConfig = findVolSurfaceConfig(config, option.getVolSurfaceId());

            OisCurve baseCurve = buildCurveFromState(baseState, config);
            OisCurve shockedCurve = buildCurveFromState(shockedState, config);

            double basePrice = OptionPricingService.price(option, baseSpot, baseNodeVols, volSurfaceConfig, baseCurve);
            double shockedPrice = OptionPricingService.price(option, shockedSpot, shockedNodeVols, volSurfaceConfig, shockedCurve);

            double pnl = option.getQuantity() * (shockedPrice - basePrice);
            totalPnL += pnl;
        }

        return totalPnL;
    }

    public static double[] computePnLDistribution(OptionPortfolio optionPortfolio, ScenarioMarketState baseState,
                                                  List<String> factorNames, List<FhsScenario> scenarios,
                                                  PipelineConfig config, PcaModelRegistry pcaRegistry)
    {
        double[] pnls = new double[scenarios.size()];

        for (int i = 0; i < scenarios.size(); i++)
        {
            ScenarioMarketState shockedState = ScenarioMarketShocker.shockMarket(baseState, factorNames,
                    scenarios.get(i).getSimulatedShock(), config, pcaRegistry);

            pnls[i] = computeScenarioPnL(optionPortfolio, baseState, shockedState, config);
        }

        return pnls;
    }

    private static OisCurve buildCurveFromState(ScenarioMarketState state, PipelineConfig config)
    {
        return new OisCurve(state.getRate("rate_1m"), state.getRate("rate_3m"),
                state.getRate("rate_6m"), state.getRate("rate_1y"), state.getRate("rate_2y"),
                state.getRate("rate_3y"), state.getRate("rate_4y"), state.getRate("rate_5y"));
    }

    private static VolSurfaceConfig findVolSurfaceConfig(PipelineConfig config, String volSurfaceId)
    {
        for (VolSurfaceConfig cfg : config.getVolSurfaces())
        {
            if (cfg.getVolSurfaceId().equals(volSurfaceId))
            {
                return cfg;
            }
        }
        throw new IllegalArgumentException("Kunde inte hitta volSurfaceConfig för " + volSurfaceId);
    }
}

class MixedFullRepricingPnLCalculator
{
    public static double[] computePnLDistribution(MixedPortfolio portfolio,
                                                  ScenarioMarketState baseState,
                                                  List<String> factorNames,
                                                  List<FhsScenario> scenarios,
                                                  PipelineConfig config,
                                                  PcaModelRegistry pcaRegistry)
    {
        double[] pnls = new double[scenarios.size()];

        for (int i = 0; i < scenarios.size(); i++)
        {
            FhsScenario scenario = scenarios.get(i);

            ScenarioMarketState shockedState = ScenarioMarketShocker.shockMarket(baseState, factorNames, scenario.getSimulatedShock(),
                    config, pcaRegistry);

            pnls[i] = computeScenarioPnL(portfolio, baseState, shockedState, config);
        }

        return pnls;
    }

    public static double computeScenarioPnL(MixedPortfolio portfolio,
                                            ScenarioMarketState baseState,
                                            ScenarioMarketState shockedState,
                                            PipelineConfig config)
    {
        double pnl = 0.0;

        for (PortfolioOption option : portfolio.getOptions())
        {
            pnl += computeOptionScenarioPnL(option, baseState, shockedState, config);
        }

        for (StockPosition stock : portfolio.getStocks())
        {
            pnl += computeStockScenarioPnL(stock, baseState, shockedState);
        }

        for (BondPosition bond : portfolio.getBonds())
        {
            pnl += computeBondScenarioPnL(bond, baseState, shockedState);
        }

        return pnl;
    }

    private static double[] computeFxSpotFullRepricingPnLDistribution(String fxId,
                                                                      double eurNotional,
                                                                      List<String> factorNames,
                                                                      List<FhsScenario> scenarios,
                                                                      Map<String, Double> currentFxById)
    {
        String fxFactor = FactorIds.fx(fxId);

        int fxIndex = factorNames.indexOf(fxFactor);
        if (fxIndex < 0)
        {
            throw new IllegalArgumentException("Hittar inte FX factor i factorNames: " + fxFactor);
        }

        double currentFx = currentFxById.get(fxId);

        double[] pnls = new double[scenarios.size()];

        for (int i = 0; i < scenarios.size(); i++)
        {
            double fxLogRet = scenarios.get(i).getSimulatedShock()[fxIndex];

            double shockedFx = currentFx * Math.exp(fxLogRet);

            /*
             * Full repricing:
             *
             * V0 = N_EUR * FX0
             * V1 = N_EUR * FX1
             * PnL = V1 - V0
             */
            double baseValue = eurNotional * currentFx;
            double shockedValue = eurNotional * shockedFx;

            pnls[i] = shockedValue - baseValue;
        }

        return pnls;
    }

    private static double computeOptionScenarioPnL(PortfolioOption option,
                                                   ScenarioMarketState baseState,
                                                   ScenarioMarketState shockedState,
                                                   PipelineConfig config)
    {
        double baseSpot = baseState.getSpot(option.getSpotId());
        double shockedSpot = shockedState.getSpot(option.getSpotId());

        double[] baseNodeVols = baseState.getNodeVols(option.getVolSurfaceId());
        double[] shockedNodeVols = shockedState.getNodeVols(option.getVolSurfaceId());

        VolSurfaceConfig volSurfaceConfig =
                findVolSurfaceConfigLocal(config, option.getVolSurfaceId());

        OisCurve baseCurve = buildCurveFromStateLocal(baseState);
        OisCurve shockedCurve = buildCurveFromStateLocal(shockedState);

        double basePrice = OptionPricingService.price(option, baseSpot, baseNodeVols, volSurfaceConfig, baseCurve);

        double shockedPrice = OptionPricingService.price(option, shockedSpot, shockedNodeVols, volSurfaceConfig, shockedCurve);

        return option.getQuantity() * (shockedPrice - basePrice);
    }

    private static double computeStockScenarioPnL(StockPosition stock,
                                                  ScenarioMarketState baseState,
                                                  ScenarioMarketState shockedState)
    {
        double baseSpot = baseState.getSpot(stock.getSpotId());
        double shockedSpot = shockedState.getSpot(stock.getSpotId());

        return stock.getQuantity() * (shockedSpot - baseSpot);
    }

    private static double computeBondScenarioPnL(BondPosition bond,
                                                 ScenarioMarketState baseState,
                                                 ScenarioMarketState shockedState)
    {
        OisCurve baseCurve = buildCurveFromStateLocal(baseState);
        OisCurve shockedCurve = buildCurveFromStateLocal(shockedState);

        double baseLocalPrice = BondPricingService.priceLocal(bond, baseCurve);
        double shockedLocalPrice = BondPricingService.priceLocal(bond, shockedCurve);

        double baseFx = baseState.getFx(bond.getFxId());
        double shockedFx = shockedState.getFx(bond.getFxId());

        double baseValue = baseFx * baseLocalPrice;
        double shockedValue = shockedFx * shockedLocalPrice;

        return bond.getQuantity() * (shockedValue - baseValue);
    }

    private static OisCurve buildCurveFromStateLocal(ScenarioMarketState state)
    {
        return new OisCurve(
                state.getRate("rate_1m"),
                state.getRate("rate_3m"),
                state.getRate("rate_6m"),
                state.getRate("rate_1y"),
                state.getRate("rate_2y"),
                state.getRate("rate_3y"),
                state.getRate("rate_4y"),
                state.getRate("rate_5y"));
    }

    private static VolSurfaceConfig findVolSurfaceConfigLocal(PipelineConfig config,
                                                              String volSurfaceId)
    {
        for (VolSurfaceConfig cfg : config.getVolSurfaces())
        {
            if (cfg.getVolSurfaceId().equals(volSurfaceId))
            {
                return cfg;
            }
        }

        throw new IllegalArgumentException("Kunde inte hitta volSurfaceConfig för " + volSurfaceId);
    }
}


