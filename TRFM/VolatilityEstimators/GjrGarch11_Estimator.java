package TRFM.VolatilityEstimators;

import TRFM.GarchDistribution;

import TRFM.Optimize.Optimizer_NelderMead;
import org.apache.commons.math3.special.Gamma;

import java.util.function.Function;

public class GjrGarch11_Estimator
{

    private final double[] returns;
    private final Optimizer_NelderMead optimizer;
    private final GarchDistribution distribution;

    public GjrGarch11_Estimator(double[] returns, GarchDistribution distribution)
    {
        this.returns = returns;
        this.optimizer = new Optimizer_NelderMead();
        this.distribution = distribution;
    }

    public double[] estimate()
    {

        double[][] simplex;
        Function<double[], Double> objective;

        if (distribution == GarchDistribution.STUDENT_T) // STUDENT-T DIST
        {
            // Parameters: [omega, alpha, gamma, beta, nu]
            // rows = nParams + 1 = 6
            simplex = new double[][]{
                    {0.1, 0.05, 0.05, 0.90, 10.0},
                    {0.12, 0.05, 0.05, 0.90, 10.0},
                    {0.1, 0.07, 0.05, 0.90, 10.0},
                    {0.1, 0.05, 0.07, 0.90, 10.0},
                    {0.1, 0.05, 0.05, 0.85, 10.0},
                    {0.1, 0.05, 0.05, 0.90, 12.0}};

            objective = this::negativeLogLikelihood_S;
        }
        else // NORMAL DIST
        {
            // Parameters: [omega, alpha, gamma, beta]
            // rows = nParams + 1 = 5
            simplex = new double[][]{
                    {0.1, 0.05, 0.05, 0.90},
                    {0.12, 0.05, 0.05, 0.90},
                    {0.1, 0.07, 0.05, 0.90},
                    {0.1, 0.05, 0.07, 0.90},
                    {0.1, 0.05, 0.05, 0.85}};

            objective = this::negativeLogLikelihood_N;
        }

        return optimizer.minimize(objective, simplex);
    }

    private Double negativeLogLikelihood_N(double[] params) //NORMAL DISTRIBUTION
    {

        double omega = params[0];
        double alpha = params[1];
        double gamma = params[2];
        double beta  = params[3];

        // GJR stationarity approximation:
        // alpha + beta + 0.5 * gamma < 1
        if (omega <= 0 || alpha < 0 || gamma < 0 || beta < 0 || alpha + beta + 0.5 * gamma >= 1)
        {
            return Double.POSITIVE_INFINITY;
        }

        int T = returns.length;
        double[] sigma2 = new double[T];

        sigma2[0] = variance(returns);

        double logLikelihood = 0.0;

        for (int t = 1; t < T; t++)
        {
            double prevReturn = returns[t - 1];
            double indicator = prevReturn < 0.0 ? 1.0 : 0.0;

            sigma2[t] = omega + alpha * prevReturn * prevReturn + gamma * indicator * prevReturn * prevReturn + beta * sigma2[t - 1];

            if (sigma2[t] <= 0 || Double.isNaN(sigma2[t]) || Double.isInfinite(sigma2[t]))
            {
                return Double.POSITIVE_INFINITY;
            }

            logLikelihood += Math.log(sigma2[t]) + (returns[t] * returns[t]) / sigma2[t];
        }

        return 0.5 * logLikelihood;
    }

    private Double negativeLogLikelihood_S(double[] params) // STUDENT-T DISTRIBUTION
    {
        double omega = params[0];
        double alpha = params[1];
        double gamma = params[2];
        double beta  = params[3];
        double nu    = params[4];

        // Same Student-t constraints as GARCH estimator,
        // with GJR persistence alpha + beta + 0.5 gamma.
        if (omega <= 0 || alpha < 0 || gamma < 0 || beta < 0 || alpha + beta + 0.5 * gamma >= 1 || nu <= 2.01 || nu > 50)
        {
            return Double.POSITIVE_INFINITY;
        }

        int T = returns.length;
        double[] sigma2 = new double[T];

        sigma2[0] = variance(returns);

        double logLikelihood = 0.0;

        double logGammaNuHalf = logGamma(nu / 2.0);
        double logGammaNuPlus1Half = logGamma((nu + 1.0) / 2.0);
        double logPi = Math.log(Math.PI);
        double logNuMinus2 = Math.log(nu - 2.0);

        for (int t = 1; t < T; t++)
        {
            double prevReturn = returns[t - 1];
            double indicator = prevReturn < 0.0 ? 1.0 : 0.0;

            sigma2[t] = omega + alpha * prevReturn * prevReturn + gamma * indicator * prevReturn * prevReturn + beta * sigma2[t - 1];

            if (sigma2[t] <= 0 || Double.isNaN(sigma2[t]) || Double.isInfinite(sigma2[t]))
            {
                return Double.POSITIVE_INFINITY;
            }

            double residual = returns[t];

            double term = residual * residual / (sigma2[t] * (nu - 2.0));

            double logContrib = logGammaNuPlus1Half - logGammaNuHalf
                    - 0.5 * (logPi + logNuMinus2 + Math.log(sigma2[t]))
                    - (nu + 1.0) / 2.0 * Math.log(1.0 + term);

            logLikelihood += logContrib;
        }

        return -logLikelihood;
    }

    private double logGamma(double x)
    {
        return Gamma.logGamma(x);
    }

    private double variance(double[] data)
    {
        double mean = 0.0;

        for (double v : data)
        {
            mean += v;
        }

        mean /= data.length;

        double var = 0.0;

        for (double v : data)
        {
            double diff = v - mean;
            var += diff * diff;
        }

        return var / data.length;
    }
}