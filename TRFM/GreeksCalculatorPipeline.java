package TRFM;

import TRFM.AssetModels.*;

import java.util.*;

public class GreeksCalculatorPipeline {}

class ConsistentGreekCalculator
{
    private static final double DEFAULT_RATE_BUMP_PP = 0.0001;

    public static double computeDelta(PortfolioOption option, double spot, double[] currentNodeVols, VolSurfaceConfig volSurfaceConfig,
                                      OisCurve curve, double deltaBumpRel)
    {
        double h = deltaBumpRel * spot;
        double up = OptionPricingService.price(option, spot + h, currentNodeVols, volSurfaceConfig, curve);
        double down = OptionPricingService.price(option, spot - h, currentNodeVols, volSurfaceConfig, curve);
        return (up - down) / (2.0 * h);
    }

    public static double computeGamma(PortfolioOption option, double spot, double[] currentNodeVols, VolSurfaceConfig volSurfaceConfig,
                                      OisCurve curve, double gammaBumpRel)
    {
        double h = gammaBumpRel * spot;

        double pUp2 = OptionPricingService.price(option, spot + 2.0 * h, currentNodeVols, volSurfaceConfig, curve);
        double pUp1 = OptionPricingService.price(option, spot + h, currentNodeVols, volSurfaceConfig, curve);
        double pMid = OptionPricingService.price(option, spot, currentNodeVols, volSurfaceConfig, curve);
        double pDn1 = OptionPricingService.price(option, spot - h, currentNodeVols, volSurfaceConfig, curve);
        double pDn2 = OptionPricingService.price(option, spot - 2.0 * h, currentNodeVols, volSurfaceConfig, curve);

        return (-pUp2 + 16.0 * pUp1 - 30.0 * pMid + 16.0 * pDn1 - pDn2) / (12.0 * h * h);
    }

    public static Map<String, Double> computeBucketRho(PortfolioOption option,
                                                       double spot,
                                                       double[] currentNodeVols,
                                                       VolSurfaceConfig volSurfaceConfig,
                                                       OisCurve baseCurve,
                                                       PipelineConfig config)
    {
        Map<String, Double> result = new LinkedHashMap<>();

        for (String rateCol : config.getRateColumns())
        {
            String tenor = config.extractTenor(rateCol);
            OisCurve upCurve = baseCurve.bumped(tenor, DEFAULT_RATE_BUMP_PP);
            OisCurve downCurve = baseCurve.bumped(tenor, -DEFAULT_RATE_BUMP_PP);
            double up = OptionPricingService.price(option, spot, currentNodeVols, volSurfaceConfig, upCurve);
            double down = OptionPricingService.price(option, spot, currentNodeVols, volSurfaceConfig, downCurve);
            double rho = (up - down) / (2.0 * DEFAULT_RATE_BUMP_PP);
            result.put(config.getRateFactorId(rateCol), rho);
        }

        return result;
    }

    public static Map<String, Double> computeRatePcSensitivitiesFromBucketRho(Map<String, Double> bucketRho, PcaResult ratePca, PipelineConfig config)
    {
        Map<String, Double> result = new LinkedHashMap<>();

        double[][] loadings = ratePca.getLoadings();
        double[] stdDevs = ratePca.getStdDevs();
        String[] inputCols = ratePca.getInputColumns();

        int k = loadings[0].length;

        for (int pc = 0; pc < k; pc++)
        {
            double sens = 0.0;

            for (int j = 0; j < inputCols.length; j++)
            {
                String factorName = inputCols[j];
                double bucketSens = bucketRho.getOrDefault(factorName, 0.0);

                sens += bucketSens * stdDevs[j] * loadings[j][pc];
            }

            result.put(FactorIds.ratePc(config.getRateCurveId(), pc + 1), sens);
        }

        return result;
    }

