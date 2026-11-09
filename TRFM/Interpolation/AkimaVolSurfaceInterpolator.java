package TRFM.Interpolation;

import TRFM.VolSurfaceConfig;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.*;

public class AkimaVolSurfaceInterpolator
{
    private static final AkimaSplineInterpolator AKIMA = new AkimaSplineInterpolator();
    private static final LinearInterpolator LINEAR = new LinearInterpolator();

    public static double interpolateVol(double[] nodeVols,
                                        VolSurfaceConfig volSurfaceConfig,
                                        double moneynessPct,
                                        double maturityDays)
    {
        TRFM.Interpolation.AkimaVolSurfaceInterpolator.SurfaceGrid grid = buildSurfaceGrid(nodeVols, volSurfaceConfig);

        double tq = clamp(daysToMonths(maturityDays),
                grid.maturityMonths[0],
                grid.maturityMonths[grid.maturityMonths.length - 1]);

        double mq = clamp(moneynessPct,
                grid.moneynessPcts[0],
                grid.moneynessPcts[grid.moneynessPcts.length - 1]);

        // For each maturity slice: Akima spline in moneyness dimension
        double[] volsAcrossMaturity = new double[grid.maturityMonths.length];
        for (int i = 0; i < grid.maturityMonths.length; i++)
        {
            volsAcrossMaturity[i] = interpolate1D(grid.moneynessPcts, grid.volMatrix[i], mq);
        }

        // Akima spline in maturity dimension
        double vol = interpolate1D(grid.maturityMonths, volsAcrossMaturity, tq);

        return Math.max(vol, 1e-8);
    }

    private static double interpolate1D(double[] x, double[] y, double xq)
    {
        if (x.length != y.length)
            throw new IllegalArgumentException("x and y must have the same length.");
        if (x.length == 0)
            throw new IllegalArgumentException("Empty interpolation data.");
        if (x.length == 1)
            return y[0];

        double xqc = clamp(xq, x[0], x[x.length - 1]);

        // Use linear interpolation for only two points
        if (x.length == 2)
        {
            double w = (xqc - x[0]) / (x[1] - x[0]);
            return y[0] + w * (y[1] - y[0]);
        }

        // Try Akima spline
        try
        {
            PolynomialSplineFunction f = AKIMA.interpolate(x, y);
            return f.value(xqc);
        }
        catch (Exception e)
        {
            // Fallback to linear interpolation if Akima fails
            return linearInterpolate(x, y, xqc);
        }
    }

    private static double linearInterpolate(double[] x, double[] y, double xq)
    {
        int idx = Arrays.binarySearch(x, xq);
        if (idx >= 0) return y[idx];
        idx = -idx - 2;
        if (idx < 0) return y[0];
        if (idx >= x.length - 1) return y[y.length - 1];
        double w = (xq - x[idx]) / (x[idx+1] - x[idx]);
        return y[idx] + w * (y[idx+1] - y[idx]);
    }

    private static TRFM.Interpolation.AkimaVolSurfaceInterpolator.SurfaceGrid buildSurfaceGrid(double[] nodeVols,
                                                                                 VolSurfaceConfig volSurfaceConfig)
    {
        List<String> nodeCols = volSurfaceConfig.getNodeColumns();

        if (nodeVols.length != nodeCols.size())
            throw new IllegalArgumentException("nodeVols and volSurfaceConfig dimensions do not match.");

        NavigableSet<Double> maturitySet = new TreeSet<>();
        NavigableSet<Double> moneynessSet = new TreeSet<>();
        Map<String, Double> valueByKey = new LinkedHashMap<>();

        for (int i = 0; i < nodeCols.size(); i++)
        {
            TRFM.Interpolation.AkimaVolSurfaceInterpolator.NodePoint p = parseNode(nodeCols.get(i));
            maturitySet.add(p.maturityMonths);
            moneynessSet.add(p.moneynessPct);
            valueByKey.put(key(p.maturityMonths, p.moneynessPct), nodeVols[i]);
        }

        double[] maturities = maturitySet.stream().mapToDouble(Double::doubleValue).toArray();
        double[] moneyness = moneynessSet.stream().mapToDouble(Double::doubleValue).toArray();

        double[][] volMatrix = new double[maturities.length][moneyness.length];

        for (int i = 0; i < maturities.length; i++)
        {
            for (int j = 0; j < moneyness.length; j++)
            {
                String k = key(maturities[i], moneyness[j]);
                Double vol = valueByKey.get(k);
                if (vol == null)
                    throw new IllegalArgumentException("Incomplete volatility grid, missing node for " + k);
                volMatrix[i][j] = vol;
            }
        }

        return new TRFM.Interpolation.AkimaVolSurfaceInterpolator.SurfaceGrid(maturities, moneyness, volMatrix);
    }

    private static TRFM.Interpolation.AkimaVolSurfaceInterpolator.NodePoint parseNode(String nodeCol)
    {
        String[] parts = nodeCol.split("_");
        if (parts.length < 3)
            throw new IllegalArgumentException("Cannot parse volatility column: " + nodeCol);

        String tenorLabel = parts[1].toLowerCase(Locale.ROOT);
        String moneynessLabel = parts[2];

        double maturityMonths = tenorToMonths(tenorLabel);
        double moneynessPct = Double.parseDouble(moneynessLabel);

        return new TRFM.Interpolation.AkimaVolSurfaceInterpolator.NodePoint(maturityMonths, moneynessPct);
    }

    private static double tenorToMonths(String tenorLabel)
    {
        if (tenorLabel.endsWith("d"))
        {
            double days = Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
            return daysToMonths(days);
        }
        if (tenorLabel.endsWith("m"))
            return Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
        if (tenorLabel.endsWith("y"))
            return 12.0 * Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
        throw new IllegalArgumentException("Unknown tenor label: " + tenorLabel);
    }

    private static double daysToMonths(double days)
    {
        return days * 12.0 / 365.0;
    }

    private static double clamp(double x, double lo, double hi)
    {
        return Math.max(lo, Math.min(hi, x));
    }

    private static String key(double maturityMonths, double moneynessPct)
    {
        return String.format(Locale.US, "%.8f|%.8f", maturityMonths, moneynessPct);
    }

    private static class NodePoint
    {
        private final double maturityMonths;
        private final double moneynessPct;
        private NodePoint(double maturityMonths, double moneynessPct)
        {
            this.maturityMonths = maturityMonths;
            this.moneynessPct = moneynessPct;
        }
    }

    private static class SurfaceGrid
    {
        private final double[] maturityMonths;
        private final double[] moneynessPcts;
        private final double[][] volMatrix;
        private SurfaceGrid(double[] maturityMonths, double[] moneynessPcts, double[][] volMatrix)
        {
            this.maturityMonths = maturityMonths;
            this.moneynessPcts = moneynessPcts;
            this.volMatrix = volMatrix;
        }
    }
}
