package TRFM;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** ---------------- STEP 5: FINAL RISK FACTOR MATRIX ---------------- */
// own class now

public class RiskFactorMatrixBuilder
{
    public static RiskFactorMatrix build(List<RawFactorRow> rawFactorRows, PipelineConfig config, PcaModelRegistry pcaRegistry, boolean includeRateFactors)
    {
        int t = rawFactorRows.size();

        List<String> columnNames = new ArrayList<>();

        for (SpotRiskConfig spotCfg : config.getSpots())
        {
            columnNames.add(FactorIds.spot(spotCfg.getSpotId()));
        }

        for (FxRiskConfig fxCfg : config.getFxs())
        {
            columnNames.add(FactorIds.fx(fxCfg.getFxId()));
        }

        if (includeRateFactors)
        {
            for (RateCurveConfig rateCurveCfg : config.getRateCurves())
            {
                for (String rateCol : rateCurveCfg.getRateColumns())
                {
                    columnNames.add(rateCurveCfg.getFactorId(rateCol));
                }
            }
        }

        for (VolSurfaceConfig volSurfaceCfg : config.getVolSurfaces())
        {
            PcaResult pca = pcaRegistry.get(volSurfaceCfg.getVolSurfaceId());
            int k = pca.getScores()[0].length;
            for (int i = 0; i < k; i++)
            {
                columnNames.add(FactorIds.volPc(volSurfaceCfg.getVolSurfaceId(), i + 1));
            }
        }

        double[][] data = new double[t][columnNames.size()];
        List<LocalDate> dates = new ArrayList<>();

        for (int i = 0; i < t; i++)
        {
            RawFactorRow row = rawFactorRows.get(i);
            dates.add(row.getDate());

            int c = 0;

            for (SpotRiskConfig spotCfg : config.getSpots())
            {
                data[i][c++] = row.get(FactorIds.spot(spotCfg.getSpotId()));
            }

            for (FxRiskConfig fxCfg : config.getFxs())
            {
                data[i][c++] = row.get(FactorIds.fx(fxCfg.getFxId()));
            }

            if (includeRateFactors)
            {
                for (RateCurveConfig rateCurveCfg : config.getRateCurves())
                {
                    for (String rateCol : rateCurveCfg.getRateColumns())
                    {
                        data[i][c++] = row.get(rateCurveCfg.getFactorId(rateCol));
                    }
                }
            }

            for (VolSurfaceConfig volSurfaceCfg : config.getVolSurfaces())
            {
                PcaResult pca = pcaRegistry.get(volSurfaceCfg.getVolSurfaceId());
                int k = pca.getScores()[0].length;
                for (int pc = 0; pc < k; pc++)
                {
                    data[i][c++] = pca.getScores()[i][pc];
                }
            }
        }

        return new RiskFactorMatrix(dates, columnNames, data);
    }


    public static RiskFactorMatrix build(List<RawFactorRow> rawFactorRows, PipelineConfig config, PcaModelRegistry pcaRegistry,
                                         boolean includeRateFactors, boolean useRatePca, PcaResult ratePca)
    {
        int t = rawFactorRows.size();
        List<String> columnNames = new ArrayList<>();

        for (SpotRiskConfig spotCfg : config.getSpots())
        {
            columnNames.add(FactorIds.spot(spotCfg.getSpotId()));
        }

        for (FxRiskConfig fxCfg : config.getFxs())
        {
            columnNames.add(FactorIds.fx(fxCfg.getFxId()));
        }

        if (includeRateFactors)
        {
            if (useRatePca)
            {
                int k = ratePca.getScores()[0].length;
                for (int pc = 0; pc < k; pc++)
                {
                    columnNames.add(FactorIds.ratePc(config.getRateCurveId(), pc + 1));
                }
            }
            else
            {
                for (RateCurveConfig rateCurveCfg : config.getRateCurves())
                {
                    for (String rateCol : rateCurveCfg.getRateColumns())
                    {
                        columnNames.add(rateCurveCfg.getFactorId(rateCol));
                    }
                }
            }
        }

        for (VolSurfaceConfig volSurfaceCfg : config.getVolSurfaces())
        {
            PcaResult pca = pcaRegistry.get(volSurfaceCfg.getVolSurfaceId());
            int k = pca.getScores()[0].length;
            for (int i = 0; i < k; i++)
            {
                columnNames.add(FactorIds.volPc(volSurfaceCfg.getVolSurfaceId(), i + 1));
            }
        }

        double[][] data = new double[t][columnNames.size()];
        List<LocalDate> dates = new ArrayList<>();

        for (int i = 0; i < t; i++)
        {
            RawFactorRow row = rawFactorRows.get(i);
            dates.add(row.getDate());

            int c = 0;

            for (SpotRiskConfig spotCfg : config.getSpots())
            {
                data[i][c++] = row.get(FactorIds.spot(spotCfg.getSpotId()));
            }

            for (FxRiskConfig fxCfg : config.getFxs())
            {
                data[i][c++] = row.get(FactorIds.fx(fxCfg.getFxId()));
            }

            if (includeRateFactors)
            {
                if (useRatePca)
                {
                    int k = ratePca.getScores()[0].length;
                    for (int pc = 0; pc < k; pc++)
                    {
                        data[i][c++] = ratePca.getScores()[i][pc];
                    }
                }
                else
                {
                    for (RateCurveConfig rateCurveCfg : config.getRateCurves())
                    {
                        for (String rateCol : rateCurveCfg.getRateColumns())
                        {
                            data[i][c++] = row.get(rateCurveCfg.getFactorId(rateCol));
                        }
                    }
                }
            }

            for (VolSurfaceConfig volSurfaceCfg : config.getVolSurfaces())
            {
                PcaResult pca = pcaRegistry.get(volSurfaceCfg.getVolSurfaceId());
                int k = pca.getScores()[0].length;
                for (int pc = 0; pc < k; pc++)
                {
                    data[i][c++] = pca.getScores()[i][pc];
                }
            }
        }

        return new RiskFactorMatrix(dates, columnNames, data);
    }

}