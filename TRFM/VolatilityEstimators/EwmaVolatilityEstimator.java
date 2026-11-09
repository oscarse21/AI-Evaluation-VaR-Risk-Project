package TRFM.VolatilityEstimators;

import TRFM.RiskFactorMatrix;
import TRFM.VolatilityResults.EwmaResult;

public class EwmaVolatilityEstimator // implement VolModel
{

    public static EwmaResult estimate(RiskFactorMatrix matrix, double lambda)
    {
        double[][] x = matrix.getData();
        int t = x.length;
        int d = x[0].length;

        double[][] sigma = new double[t][d];
        double[] initialSigma = computeColumnStdDevs(x);

        for (int j = 0; j < d; j++)
        {
            sigma[0][j] = Math.max(initialSigma[j], 1e-8);
        }

        // EWMA FORMELN
        for (int i = 1; i < t; i++)
        {
            for (int j = 0; j < d; j++)
            {
                double prevSigma2 = sigma[i - 1][j] * sigma[i - 1][j];
                double prevX2 = x[i - 1][j] * x[i - 1][j];
                sigma[i][j] = Math.sqrt(lambda * prevSigma2 + (1.0 - lambda) * prevX2);

                if (sigma[i][j] < 1e-8)
                {
                    sigma[i][j] = 1e-8;
                }
            }
        }

        return new EwmaResult(matrix.getColumnNames(), matrix.getDates(), sigma, lambda);
    }

    public static double[] forecastNextSigma(RiskFactorMatrix matrix, EwmaResult ewma)
    {
        double[][] x = matrix.getData();
        double[][] sigma = ewma.getSigma();

        int t = x.length;
        int d = x[0].length;

        if (sigma.length != t || sigma[0].length != d)
        {
            throw new IllegalArgumentException("Dimension mismatch mellan x och sigma.");
        }

        double[] nextSigma = new double[d];
        double lambda = ewma.getLambda();

        // EWMA FORMELN
        for (int j = 0; j < d; j++)
        {
            double lastSigma2 = sigma[t - 1][j] * sigma[t - 1][j];
            double lastX2 = x[t - 1][j] * x[t - 1][j];
            nextSigma[j] = Math.sqrt(lambda * lastSigma2 + (1.0 - lambda) * lastX2);

            if (nextSigma[j] < 1e-8)
            {
                nextSigma[j] = 1e-8;
            }
        }

        return nextSigma;
    }

    private static double[] computeColumnStdDevs(double[][] x)
    {
        int t = x.length;
        int d = x[0].length;

        double[] means = new double[d];
        for (int j = 0; j < d; j++)
        {
            for (int i = 0; i < t; i++)
            {
                means[j] += x[i][j];
            }
            means[j] = means[j]/t;
        }

        double[] std = new double[d];
        for (int j = 0; j < d; j++)
        {
            double var = 0.0;
            for (int i = 0; i < t; i++)
            {
                double diff = x[i][j] - means[j];
                var += diff * diff;
            }
            var = var/Math.max(t - 1, 1);
            std[j] = Math.sqrt(var);
            if (std[j] < 1e-8)
            {
                std[j] = 1e-8;
            }
        }

        return std;
    }
}
