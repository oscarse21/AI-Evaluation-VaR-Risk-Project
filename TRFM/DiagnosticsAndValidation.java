package TRFM;

import TRFM.AssetModels.OptionPortfolio;
import TRFM.AssetModels.OptionPricingService;
import TRFM.AssetModels.OptionType;
import TRFM.AssetModels.PortfolioOption;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class DiagnosticsAndValidation {}

class PortfolioTestSetFactory
{
    private static class OptionTemplate
    {
        private final OptionType optionType;
        private final double strikeMultiplier;
        private final double maturityDays;

        private OptionTemplate(OptionType optionType, double strikeMultiplier, double maturityDays)
        {
            this.optionType = optionType;
            this.strikeMultiplier = strikeMultiplier;
            this.maturityDays = maturityDays;
        }
    }

    public static List<NamedOptionPortfolio> buildPortfolioSet(double currentSpot, int portfoliosPerFamily, long seed)
    {
        Random rnd = new Random(seed);

        List<OptionTemplate> universe = buildUniverse();
        List<NamedOptionPortfolio> result = new ArrayList<>();

        result.addAll(buildFamily("BALANCED", currentSpot, portfoliosPerFamily, universe, rnd));
        result.addAll(buildFamily("SHORT_DATED", currentSpot, portfoliosPerFamily, universe, rnd));
        result.addAll(buildFamily("LONG_DATED", currentSpot, portfoliosPerFamily, universe, rnd));
        result.addAll(buildFamily("OTM_HEAVY", currentSpot, portfoliosPerFamily, universe, rnd));
        result.addAll(buildFamily("ATM_HEAVY", currentSpot, portfoliosPerFamily, universe, rnd));

        return result;
    }

    private static List<OptionTemplate> buildUniverse()
    {
        List<OptionTemplate> universe = new ArrayList<>();

        double[] maturities = {30.0, 45.0, 60.0, 90.0, 120.0, 180.0, 270.0, 365.0, 540.0, 730.0};
        double[] strikeMults = {0.85, 0.90, 0.95, 0.975, 1.00, 1.025, 1.05, 1.10, 1.15, 1.20};

        for (double maturity : maturities)
        {
            for (double k : strikeMults)
            {
                universe.add(new OptionTemplate(OptionType.CALL, k, maturity));
                universe.add(new OptionTemplate(OptionType.PUT, k, maturity));
            }
        }

        return universe;
    }

    private static List<NamedOptionPortfolio> buildFamily(String family, double currentSpot, int nPortfolios,
                                                          List<OptionTemplate> universe, Random rnd)
    {
        List<NamedOptionPortfolio> result = new ArrayList<>();

        for (int p = 0; p < nPortfolios; p++)
        {
            List<OptionTemplate> eligible = filterUniverse(universe, family);

            List<OptionTemplate> shuffled = new ArrayList<>(eligible);
            Collections.shuffle(shuffled, rnd);

            int nPositions = switch (family)
            {
                case "BALANCED" -> 8 + rnd.nextInt(5);      // 8..12
                case "SHORT_DATED" -> 6 + rnd.nextInt(4);   // 6..9
                case "LONG_DATED" -> 6 + rnd.nextInt(4);
                case "OTM_HEAVY" -> 8 + rnd.nextInt(5);
                case "ATM_HEAVY" -> 8 + rnd.nextInt(5);
                default -> 8;
            };

            nPositions = Math.min(nPositions, shuffled.size());

            List<PortfolioOption> positions = new ArrayList<>();

            for (int i = 0; i < nPositions; i++)
            {
                OptionTemplate t = shuffled.get(i);

                double quantityAbs = 2.0 + rnd.nextInt(12); // 2..13
                double sign = rnd.nextBoolean() ? 1.0 : -1.0;
                double quantity = sign * quantityAbs;

                String instrumentId = family + "_P" + p + "_POS" + i + "_"
                        + t.optionType + "_K" + t.strikeMultiplier + "_T" + (int) t.maturityDays;

                positions.add(new PortfolioOption(instrumentId, "SPX", "EURUSD", "SPX_IV",
                        t.optionType, quantity, currentSpot * t.strikeMultiplier, t.maturityDays, 0.0));
            }

            result.add(new NamedOptionPortfolio(family + "_" + String.format(Locale.US, "%03d", p + 1), family, new OptionPortfolio(positions)));
        }

        return result;
    }