    public static Map<String, Double> computeRatePcCrossSensitivitiesFromBucketCross(Map<String, Double> bucketCross, PcaResult ratePca, PipelineConfig config)
    {
        Map<String, Double> result = new LinkedHashMap<>();

        double[][] loadings = ratePca.getLoadings();
        double[] stdDevs = ratePca.getStdDevs();
        String[] inputCols = ratePca.getInputColumns();

        int k = loadings[0].length;

        for (int pc = 0; pc < k; pc++)
        {
            double sens = 0.0;

            for (int j = 0; j < inputCols.length; j++)
            {
                String factorName = inputCols[j];
                double bucketSens = bucketCross.getOrDefault(factorName, 0.0);

                sens += bucketSens * stdDevs[j] * loadings[j][pc];
            }

            result.put(FactorIds.ratePc(config.getRateCurveId(), pc + 1), sens);
        }

        return result;
    }


    public static Map<String, Double> computeSpotRateCrossSensitivities(PortfolioOption option,
                                                                        double spot,
                                                                        double[] currentNodeVols,
                                                                        VolSurfaceConfig volSurfaceConfig,
                                                                        OisCurve baseCurve,
                                                                        PipelineConfig config,
                                                                        double deltaBumpRel)
    {
        Map<String, Double> result = new LinkedHashMap<>();

        double hs = deltaBumpRel * spot;
        double hr = DEFAULT_RATE_BUMP_PP;

        for (String rateCol : config.getRateColumns())
        {
            String tenor = config.extractTenor(rateCol);

            OisCurve upCurve = baseCurve.bumped(tenor, hr);
            OisCurve downCurve = baseCurve.bumped(tenor, -hr);

            double pp = OptionPricingService.price(option, spot + hs, currentNodeVols, volSurfaceConfig, upCurve);
            double pm = OptionPricingService.price(option, spot + hs, currentNodeVols, volSurfaceConfig, downCurve);
            double mp = OptionPricingService.price(option, spot - hs, currentNodeVols, volSurfaceConfig, upCurve);
            double mm = OptionPricingService.price(option, spot - hs, currentNodeVols, volSurfaceConfig, downCurve);

            double cross = (pp - pm - mp + mm) / (4.0 * hs * hr);
            result.put(config.getRateFactorId(rateCol), cross);
        }

        return result;
    }

    public static Map<String, Double> computeVolPcSensitivities(PortfolioOption option, double spot, double[] currentNodeVols,
                                                                VolSurfaceConfig volSurfaceConfig, OisCurve curve, PcaResult pcaResult, double pcBump)
    {
        Map<String, Double> result = new LinkedHashMap<>();

        int k = pcaResult.getLoadings()[0].length;
        for (int pc = 0; pc < k; pc++)
        {
            double[] upNodeVols = bumpNodeVolsByPc(currentNodeVols, pcaResult, pc, pcBump);
            double[] downNodeVols = bumpNodeVolsByPc(currentNodeVols, pcaResult, pc, -pcBump);

            double up = OptionPricingService.price(option, spot, upNodeVols, volSurfaceConfig, curve);
            double down = OptionPricingService.price(option, spot, downNodeVols, volSurfaceConfig, curve);

            double sens = (up - down) / (2.0 * pcBump);

            result.put(FactorIds.volPc(option.getVolSurfaceId(), pc + 1), sens);
        }

        return result;
    }

