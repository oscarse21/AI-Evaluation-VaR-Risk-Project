package TRFM.VolatilityEstimators;

import TRFM.GarchDistribution;
import TRFM.Optimize.Optimizer_NelderMead;
import org.apache.commons.math3.special.Gamma;

import java.util.function.Function;

public class Garch11_Estimator {

    private final double[] returns;
    private final Optimizer_NelderMead optimizer;
    private final GarchDistribution distribution;

    public Garch11_Estimator(double[] returns, GarchDistribution distribution) {
        this.returns = returns;
        this.optimizer = new Optimizer_NelderMead();
        this.distribution = distribution;
    }

    public double[] estimate() {

        // Initial simplex (omega, alpha, beta, (dof))
        double[][] simplex;
        Function<double[], Double> objective;

        // ST
        if(distribution == GarchDistribution.STUDENT_T) {
            // 4‑parameter simplex: [omega, alpha, beta, nu]
            // rows = nParams + 1 = 5
            simplex = new double[][]{
                    {0.1, 0.05, 0.9, 10.0},
                    {0.12, 0.05, 0.9, 10.0},
                    {0.1, 0.07, 0.9, 10.0},
                    {0.1, 0.05, 0.85, 10.0},
                    {0.1, 0.05, 0.9, 12.0}
            };
            objective = this::negativeLogLikelihood_S;
        }
        else {
            simplex = new double[][]{
                    {0.1, 0.05, 0.9},
                    {0.12, 0.05, 0.9},
                    {0.1, 0.07, 0.9},
                    {0.1, 0.05, 0.85}
            };
            objective = this::negativeLogLikelihood_N;
        }

        return optimizer.minimize(objective, simplex);
    }

    private double negativeLogLikelihood(double[] params) {
        if (distribution == GarchDistribution.NORMAL) {
            return negativeLogLikelihood_N(params);
        } else {
            return negativeLogLikelihood_S(params);
        }
    }

    private Double negativeLogLikelihood_N(double[] params) {

        double omega = params[0];
        double alpha = params[1];
        double beta = params[2];

        // --- Constraints ---
        if (omega <= 0 || alpha < 0 || beta < 0 || alpha + beta >= 1) {
            return Double.POSITIVE_INFINITY;
        }

        int T = returns.length;

        double[] sigma2 = new double[T];

        // Initialize variance with sample variance
        sigma2[0] = variance(returns);

        double logLikelihood = 0.0;

        for (int t = 1; t < T; t++) {
            sigma2[t] = omega
                    + alpha * returns[t - 1] * returns[t - 1]
                    + beta * sigma2[t - 1];

            if (sigma2[t] <= 0) {
                return Double.POSITIVE_INFINITY;
            }

            logLikelihood += Math.log(sigma2[t])
                    + (returns[t] * returns[t]) / sigma2[t];
        }

        return 0.5 * logLikelihood;
    }

    private Double negativeLogLikelihood_S(double[] params) {
        double omega = params[0];
        double alpha = params[1];
        double beta  = params[2];
        double nu    = params[3];   // degrees of freedom

        // Constraints
        if (omega <= 0 || alpha < 0 || beta < 0 || alpha + beta >= 1 || nu <= 2.01 || nu > 50) {
            return Double.POSITIVE_INFINITY;
        }

        int T = returns.length;
        double[] sigma2 = new double[T];

        // Initialize variance with sample variance (or unconditional variance)
        double initVar = variance(returns);
        sigma2[0] = initVar;

        double logLikelihood = 0.0;

        // Precompute constants for Student‑t
        double logGammaNuHalf = logGamma(nu / 2.0);
        double logGammaNuPlus1Half = logGamma((nu + 1) / 2.0);
        double logPi = Math.log(Math.PI);
        double logNuMinus2 = Math.log(nu - 2);

        for (int t = 1; t < T; t++) {
            sigma2[t] = omega
                    + alpha * returns[t-1] * returns[t-1]
                    + beta * sigma2[t-1];

            if (sigma2[t] <= 0) return Double.POSITIVE_INFINITY;

            double residual = returns[t];
            double term = residual * residual / (sigma2[t] * (nu - 2));
            double logContrib = logGammaNuPlus1Half - logGammaNuHalf
                    - 0.5 * (logPi + logNuMinus2 + Math.log(sigma2[t]))
                    - (nu + 1) / 2.0 * Math.log(1 + term);
            logLikelihood += logContrib;
        }

        return -logLikelihood;  // negative log‑likelihood for minimisation
    }

    private double logGamma(double x) {
        return Gamma.logGamma(x); // Apache Commons
    }

    private double variance(double[] data) {
        double mean = 0.0;
        for (double v : data) mean += v;
        mean /= data.length;

        double var = 0.0;
        for (double v : data) {
            double diff = v - mean;
            var += diff * diff;
        }
        return var / data.length;
    }
}