    private static List<OptionTemplate> filterUniverse(List<OptionTemplate> universe, String family)
    {
        List<OptionTemplate> result = new ArrayList<>();

        for (OptionTemplate t : universe)
        {
            boolean keep = switch (family)
            {
                case "BALANCED" -> true;
                case "SHORT_DATED" -> t.maturityDays <= 120.0;
                case "LONG_DATED" -> t.maturityDays >= 365.0;
                case "OTM_HEAVY" ->
                        (t.optionType == OptionType.CALL && t.strikeMultiplier >= 1.05) ||
                                (t.optionType == OptionType.PUT && t.strikeMultiplier <= 0.95);
                case "ATM_HEAVY" -> t.strikeMultiplier >= 0.975 && t.strikeMultiplier <= 1.025;
                default -> true;
            };

            if (keep)
            {
                result.add(t);
            }
        }

        return result;
    }
}



class FrozenBumpValidationRow
{
    final String portfolioId;
    final String family;
    final int nPositions;
    final double deltaBumpRel;
    final double gammaBumpRel;
    final double pcBump;

    final double netMarketValue;
    final double grossMarketValue;

    final double var95DiffPctGross;
    final double absVar95DiffPctGross;
    final double var99DiffPctGross;
    final double absVar99DiffPctGross;

    final double fullVar95PctGross;
    final double fullVar99PctGross;

    final TaylorFullDiagnosticsSummary diag;

    final boolean pass95;
    final boolean pass99;
    final boolean passBoth;

    FrozenBumpValidationRow(String portfolioId,
                            String family,
                            int nPositions,
                            double deltaBumpRel,
                            double gammaBumpRel,
                            double pcBump,
                            TaylorFullDiagnosticsSummary diag,
                            PortfolioValueSummary valueSummary,
                            double targetAbsVar95Diff,
                            double targetAbsVar99Diff)
    {
        this.portfolioId = portfolioId;
        this.family = family;
        this.nPositions = nPositions;
        this.deltaBumpRel = deltaBumpRel;
        this.gammaBumpRel = gammaBumpRel;
        this.pcBump = pcBump;

        this.diag = diag;

        this.netMarketValue = valueSummary.netMarketValue;
        this.grossMarketValue = valueSummary.grossMarketValue;

        this.var95DiffPctGross =
                safePctOfGross(diag.var95Diff, grossMarketValue);

        this.absVar95DiffPctGross =
                safePctOfGross(Math.abs(diag.var95Diff), grossMarketValue);

        this.var99DiffPctGross =
                safePctOfGross(diag.var99Diff, grossMarketValue);

        this.absVar99DiffPctGross =
                safePctOfGross(Math.abs(diag.var99Diff), grossMarketValue);

        this.fullVar95PctGross =
                safePctOfGross(diag.fullVar95, grossMarketValue);

        this.fullVar99PctGross =
                safePctOfGross(diag.fullVar99, grossMarketValue);

        this.pass95 = Math.abs(diag.var95Diff) <= targetAbsVar95Diff;
        this.pass99 = Math.abs(diag.var99Diff) <= targetAbsVar99Diff;
        this.passBoth = this.pass95 && this.pass99;
    }

    private static double safePctOfGross(double value,
                                         double grossMarketValue)
    {
        if (Math.abs(grossMarketValue) < 1e-12)
        {
            return Double.NaN;
        }

        return 100.0 * value / grossMarketValue;
    }
}

class FrozenBumpValidationAggregateRow
{
    final String bucket;
    final int nPortfolios;

    final double avgAbsVar95Diff;
    final double medianAbsVar95Diff;
    final double p90AbsVar95Diff;
    final double maxAbsVar95Diff;

    final double avgAbsVar99Diff;
    final double medianAbsVar99Diff;
    final double p90AbsVar99Diff;
    final double maxAbsVar99Diff;

    final double avgRmse;
    final double avgMae;
    final double avgMaxAbs;

    final double passRate95;
    final double passRate99;
    final double passRateBoth;