    public static OptionGreekRow buildOptionGreekRow(PortfolioOption option,
                                                     double spot,
                                                     double[] currentNodeVols,
                                                     VolSurfaceConfig volSurfaceConfig,
                                                     OisCurve curve,
                                                     PcaResult pcaResult,
                                                     PipelineConfig config,
                                                     boolean includeRateFactors,
                                                     double deltaBumpRel,
                                                     double gammaBumpRel,
                                                     double pcBump)
    {
        double delta = computeDelta(option, spot, currentNodeVols, volSurfaceConfig, curve, deltaBumpRel);
        double gamma = computeGamma(option, spot, currentNodeVols, volSurfaceConfig, curve, gammaBumpRel);

        Map<String, Double> factorSensitivities = new LinkedHashMap<>();
        if (includeRateFactors)
        {
            factorSensitivities.putAll(computeBucketRho(option, spot, currentNodeVols, volSurfaceConfig, curve, config));
        }
        factorSensitivities.putAll(computeVolPcSensitivities(option, spot, currentNodeVols, volSurfaceConfig, curve, pcaResult, pcBump));

        Map<String, Double> factorSecondOrderSensitivities = new LinkedHashMap<>();
        factorSecondOrderSensitivities.putAll(computeVolPcSecondOrderSensitivities(option, spot, currentNodeVols, volSurfaceConfig, curve, pcaResult, pcBump));

        Map<String, Double> spotFactorCrossSensitivities = new LinkedHashMap<>();
        if (includeRateFactors)
        {
            spotFactorCrossSensitivities.putAll(computeSpotRateCrossSensitivities(option, spot, currentNodeVols, volSurfaceConfig, curve, config, deltaBumpRel));
        }
        spotFactorCrossSensitivities.putAll(computeSpotVolPcCrossSensitivities(option, spot, currentNodeVols, volSurfaceConfig, curve, pcaResult, deltaBumpRel, pcBump));

        double fxDelta = 0.0;

        return new OptionGreekRow(
                option.getInstrumentId(),
                option.getSpotId(),
                option.getFxId(),
                option.getQuantity(),
                delta,
                gamma,
                fxDelta,
                factorSensitivities,
                factorSecondOrderSensitivities,
                spotFactorCrossSensitivities);
    }


    public static OptionGreekRow buildOptionGreekRow(PortfolioOption option,
                                                     double spot,
                                                     double[] currentNodeVols,
                                                     VolSurfaceConfig volSurfaceConfig,
                                                     OisCurve curve,
                                                     PcaResult volPcaResult,
                                                     PcaResult ratePca,
                                                     PipelineConfig config,
                                                     boolean includeRateFactors,
                                                     boolean useRatePca,
                                                     double deltaBumpRel,
                                                     double gammaBumpRel,
                                                     double pcBump)
    {
        double delta = computeDelta(option, spot, currentNodeVols, volSurfaceConfig, curve, deltaBumpRel);
        double gamma = computeGamma(option, spot, currentNodeVols, volSurfaceConfig, curve, gammaBumpRel);

        Map<String, Double> factorSensitivities = new LinkedHashMap<>();
        Map<String, Double> factorSecondOrderSensitivities = new LinkedHashMap<>();
        Map<String, Double> spotFactorCrossSensitivities = new LinkedHashMap<>();

        if (includeRateFactors)
        {
            Map<String, Double> bucketRho = computeBucketRho(option, spot, currentNodeVols, volSurfaceConfig, curve, config);
            Map<String, Double> bucketCross = computeSpotRateCrossSensitivities(option, spot, currentNodeVols, volSurfaceConfig, curve, config, deltaBumpRel);

            if (useRatePca)
            {
                factorSensitivities.putAll(computeRatePcSensitivitiesFromBucketRho(bucketRho, ratePca, config));
                spotFactorCrossSensitivities.putAll(computeRatePcCrossSensitivitiesFromBucketCross(bucketCross, ratePca, config));
            }
            else
            {
                factorSensitivities.putAll(bucketRho);
                spotFactorCrossSensitivities.putAll(bucketCross);
            }
        }

        factorSensitivities.putAll(computeVolPcSensitivities(option, spot, currentNodeVols, volSurfaceConfig, curve, volPcaResult, pcBump));

        factorSecondOrderSensitivities.putAll(computeVolPcSecondOrderSensitivities(option, spot, currentNodeVols, volSurfaceConfig, curve, volPcaResult, pcBump));

        spotFactorCrossSensitivities.putAll(computeSpotVolPcCrossSensitivities(option, spot, currentNodeVols, volSurfaceConfig, curve, volPcaResult, deltaBumpRel, pcBump));

        double fxDelta = 0.0;

        return new OptionGreekRow(
                option.getInstrumentId(),
                option.getSpotId(),
                option.getFxId(),
                option.getQuantity(),
                delta,
                gamma,
                fxDelta,
                factorSensitivities,
                factorSecondOrderSensitivities,
                spotFactorCrossSensitivities);
    }

