package TRFM.Calibration;

import TRFM.*;
import TRFM.VolatilityModels.VolatilityModel;
import TRFM.VolatilityResults.VolatilityResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CalibrationEngine
{
    public CalibrationResult calibrate(
            List<MarketObservation> marketData,
            PipelineConfig config,
            boolean includeRateFactors,
            boolean useRatePca,
            int nRatePcaComponents,
            VolatilityModel volatilityModel)
    {
        // 1. Raw risk-factor changes

        List<RawFactorRow> rawFactorRows = RawFactorBuilder.build(marketData, config, includeRateFactors);

        // 2. Rate PCA

        List<String> rateFactorCols =
                config.getRateColumns()
                        .stream()
                        .map(config::getRateFactorId)
                        .collect(Collectors.toList());

        PcaResult ratePca =
                PcaUtils.fitAndTransformOnFactorColumns(
                        rawFactorRows,
                        rateFactorCols,
                        nRatePcaComponents,
                        true);

        // 3. Volatility-surface PCA

        PcaModelRegistry pcaRegistry = buildPcaRegistry(rawFactorRows, config);

        PcaResult volPca = null;
        double[] currentNodeVols = new double[0];

        if (config.hasVolSurfaces())
        {
            volPca = pcaRegistry.get(config.getVolSurfaceId());

            currentNodeVols = extractCurrentNodeVols(marketData, config.getPrimaryVolSurface());
        }

        // 4. Final RiskFactorMatrix

        RiskFactorMatrix riskMatrix = RiskFactorMatrixBuilder.build(rawFactorRows, config, pcaRegistry, includeRateFactors,
                        useRatePca, ratePca);

        // 5. Volatility calibration + forecast + residuals

        VolatilityResult volResult;
        double[] nextSigma;
        ResidualMatrix residuals;

        volResult = volatilityModel.estimate(riskMatrix);

        nextSigma = volatilityModel.forecastNextSigma(riskMatrix, volResult);

        residuals = ResidualBuilder.build(riskMatrix, volResult);

        // 6. Current market

        Map<String, Double> currentSpotById = extractCurrentSpots(marketData, config);
        Map<String, Double> currentFxById = extractCurrentFxs(marketData, config);
        Map<String, Double> currentRates = extractCurrentRates(marketData, config);
        OisCurve baseCurve = buildCurveFromRates(currentRates);

        // 8. Return calibration

        return new CalibrationResult(
                riskMatrix,
                residuals,
                nextSigma,
                pcaRegistry,
                volPca,
                currentSpotById,
                currentFxById,
                currentNodeVols,
                baseCurve,
                ratePca);
    }

    // HELP METHODS FROM HERE DOWN

    private static PcaModelRegistry buildPcaRegistry(
            List<RawFactorRow> rawFactorRows,
            PipelineConfig config)
    {
        PcaModelRegistry registry = new PcaModelRegistry();

        for (VolSurfaceConfig volSurfaceCfg : config.getVolSurfaces())
        {
            PcaResult pca = PcaUtils.fitAndTransform(rawFactorRows, volSurfaceCfg, true);

            registry.put(volSurfaceCfg.getVolSurfaceId(), pca);
        }

        return registry;
    }

    private static double[] extractCurrentNodeVols(List<MarketObservation> raw, VolSurfaceConfig volSurfaceConfig)
    {
        MarketObservation last = raw.get(raw.size() - 1);
        List<String> volCols = volSurfaceConfig.getNodeColumns();

        double[] vols = new double[volCols.size()];
        for (int j = 0; j < volCols.size(); j++)
        {
            vols[j] = last.get(volCols.get(j));
        }

        return vols;
    }

    private static Map<String, Double> extractCurrentSpots(List<MarketObservation> raw, PipelineConfig config)
    {
        MarketObservation last = raw.get(raw.size() - 1);
        Map<String, Double> result = new LinkedHashMap<>();

        for (SpotRiskConfig spotCfg : config.getSpots())
        {
            result.put(spotCfg.getSpotId(), last.get(spotCfg.getColumnName()));
        }

        return result;
    }

    private static Map<String, Double> extractCurrentFxs(List<MarketObservation> raw, PipelineConfig config)
    {
        MarketObservation last = raw.get(raw.size() - 1);
        Map<String, Double> result = new LinkedHashMap<>();

        for (FxRiskConfig fxCfg : config.getFxs())
        {
            result.put(fxCfg.getFxId(), last.get(fxCfg.getColumnName()));
        }

        return result;
    }

    private static Map<String, Double> extractCurrentRates(List<MarketObservation> raw, PipelineConfig config)
    {
        MarketObservation last = raw.get(raw.size() - 1);
        Map<String, Double> result = new LinkedHashMap<>();

        for (String rateCol : config.getRateColumns())
        {
            result.put(rateCol, last.get(rateCol));
        }

        return result;
    }

    private static OisCurve buildCurveFromRates(Map<String, Double> rates)
    {
        return new OisCurve(
                rates.get("rate_1m"),
                rates.get("rate_3m"),
                rates.get("rate_6m"),
                rates.get("rate_1y"),
                rates.get("rate_2y"),
                rates.get("rate_3y"),
                rates.get("rate_4y"),
                rates.get("rate_5y"));
    }
}