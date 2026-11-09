package TRFM.Interpolation;

import TRFM.VolSurfaceConfig;

import java.util.List;
import java.util.Map;

public class VolSurfaceInterpolator
{
    public static double interpolateVol(double[] currentNodeVols, VolSurfaceConfig volSurfaceConfig, double moneynessPct, double maturityDays)
    {
        Map<String, Double> weights = BilinearNodeVegaInterpolator.buildWeightMap(moneynessPct, maturityDays, volSurfaceConfig);

        double vol = 0.0;
        List<String> nodeCols = volSurfaceConfig.getNodeColumns();

        for (int i = 0; i < nodeCols.size(); i++)
        {
            String col = nodeCols.get(i);
            double w = weights.getOrDefault(col, 0.0);
            vol += w * currentNodeVols[i];
        }

        return vol;
    }
}