    public static Portfolio buildPortfolio(OptionPortfolio optionPortfolio, double spot, double[] currentNodeVols, VolSurfaceConfig volSurfaceConfig,
                                           OisCurve curve, PcaResult pcaResult, PipelineConfig config, boolean includeRateFactors, double deltaBumpRel,
                                           double gammaBumpRel, double pcBump)
    {
        List<OptionGreekRow> rows = new ArrayList<>();

        for (PortfolioOption option : optionPortfolio.getPositions())
        {
            rows.add(buildOptionGreekRow(
                    option,
                    spot,
                    currentNodeVols,
                    volSurfaceConfig,
                    curve,
                    pcaResult,
                    config,
                    includeRateFactors,
                    deltaBumpRel,
                    gammaBumpRel,
                    pcBump));
        }

        return new Portfolio(rows);
    }

    private static double[] bumpNodeVolsByPc(double[] currentNodeVols, PcaResult pcaResult, int pcIndexZeroBased, double pcBump)
    {
        double[][] loadings = pcaResult.getLoadings();
        double[] stdDevs = pcaResult.getStdDevs();

        if (currentNodeVols.length != loadings.length)
        {
            throw new IllegalArgumentException("Dimension mismatch mellan currentNodeVols och PCA-loadings.");
        }

        double[] bumped = new double[currentNodeVols.length];

        for (int j = 0; j < currentNodeVols.length; j++)
        {
            double dLogVol = pcBump * loadings[j][pcIndexZeroBased] * stdDevs[j];
            bumped[j] = currentNodeVols[j] * Math.exp(dLogVol);
        }

        return bumped;
    }

    public static Map<String, Double> computeVolPcSecondOrderSensitivities(PortfolioOption option,
                                                                           double spot,
                                                                           double[] currentNodeVols,
                                                                           VolSurfaceConfig volSurfaceConfig,
                                                                           OisCurve curve,
                                                                           PcaResult pcaResult,
                                                                           double pcBump)
    {
        Map<String, Double> result = new LinkedHashMap<>();

        int k = pcaResult.getLoadings()[0].length;
        double h = pcBump;

        for (int pc = 0; pc < k; pc++)
        {
            double[] upNodeVols = bumpNodeVolsByPc(currentNodeVols, pcaResult, pc, h);
            double[] downNodeVols = bumpNodeVolsByPc(currentNodeVols, pcaResult, pc, -h);

            double up = OptionPricingService.price(option, spot, upNodeVols, volSurfaceConfig, curve);
            double mid = OptionPricingService.price(option, spot, currentNodeVols, volSurfaceConfig, curve);
            double down = OptionPricingService.price(option, spot, downNodeVols, volSurfaceConfig, curve);

            double second = (up - 2.0 * mid + down) / (h * h);

            result.put(FactorIds.volPc(option.getVolSurfaceId(), pc + 1), second);
        }

        return result;
    }

    public static Map<String, Double> computeSpotVolPcCrossSensitivities(PortfolioOption option,
                                                                         double spot,
                                                                         double[] currentNodeVols,
                                                                         VolSurfaceConfig volSurfaceConfig,
                                                                         OisCurve curve,
                                                                         PcaResult pcaResult,
                                                                         double deltaBumpRel,
                                                                         double pcBump)
    {
        Map<String, Double> result = new LinkedHashMap<>();

        double hs = deltaBumpRel * spot;
        double hp = pcBump;

        int k = pcaResult.getLoadings()[0].length;

        for (int pc = 0; pc < k; pc++)
        {
            double[] pcUpNodeVols = bumpNodeVolsByPc(currentNodeVols, pcaResult, pc, hp);
            double[] pcDownNodeVols = bumpNodeVolsByPc(currentNodeVols, pcaResult, pc, -hp);

            double pp = OptionPricingService.price(option, spot + hs, pcUpNodeVols, volSurfaceConfig, curve);
            double pm = OptionPricingService.price(option, spot + hs, pcDownNodeVols, volSurfaceConfig, curve);
            double mp = OptionPricingService.price(option, spot - hs, pcUpNodeVols, volSurfaceConfig, curve);
            double mm = OptionPricingService.price(option, spot - hs, pcDownNodeVols, volSurfaceConfig, curve);

            double cross = (pp - pm - mp + mm) / (4.0 * hs * hp);
            result.put(FactorIds.volPc(option.getVolSurfaceId(), pc + 1), cross);
        }

        return result;
    }
}

