package TRFM.VolatilityModels;

import TRFM.GarchDistribution;
import TRFM.RiskFactorMatrix;
import TRFM.VolatilityEstimators.Garch11_Estimator;
import TRFM.VolatilityResults.GarchResult;
import TRFM.VolatilityResults.VolatilityResult;

public class GarchVolatilityModel implements VolatilityModel {

    private final GarchDistribution distribution;

    public GarchVolatilityModel(GarchDistribution distribution) {
        this.distribution = distribution;
    }

    @Override
    public VolatilityResult estimate(RiskFactorMatrix matrix) {
        double[][] returns = matrix.getData();  // T x d
        int T = returns.length;
        int d = returns[0].length;

        double[][] sigma = new double[T][d];
        double[][] garchParams = new double[d][3];    // [omega, alpha, beta]
        double[] unconditionalVars = new double[d];

        for (int j = 0; j < d; j++) {
            // Extract the j-th column series
            double[] series = new double[T];
            for (int i = 0; i < T; i++) {
                series[i] = returns[i][j];
            }

            // Estimate GARCH(1,1) parameters
            Garch11_Estimator estimator = new Garch11_Estimator(series, distribution);
            double[] params = estimator.estimate();  // [omega, alpha, beta]
            garchParams[j] = params.clone();

            double omega = params[0];
            double alpha = params[1];
            double beta  = params[2];

            // Unconditional variance for initialisation
            double unconditionalVar = omega / (1 - alpha - beta);
            unconditionalVars[j] = unconditionalVar;

            // Recursive conditional variance
            double prevVar = unconditionalVar;
            for (int i = 0; i < T; i++) {
                double r = series[i];
                double sqrResid = (i > 0) ? series[i-1] * series[i-1] : unconditionalVar;
                double newVar = omega + alpha * sqrResid + beta * prevVar;
                if (newVar <= 0) newVar = 1e-8;
                sigma[i][j] = Math.sqrt(newVar);
                prevVar = newVar;
            }
        }

        return new GarchResult(sigma, garchParams, unconditionalVars);
    }

    @Override
    public double[] forecastNextSigma(RiskFactorMatrix matrix, VolatilityResult result) {
        if (!(result instanceof GarchResult)) {
            throw new IllegalArgumentException("Result must be a GarchResult");
        }
        GarchResult gres = (GarchResult) result;
        double[][] returns = matrix.getData();
        int T = returns.length;
        int d = returns[0].length;
        double[] nextSigma = new double[d];

        for (int j = 0; j < d; j++) {
            double omega = gres.getGarchParams()[j][0];
            double alpha = gres.getGarchParams()[j][1];
            double beta  = gres.getGarchParams()[j][2];
            double lastVar = gres.getLastSigma()[j] * gres.getLastSigma()[j];
            double lastRet2 = returns[T-1][j] * returns[T-1][j];
            double newVar = omega + alpha * lastRet2 + beta * lastVar;
            if (newVar <= 0) newVar = 1e-8;
            nextSigma[j] = Math.sqrt(newVar);
        }
        return nextSigma;
    }
}