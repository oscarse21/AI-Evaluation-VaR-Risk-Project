package TRFM.Interpolation;

import TRFM.VolSurfaceConfig;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.*;

public class CubicSplineVolSurfaceInterpolator
{
    private static final SplineInterpolator SPLINE = new SplineInterpolator();
    private static final LinearInterpolator LINEAR = new LinearInterpolator();

    public static double interpolateVol(double[] nodeVols,
                                        VolSurfaceConfig volSurfaceConfig,
                                        double moneynessPct,
                                        double maturityDays)
    {
        TRFM.Interpolation.CubicSplineVolSurfaceInterpolator.SurfaceGrid grid = buildSurfaceGrid(nodeVols, volSurfaceConfig);

        double tq = clamp(daysToMonths(maturityDays),
                grid.maturityMonths[0],
                grid.maturityMonths[grid.maturityMonths.length - 1]);

        double mq = clamp(moneynessPct,
                grid.moneynessPcts[0],
                grid.moneynessPcts[grid.moneynessPcts.length - 1]);

        // För varje maturity-slice: spline i moneyness-led
        double[] volsAcrossMaturity = new double[grid.maturityMonths.length];
        for (int i = 0; i < grid.maturityMonths.length; i++)
        {
            volsAcrossMaturity[i] = interpolate1D(grid.moneynessPcts, grid.volMatrix[i], mq);
        }

        // Spline i maturity-led
        double vol = interpolate1D(grid.maturityMonths, volsAcrossMaturity, tq);

        return Math.max(vol, 1e-8);
    }

    private static double interpolate1D(double[] x, double[] y, double xq)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("x och y måste ha samma längd.");
        }
        if (x.length == 0)
        {
            throw new IllegalArgumentException("Tom interpolation.");
        }
        if (x.length == 1)
        {
            return y[0];
        }

        double xqc = clamp(xq, x[0], x[x.length - 1]);

        if (x.length == 2)
        {
            double w = (xqc - x[0]) / (x[1] - x[0]);
            return y[0] + w * (y[1] - y[0]);
        }

        PolynomialSplineFunction f = SPLINE.interpolate(x, y);
        return f.value(xqc);
    }

    private static TRFM.Interpolation.CubicSplineVolSurfaceInterpolator.SurfaceGrid buildSurfaceGrid(double[] nodeVols,
                                                                                       VolSurfaceConfig volSurfaceConfig)
    {
        List<String> nodeCols = volSurfaceConfig.getNodeColumns();

        if (nodeVols.length != nodeCols.size())
        {
            throw new IllegalArgumentException("nodeVols och volSurfaceConfig har olika dimension.");
        }

        NavigableSet<Double> maturitySet = new TreeSet<>();
        NavigableSet<Double> moneynessSet = new TreeSet<>();
        Map<String, Double> valueByKey = new LinkedHashMap<>();

        for (int i = 0; i < nodeCols.size(); i++)
        {
            TRFM.Interpolation.CubicSplineVolSurfaceInterpolator.NodePoint p = parseNode(nodeCols.get(i));
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
                {
                    throw new IllegalArgumentException("Ofullständigt volgrid, saknar nod för " + k);
                }
                volMatrix[i][j] = vol;
            }
        }

        return new TRFM.Interpolation.CubicSplineVolSurfaceInterpolator.SurfaceGrid(maturities, moneyness, volMatrix);
    }

    private static TRFM.Interpolation.CubicSplineVolSurfaceInterpolator.NodePoint parseNode(String nodeCol)
    {
        String[] parts = nodeCol.split("_");
        if (parts.length < 3)
        {
            throw new IllegalArgumentException("Kan inte parsa volkolumn: " + nodeCol);
        }

        String tenorLabel = parts[1].toLowerCase(Locale.ROOT);
        String moneynessLabel = parts[2];

        double maturityMonths = tenorToMonths(tenorLabel);
        double moneynessPct = Double.parseDouble(moneynessLabel);

        return new TRFM.Interpolation.CubicSplineVolSurfaceInterpolator.NodePoint(maturityMonths, moneynessPct);
    }

    private static double tenorToMonths(String tenorLabel)
    {
        if (tenorLabel.endsWith("d"))
        {
            double days = Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
            return daysToMonths(days);
        }
        if (tenorLabel.endsWith("m"))
        {
            return Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
        }
        if (tenorLabel.endsWith("y"))
        {
            return 12.0 * Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
        }

        throw new IllegalArgumentException("Okänd tenor-label: " + tenorLabel);
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
