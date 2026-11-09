package TRFM.VolatilityModels;

import TRFM.*;
import TRFM.VolatilityEstimators.Garch11_Estimator;
import TRFM.VolatilityEstimators.GjrGarch11_Estimator;
import TRFM.VolatilityModels.VolatilityModel;
import TRFM.VolatilityResults.GjrGarchResult;
import TRFM.VolatilityResults.VolatilityResult;

public class GjrGarchVolatilityModel implements VolatilityModel
{
    private static final double MIN_VAR = 1e-8;

    private final GarchDistribution distribution;

    public GjrGarchVolatilityModel(GarchDistribution distribution)
    {
        this.distribution = distribution;
    }

    @Override
    public VolatilityResult estimate(RiskFactorMatrix matrix)
    {
        double[][] returns = matrix.getData();
        int T = returns.length;
        int d = returns[0].length;

        double[][] sigma = new double[T][d];

        // För vanliga GARCH-faktorer sätts gamma = 0.
        double[][] paramsByFactor = new double[d][5];
        double[] unconditionalVars = new double[d];

        for (int j = 0; j < d; j++)
        {
            double[] series = new double[T];

            for (int i = 0; i < T; i++)
            {
                series[i] = returns[i][j];
            }

            double omega;
            double alpha;
            double gamma;
            double beta;
            double nu;

            if (j == 0)
            {
                // SPX spot: GJR-GARCH Student-t / Normal
                GjrGarch11_Estimator estimator = new GjrGarch11_Estimator(series, distribution);

                double[] p = estimator.estimate();

                omega = p[0];
                alpha = p[1];
                gamma = p[2];
                beta  = p[3];
                nu    = p.length > 4 ? p[4] : Double.NaN;
            }
            else
            {
                // Alla andra faktorer: vanlig GARCH Student-t / Normal
                Garch11_Estimator estimator = new Garch11_Estimator(series, distribution);

                double[] p = estimator.estimate();

                omega = p[0];
                alpha = p[1];
                gamma = 0.0;
                beta  = p[2];
                nu    = p.length > 3 ? p[3] : Double.NaN;
            }

            paramsByFactor[j][0] = omega;
            paramsByFactor[j][1] = alpha;
            paramsByFactor[j][2] = gamma;
            paramsByFactor[j][3] = beta;
            paramsByFactor[j][4] = nu;

            double persistence = alpha + beta + 0.5 * gamma;
            double unconditionalVar = omega / Math.max(1.0 - persistence, 1e-8);

            if (!isFinitePositive(unconditionalVar))
            {
                unconditionalVar = sampleVariance(series);
            }

            unconditionalVars[j] = unconditionalVar;
            double prevVar = unconditionalVar;

            for (int i = 0; i < T; i++)
            {
                double newVar;

                if (i == 0)
                {
                    newVar = unconditionalVar;
                }
                else
                {
                    double prevReturn = series[i - 1];
                    double indicator = prevReturn < 0.0 ? 1.0 : 0.0;

                    newVar = omega
                            + alpha * prevReturn * prevReturn
                            + gamma * indicator * prevReturn * prevReturn
                            + beta * prevVar;
                }

                if (!isFinitePositive(newVar))
                {
                    newVar = MIN_VAR;
                }

                sigma[i][j] = Math.sqrt(newVar);
                prevVar = newVar;
            }
        }

        return new GjrGarchResult(sigma, paramsByFactor, unconditionalVars);
    }

    @Override
    public double[] forecastNextSigma(RiskFactorMatrix matrix, VolatilityResult result)
    {
        if (!(result instanceof GjrGarchResult))
        {
            throw new IllegalArgumentException("Result must be a GjrGarchResult");
        }

        GjrGarchResult gjr = (GjrGarchResult) result;

        double[][] returns = matrix.getData();
        int T = returns.length;
        int d = returns[0].length;

        double[] nextSigma = new double[d];

        for (int j = 0; j < d; j++)
        {
            double omega = gjr.getGjrParams()[j][0];
            double alpha = gjr.getGjrParams()[j][1];
            double gamma = gjr.getGjrParams()[j][2];
            double beta  = gjr.getGjrParams()[j][3];

            double lastReturn = returns[T - 1][j];
            double lastVar = gjr.getLastSigma()[j] * gjr.getLastSigma()[j];
            double indicator = lastReturn < 0.0 ? 1.0 : 0.0;

            double newVar = omega + alpha * lastReturn * lastReturn + gamma * indicator * lastReturn * lastReturn + beta * lastVar;

            if (!isFinitePositive(newVar))
            {
                newVar = MIN_VAR;
            }

            nextSigma[j] = Math.sqrt(newVar);
        }

        return nextSigma;
    }

    private static double sampleVariance(double[] x)
    {
        double mean = 0.0;

        for (double v : x)
        {
            mean += v;
        }

        mean /= x.length;

        double var = 0.0;

        for (double v : x)
        {
            double diff = v - mean;
            var += diff * diff;
        }

        var /= Math.max(x.length - 1, 1);

        return Math.max(var, MIN_VAR);
    }

    private static boolean isFinitePositive(double x)
    {
        return x > MIN_VAR && !Double.isNaN(x) && !Double.isInfinite(x);
    }

    public GjrGarchResult estimateWithFixedParameters(
            RiskFactorMatrix matrix,
            double[][] fixedParams,
            double[] fixedUnconditionalVars)
    {
        double[][] returns = matrix.getData();

        int T = returns.length;
        int d = returns[0].length;

        if (fixedParams.length != d || fixedUnconditionalVars.length != d)
        {
            throw new IllegalArgumentException("Cached GJR parameters do not match risk matrix dimension.");
        }

        double[][] sigma = new double[T][d];

        for (int j = 0; j < d; j++)
        {
            double omega = fixedParams[j][0];
            double alpha = fixedParams[j][1];
            double gamma = fixedParams[j][2];
            double beta  = fixedParams[j][3];

            double prevVar = fixedUnconditionalVars[j];

            for (int i = 0; i < T; i++)
            {
                double newVar;

                if (i == 0)
                {
                    newVar = fixedUnconditionalVars[j];
                }
                else
                {
                    double prevReturn = returns[i - 1][j];
                    double indicator = prevReturn < 0.0 ? 1.0 : 0.0;

                    newVar = omega
                            + (alpha + gamma * indicator) * prevReturn * prevReturn
                            + beta * prevVar;
                }

                if (!isFinitePositive(newVar))
                {
                    newVar = MIN_VAR;
                }

                sigma[i][j] = Math.sqrt(newVar);
                prevVar = newVar;
            }
        }

        return new GjrGarchResult(
                sigma,
                deepCopy(fixedParams),
                fixedUnconditionalVars.clone());
    }

    private static double[][] deepCopy(double[][] x)
    {
        double[][] copy = new double[x.length][];

        for (int i = 0; i < x.length; i++)
        {
            copy[i] = x[i].clone();
        }

        return copy;
    }
}