    FrozenBumpValidationAggregateRow(String bucket,
                                     int nPortfolios,
                                     double avgAbsVar95Diff,
                                     double medianAbsVar95Diff,
                                     double p90AbsVar95Diff,
                                     double maxAbsVar95Diff,
                                     double avgAbsVar99Diff,
                                     double medianAbsVar99Diff,
                                     double p90AbsVar99Diff,
                                     double maxAbsVar99Diff,
                                     double avgRmse,
                                     double avgMae,
                                     double avgMaxAbs,
                                     double passRate95,
                                     double passRate99,
                                     double passRateBoth)
    {
        this.bucket = bucket;
        this.nPortfolios = nPortfolios;
        this.avgAbsVar95Diff = avgAbsVar95Diff;
        this.medianAbsVar95Diff = medianAbsVar95Diff;
        this.p90AbsVar95Diff = p90AbsVar95Diff;
        this.maxAbsVar95Diff = maxAbsVar95Diff;
        this.avgAbsVar99Diff = avgAbsVar99Diff;
        this.medianAbsVar99Diff = medianAbsVar99Diff;
        this.p90AbsVar99Diff = p90AbsVar99Diff;
        this.maxAbsVar99Diff = maxAbsVar99Diff;
        this.avgRmse = avgRmse;
        this.avgMae = avgMae;
        this.avgMaxAbs = avgMaxAbs;
        this.passRate95 = passRate95;
        this.passRate99 = passRate99;
        this.passRateBoth = passRateBoth;
    }
}


class NamedOptionPortfolio
{
    private final String portfolioId;
    private final String family;
    private final OptionPortfolio optionPortfolio;

    public NamedOptionPortfolio(String portfolioId, String family, OptionPortfolio optionPortfolio)
    {
        this.portfolioId = portfolioId;
        this.family = family;
        this.optionPortfolio = optionPortfolio;
    }

    public String getPortfolioId()
    {
        return portfolioId;
    }

    public String getFamily()
    {
        return family;
    }

    public OptionPortfolio getOptionPortfolio()
    {
        return optionPortfolio;
    }
}

class FrozenBumpValidationRunner
{
    public static void run(List<NamedOptionPortfolio> portfolios,
                           double deltaBumpRel,
                           double gammaBumpRel,
                           double pcBump,
                           double targetAbsVar95Diff,
                           double targetAbsVar99Diff,
                           double currentSpot,
                           double[] currentNodeVols,
                           VolSurfaceConfig volSurfaceConfig,
                           OisCurve baseCurve,
                           PcaResult volPca,
                           PipelineConfig config,
                           boolean includeRateFactors,
                           ScenarioMarketState baseState,
                           List<String> factorNames,
                           List<FhsScenario> scenarios,
                           Map<String, Double> currentSpotById,
                           Map<String, Double> currentFxById,
                           PcaModelRegistry pcaRegistry,
                           String detailCsv,
                           String aggregateCsv) throws IOException
    {
        List<FrozenBumpValidationRow> detailRows = new ArrayList<>();

        for (NamedOptionPortfolio named : portfolios)
        {

            OptionPortfolio optionPortfolio = named.getOptionPortfolio();

            Portfolio greekPortfolio = ConsistentGreekCalculator.buildPortfolio(
                    optionPortfolio,
                    currentSpot,
                    currentNodeVols,
                    volSurfaceConfig,
                    baseCurve,
                    volPca,
                    config,
                    includeRateFactors,
                    deltaBumpRel,
                    gammaBumpRel,
                    pcBump);


            double[] taylorPnL = PortfolioPnLCalculator.computePnLDistribution(
                    greekPortfolio,
                    factorNames,
                    scenarios,
                    currentSpotById,
                    currentFxById);

            double[] fullPnL = FullRepricingPnLCalculator.computePnLDistribution(
                    optionPortfolio,
                    baseState,
                    factorNames,
                    scenarios,
                    config,
                    pcaRegistry);

            TaylorFullDiagnosticsSummary diag = TaylorFullDiagnosticsSummary.compute(taylorPnL, fullPnL);

            PortfolioValueSummary valueSummary = computeOptionPortfolioBaseValue(optionPortfolio, baseState, config);

            detailRows.add(new FrozenBumpValidationRow(named.getPortfolioId(), named.getFamily(), optionPortfolio.size(),
                    deltaBumpRel, gammaBumpRel, pcBump, diag, valueSummary, targetAbsVar95Diff, targetAbsVar99Diff));
        }

        exportDetailCsv(detailCsv, detailRows);

        List<FrozenBumpValidationAggregateRow> aggregateRows = aggregate(detailRows, targetAbsVar95Diff, targetAbsVar99Diff);

        exportAggregateCsv(aggregateCsv, aggregateRows);

        FrozenBumpValidationAggregateRow overall = aggregateRows.stream()
                .filter(r -> r.bucket.equals("ALL"))
                .findFirst()
                .orElseThrow();

        System.out.println("=== Frozen-parameter final validation ===");
        System.out.println("deltaBumpRel                 = " + deltaBumpRel);
        System.out.println("gammaBumpRel                 = " + gammaBumpRel);
        System.out.println("pcBump                       = " + pcBump);
        System.out.println("nPortfolios                  = " + overall.nPortfolios);

        System.out.println("avg |VaR95 diff|             = " + overall.avgAbsVar95Diff);
        System.out.println("median |VaR95 diff|          = " + overall.medianAbsVar95Diff);
        System.out.println("p90 |VaR95 diff|             = " + overall.p90AbsVar95Diff);
        System.out.println("max |VaR95 diff|             = " + overall.maxAbsVar95Diff);

        System.out.println("avg |VaR99 diff|             = " + overall.avgAbsVar99Diff);
        System.out.println("median |VaR99 diff|          = " + overall.medianAbsVar99Diff);
        System.out.println("p90 |VaR99 diff|             = " + overall.p90AbsVar99Diff);
        System.out.println("max |VaR99 diff|             = " + overall.maxAbsVar99Diff);

        System.out.println("avg RMSE                     = " + overall.avgRmse);
        System.out.println("avg MAE                      = " + overall.avgMae);
        System.out.println("avg MaxAbs                   = " + overall.avgMaxAbs);

        System.out.println("pass rate |VaR95 diff| <= " + targetAbsVar95Diff + " = " + overall.passRate95);
        System.out.println("pass rate |VaR99 diff| <= " + targetAbsVar99Diff + " = " + overall.passRate99);
        System.out.println("pass rate BOTH               = " + overall.passRateBoth);

        System.out.println("=== By family ===");
        for (FrozenBumpValidationAggregateRow row : aggregateRows)
        {
            if (row.bucket.equals("ALL"))
            {
                continue;
            }

            System.out.println("----------------------------------------");
            System.out.println("family                       = " + row.bucket);
            System.out.println("nPortfolios                  = " + row.nPortfolios);
            System.out.println("avg |VaR95 diff|             = " + row.avgAbsVar95Diff);
            System.out.println("avg |VaR99 diff|             = " + row.avgAbsVar99Diff);
            System.out.println("p90 |VaR99 diff|             = " + row.p90AbsVar99Diff);
            System.out.println("max |VaR99 diff|             = " + row.maxAbsVar99Diff);
            System.out.println("avg RMSE                     = " + row.avgRmse);
            System.out.println("avg MAE                      = " + row.avgMae);
            System.out.println("pass rate BOTH               = " + row.passRateBoth);
        }

        System.out.println("Exported frozen validation detail CSV to: " + detailCsv);
        System.out.println("Exported frozen validation aggregate CSV to: " + aggregateCsv);
    }

