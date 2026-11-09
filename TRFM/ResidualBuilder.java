package TRFM;

import TRFM.VolatilityResults.VolatilityResult;

public class ResidualBuilder
{

    /**
     * Bygger standardiserade residualer:
     * z[t][j] = x[t][j] / sigma[t][j]
     */
    public static ResidualMatrix build(RiskFactorMatrix matrix, VolatilityResult vol_result)
    {
        double[][] x = matrix.getData();
        double[][] sigma = vol_result.getSigma();

        int t = x.length;
        int d = x[0].length;

        if (sigma.length != t || sigma[0].length != d)
        {
            throw new IllegalArgumentException("Dimension mismatch mellan riskfaktormatris och EWMA-matris.");
        }

        double[][] residuals = new double[t][d];

        for (int i = 0; i < t; i++)
        {
            for (int j = 0; j < d; j++)
            {
                double s = sigma[i][j];
                if (Math.abs(s) < 1e-12)
                {
                    s = 1e-8;
                }
                residuals[i][j] = x[i][j] / s;
            }
        }

        return new ResidualMatrix(matrix.getDates(), matrix.getColumnNames(), residuals);
    }
}