class MixedGreekPortfolioBuilder
{
    public static Portfolio build(
            OptionPortfolio optionPortfolio,
            List<StockPosition> stocks,
            double spot,
            double[] currentNodeVols,
            VolSurfaceConfig volSurfaceConfig,
            OisCurve curve,
            PcaResult volPca,
            PipelineConfig config,
            boolean includeRateFactors,
            double deltaBumpRel,
            double gammaBumpRel,
            double pcBump)
    {
        return build(optionPortfolio, stocks, Collections.emptyList(), spot, currentNodeVols, volSurfaceConfig,
                curve, volPca, config, includeRateFactors, false, null, Collections.emptyMap(),
                deltaBumpRel, gammaBumpRel, pcBump, 0.0001, 0.20, true);
    }

    public static Portfolio build(
            OptionPortfolio optionPortfolio,
            List<StockPosition> stocks,
            List<BondPosition> bonds,
            double spot,
            double[] currentNodeVols,
            VolSurfaceConfig volSurfaceConfig,
            OisCurve curve,
            PcaResult volPca,
            PipelineConfig config,
            boolean includeRateFactors,
            boolean useRatePca,
            PcaResult ratePca,
            Map<String, Double> currentFxById,
            double deltaBumpRel,
            double gammaBumpRel,
            double pcBump,
            double bondRateBump,
            double bondRatePcBump,
            boolean includeBondConvexity)
    {
        List<OptionGreekRow> rows = new ArrayList<>();

        if (optionPortfolio != null)
        {
            for (PortfolioOption option : optionPortfolio.getPositions())
            {
                rows.add(ConsistentGreekCalculator.buildOptionGreekRow(
                        option,
                        spot,
                        currentNodeVols,
                        volSurfaceConfig,
                        curve,
                        volPca,
                        config,
                        includeRateFactors,
                        deltaBumpRel,
                        gammaBumpRel,
                        pcBump));
            }
        }

        if (stocks != null)
        {
            for (StockPosition stock : stocks)
            {
                rows.add(StockGreekBuilder.buildGreekRow(stock));
            }
        }

        if (bonds != null)
        {
            for (BondPosition bond : bonds)
            {
                double currentFx = resolveCurrentFxForInstrument(bond.getInstrumentId(), bond.getFxId(), currentFxById);
                rows.add(BondGreekBuilder.buildGreekRow(bond, curve, config, currentFx, includeRateFactors, useRatePca,
                        ratePca, bondRateBump, bondRatePcBump, includeBondConvexity));
            }
        }

        return new Portfolio(rows);
    }

    private static double resolveCurrentFxForInstrument(String instrumentId,
                                                        String fxId,
                                                        Map<String, Double> currentFxById)
    {
        if (FxIds.isNoFx(fxId))
        {
            return 1.0;
        }

        Double fx = currentFxById.get(fxId);

        if (fx == null)
        {
            throw new IllegalArgumentException("Saknar current FX för instrument " + instrumentId + ", fxId=" + fxId);
        }

        return fx;
    }

}


class StockGreekBuilder
{
    public static OptionGreekRow buildGreekRow(StockPosition stock)
    {
        Map<String, Double> factorSensitivities = new LinkedHashMap<>();
        Map<String, Double> factorSecondOrderSensitivities = new LinkedHashMap<>();
        Map<String, Double> spotFactorCrossSensitivities = new LinkedHashMap<>();

        return new OptionGreekRow(stock.getInstrumentId(), stock.getSpotId(), stock.getFxId(), stock.getQuantity(),
                1.0,   // delta
                0.0,   // gamma
                0.0,   // fxDelta
                factorSensitivities,
                factorSecondOrderSensitivities,
                spotFactorCrossSensitivities);
    }
}