    private static List<FrozenBumpValidationAggregateRow> aggregate(List<FrozenBumpValidationRow> rows,
                                                                    double targetAbsVar95Diff,
                                                                    double targetAbsVar99Diff)
    {
        Map<String, List<FrozenBumpValidationRow>> grouped = new LinkedHashMap<>();
        grouped.put("ALL", new ArrayList<>(rows));

        for (FrozenBumpValidationRow row : rows)
        {
            grouped.computeIfAbsent(row.family, k -> new ArrayList<>()).add(row);
        }

        List<FrozenBumpValidationAggregateRow> result = new ArrayList<>();

        for (Map.Entry<String, List<FrozenBumpValidationRow>> entry : grouped.entrySet())
        {
            result.add(aggregateBucket(entry.getKey(), entry.getValue(), targetAbsVar95Diff, targetAbsVar99Diff));
        }

        return result;
    }

    private static FrozenBumpValidationAggregateRow aggregateBucket(String bucket, List<FrozenBumpValidationRow> rows,
                                                                    double targetAbsVar95Diff, double targetAbsVar99Diff)
    {
        int n = rows.size();

        double[] abs95 = new double[n];
        double[] abs99 = new double[n];
        double[] rmses = new double[n];
        double[] maes = new double[n];
        double[] maxAbs = new double[n];

        int pass95 = 0;
        int pass99 = 0;
        int passBoth = 0;

        for (int i = 0; i < n; i++)
        {
            FrozenBumpValidationRow row = rows.get(i);

            abs95[i] = Math.abs(row.diag.var95Diff);
            abs99[i] = Math.abs(row.diag.var99Diff);
            rmses[i] = row.diag.rmse;
            maes[i] = row.diag.mae;
            maxAbs[i] = row.diag.maxAbs;

            if (abs95[i] <= targetAbsVar95Diff) pass95++;
            if (abs99[i] <= targetAbsVar99Diff) pass99++;
            if (row.passBoth) passBoth++;
        }

        Arrays.sort(abs95);
        Arrays.sort(abs99);

        return new FrozenBumpValidationAggregateRow(
                bucket,
                n,
                average(abs95),
                percentileFromSorted(abs95, 0.50),
                percentileFromSorted(abs95, 0.90),
                abs95[n - 1],
                average(abs99),
                percentileFromSorted(abs99, 0.50),
                percentileFromSorted(abs99, 0.90),
                abs99[n - 1],
                average(rmses),
                average(maes),
                average(maxAbs),
                ((double) pass95) / n,
                ((double) pass99) / n,
                ((double) passBoth) / n);
    }

