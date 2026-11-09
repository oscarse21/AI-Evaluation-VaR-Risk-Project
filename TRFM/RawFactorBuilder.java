package TRFM;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RawFactorBuilder
{
    public static List<RawFactorRow> build(List<MarketObservation> raw, PipelineConfig config, boolean includeRateFactors)
    {
        if (raw.size() < 2)
        {
            throw new IllegalArgumentException("Behöver minst två observationer.");
        }

        List<RawFactorRow> rows = new ArrayList<>();

        for (int t = 1; t < raw.size(); t++)
        {
            MarketObservation prev = raw.get(t - 1);
            MarketObservation curr = raw.get(t);

            Map<String, Double> factors = new LinkedHashMap<>();


            for (SpotRiskConfig spotCfg : config.getSpots())
            {
                double spotRet = Math.log(curr.get(spotCfg.getColumnName()) / prev.get(spotCfg.getColumnName()));
                factors.put(FactorIds.spot(spotCfg.getSpotId()), spotRet);
            }

            for (FxRiskConfig fxCfg : config.getFxs())
            {
                double fxRet = Math.log(curr.get(fxCfg.getColumnName()) / prev.get(fxCfg.getColumnName()));
                factors.put(FactorIds.fx(fxCfg.getFxId()), fxRet);
            }

            if (includeRateFactors)
            {
                for (RateCurveConfig rateCurveCfg : config.getRateCurves())
                {
                    for (String rateCol : rateCurveCfg.getRateColumns())
                    {
                        double dr = curr.get(rateCol) - prev.get(rateCol);
                        factors.put(rateCurveCfg.getFactorId(rateCol), dr);
                    }
                }
            }

            for (VolSurfaceConfig volSurfaceCfg : config.getVolSurfaces())
            {
                for (String volCol : volSurfaceCfg.getNodeColumns())
                {
                    double dvol = Math.log(curr.get(volCol) / prev.get(volCol));
                    factors.put(FactorIds.volNodeLogChange(volSurfaceCfg.getVolSurfaceId(), volCol), dvol);
                }
            }

            rows.add(new RawFactorRow(curr.getDate(), factors));
        }

        return rows;
    }

    public static List<RawFactorRow> build(List<MarketObservation> raw, PipelineConfig config)
    {
        return build(raw, config, true);
    }
}