    private static double average(double[] x)
    {
        double sum = 0.0;
        for (double v : x)
        {
            sum += v;
        }
        return sum / x.length;
    }

    private static double percentileFromSorted(double[] sorted, double p)
    {
        if (sorted.length == 0)
        {
            throw new IllegalArgumentException("Tom array.");
        }

        double idx = p * (sorted.length - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);

        if (lo == hi)
        {
            return sorted[lo];
        }

        double w = idx - lo;
        return sorted[lo] * (1.0 - w) + sorted[hi] * w;
    }

    private static void exportDetailCsv(String fileName, List<FrozenBumpValidationRow> rows) throws IOException
    {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName)))
        {
            pw.println("portfolioId,family,nPositions,deltaBumpRel,gammaBumpRel,pcBump," +
                    "taylorVaR95,fullVaR95,VaR95Diff,absVaR95Diff," +
                    "taylorVaR99,fullVaR99,VaR99Diff,absVaR99Diff," +
                    "RMSE,MAE,MaxAbs,pass95,pass99,passBoth");

            for (FrozenBumpValidationRow row : rows)
            {
                TaylorFullDiagnosticsSummary d = row.diag;

                pw.printf(Locale.US,
                        "%s,%s,%d,%.6f,%.6f,%.6f," +
                                "%.10f,%.10f,%.10f,%.10f," +
                                "%.10f,%.10f,%.10f,%.10f," +
                                "%.10f,%.10f,%.10f,%s,%s,%s%n",
                        row.portfolioId,
                        row.family,
                        row.nPositions,
                        row.deltaBumpRel,
                        row.gammaBumpRel,
                        row.pcBump,
                        d.taylorVar95,
                        d.fullVar95,
                        d.var95Diff,
                        Math.abs(d.var95Diff),
                        d.taylorVar99,
                        d.fullVar99,
                        d.var99Diff,
                        Math.abs(d.var99Diff),
                        d.rmse,
                        d.mae,
                        d.maxAbs,
                        Boolean.toString(row.pass95),
                        Boolean.toString(row.pass99),
                        Boolean.toString(row.passBoth)
                );
            }
        }
    }

    private static void exportAggregateCsv(String fileName,
                                           List<FrozenBumpValidationAggregateRow> rows) throws IOException
    {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName)))
        {
            pw.println("bucket,nPortfolios," +
                    "avgAbsVaR95Diff,medianAbsVaR95Diff,p90AbsVaR95Diff,maxAbsVaR95Diff," +
                    "avgAbsVaR99Diff,medianAbsVaR99Diff,p90AbsVaR99Diff,maxAbsVaR99Diff," +
                    "avgRMSE,avgMAE,avgMaxAbs,passRate95,passRate99,passRateBoth");

            for (FrozenBumpValidationAggregateRow row : rows)
            {
                pw.printf(Locale.US,
                        "%s,%d," +
                                "%.10f,%.10f,%.10f,%.10f," +
                                "%.10f,%.10f,%.10f,%.10f," +
                                "%.10f,%.10f,%.10f,%.10f,%.10f,%.10f%n",
                        row.bucket,
                        row.nPortfolios,
                        row.avgAbsVar95Diff,
                        row.medianAbsVar95Diff,
                        row.p90AbsVar95Diff,
                        row.maxAbsVar95Diff,
                        row.avgAbsVar99Diff,
                        row.medianAbsVar99Diff,
                        row.p90AbsVar99Diff,
                        row.maxAbsVar99Diff,
                        row.avgRmse,
                        row.avgMae,
                        row.avgMaxAbs,
                        row.passRate95,
                        row.passRate99,
                        row.passRateBoth);
            }
        }
    }

    private static PortfolioValueSummary computeOptionPortfolioBaseValue(
            OptionPortfolio optionPortfolio,
            ScenarioMarketState baseState,
            PipelineConfig config)
    {
        double net = 0.0;
        double gross = 0.0;

        OisCurve baseCurve = buildCurveFromStateForValue(baseState);

        for (PortfolioOption option : optionPortfolio.getPositions())
        {
            double baseSpot = baseState.getSpot(option.getSpotId());
            double[] baseNodeVols = baseState.getNodeVols(option.getVolSurfaceId());

            VolSurfaceConfig volSurfaceConfig =
                    findVolSurfaceConfigForValue(config, option.getVolSurfaceId());

            double price = OptionPricingService.price(option, baseSpot, baseNodeVols, volSurfaceConfig, baseCurve);

            double signedValue = option.getQuantity() * price;

            net += signedValue;
            gross += Math.abs(signedValue);
        }

        return new PortfolioValueSummary(net, gross);
    }

    private static OisCurve buildCurveFromStateForValue(ScenarioMarketState state)
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

    private static VolSurfaceConfig findVolSurfaceConfigForValue(PipelineConfig config,
                                                                 String volSurfaceId)
    {
        for (VolSurfaceConfig cfg : config.getVolSurfaces())
        {
            if (cfg.getVolSurfaceId().equals(volSurfaceId))
            {
                return cfg;
            }
        }

        throw new IllegalArgumentException(
                "Kunde inte hitta volSurfaceConfig för " + volSurfaceId
        );
    }

    private static double safePctOfGross(double value,
                                         double grossMarketValue)
    {
        if (Math.abs(grossMarketValue) < 1e-12)
        {
            return Double.NaN;
        }

        return 100.0 * value / grossMarketValue;
    }

}

class TaylorFullDiagnosticsSummary
{
    final double mae;
    final double rmse;
    final double maxAbs;
    final int worstIndex;
    final double worstSignedError;
    final double taylorVar95;
    final double fullVar95;
    final double taylorVar99;
    final double fullVar99;
    final double var95Diff;
    final double var99Diff;

    TaylorFullDiagnosticsSummary(double mae,
                                 double rmse,
                                 double maxAbs,
                                 int worstIndex,
                                 double worstSignedError,
                                 double taylorVar95,
                                 double fullVar95,
                                 double taylorVar99,
                                 double fullVar99)
    {
        this.mae = mae;
        this.rmse = rmse;
        this.maxAbs = maxAbs;
        this.worstIndex = worstIndex;
        this.worstSignedError = worstSignedError;
        this.taylorVar95 = taylorVar95;
        this.fullVar95 = fullVar95;
        this.taylorVar99 = taylorVar99;
        this.fullVar99 = fullVar99;
        this.var95Diff = taylorVar95 - fullVar95;
        this.var99Diff = taylorVar99 - fullVar99;
    }

    static TaylorFullDiagnosticsSummary compute(double[] taylorPnL, double[] fullPnL)
    {
        if (taylorPnL.length != fullPnL.length)
        {
            throw new IllegalArgumentException("Taylor och full repricing har olika längd.");
        }

        double mae = 0.0;
        double rmse = 0.0;
        double maxAbs = 0.0;
        int worstIndex = -1;
        double worstSignedError = 0.0;

        for (int i = 0; i < taylorPnL.length; i++)
        {
            double err = taylorPnL[i] - fullPnL[i];
            double absErr = Math.abs(err);

            mae += absErr;
            rmse += err * err;

            if (absErr > maxAbs)
            {
                maxAbs = absErr;
                worstIndex = i;
                worstSignedError = err;
            }
        }

        mae /= taylorPnL.length;
        rmse = Math.sqrt(rmse / taylorPnL.length);

        VarResult t95 = VarCalculator.computeHistoricalVar(taylorPnL, 0.95);
        VarResult t99 = VarCalculator.computeHistoricalVar(taylorPnL, 0.99);
        VarResult f95 = VarCalculator.computeHistoricalVar(fullPnL, 0.95);
        VarResult f99 = VarCalculator.computeHistoricalVar(fullPnL, 0.99);

        return new TaylorFullDiagnosticsSummary(mae, rmse, maxAbs, worstIndex, worstSignedError, t95.getVar(), f95.getVar(), t99.getVar(), f99.getVar());
    }
}

class PortfolioValueSummary
{
    final double netMarketValue;
    final double grossMarketValue;

    PortfolioValueSummary(double netMarketValue, double grossMarketValue)
    {
        this.netMarketValue = netMarketValue;
        this.grossMarketValue = grossMarketValue;
    }
}


