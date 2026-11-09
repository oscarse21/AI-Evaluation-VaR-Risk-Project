package TRFM;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PcaUtils
{
    public static PcaResult fitAndTransform(List<RawFactorRow> rows, VolSurfaceConfig volSurfaceConfig, boolean standardize)
    {
        List<String> volChangeCols = volSurfaceConfig.getNodeColumns().stream().map(c -> FactorIds.volNodeLogChange(volSurfaceConfig.getVolSurfaceId(), c))
                .collect(Collectors.toList());

        return fitAndTransformInternal(rows, volChangeCols, volSurfaceConfig.getNComponents(), standardize);
    }

    private static PcaResult fitAndTransformInternal(List<RawFactorRow> rows, List<String> volChangeCols, int nComponents, boolean standardize)
    {
        double[][] x = extractMatrix(rows, volChangeCols);
        int t = x.length;
        int m = x[0].length;

        double[] means = new double[m];
        double[] stdDevs = new double[m];

        double[][] xScaled = new double[t][m];
        for (int j = 0; j < m; j++)
        {
            double mean = 0.0;
            for (int i = 0; i < t; i++)
            {
                mean += x[i][j];
            }
            mean /= t;
            means[j] = mean;

            double var = 0.0;
            for (int i = 0; i < t; i++)
            {
                double diff = x[i][j] - mean;
                var += diff * diff;
            }
            var /= (t - 1);
            double std = Math.sqrt(var);
            if (std == 0.0)
            {
                std = 1.0;
            }
            stdDevs[j] = std;

            for (int i = 0; i < t; i++)
            {
                double centered = x[i][j] - mean;
                xScaled[i][j] = standardize ? centered / std : centered;
            }
        }

        RealMatrix X = MatrixUtils.createRealMatrix(xScaled);
        RealMatrix covariance = new Covariance(X).getCovarianceMatrix();

        EigenDecomposition eig = new EigenDecomposition(covariance);

        List<EigenPair> pairs = new ArrayList<>();
        for (int i = 0; i < m; i++)
        {
            pairs.add(new EigenPair(eig.getRealEigenvalue(i), eig.getEigenvector(i).toArray()));
        }
        pairs.sort((a, b) -> Double.compare(b.eigenvalue, a.eigenvalue));

        int k = Math.min(nComponents, m);

        double[][] loadings = new double[m][k];
        double[] eigenvalues = new double[k];

        double totalEigen = pairs.stream().mapToDouble(p -> p.eigenvalue).sum();
        double[] explained = new double[k];

        for (int c = 0; c < k; c++)
        {
            EigenPair p = pairs.get(c);
            eigenvalues[c] = p.eigenvalue;
            explained[c] = p.eigenvalue / totalEigen;

            for (int j = 0; j < m; j++)
            {
                loadings[j][c] = p.eigenvector[j];
            }
        }

        RealMatrix L = MatrixUtils.createRealMatrix(loadings);
        RealMatrix scoresMat = X.multiply(L);

        return new PcaResult(volChangeCols.toArray(new String[0]), scoresMat.getData(), loadings, means, stdDevs, eigenvalues, explained);
    }

    public static PcaResult fitAndTransformOnFactorColumns(List<RawFactorRow> rows, List<String> factorColumns, int nComponents, boolean standardize)
    {
        return fitAndTransformInternal(rows, factorColumns, nComponents, standardize);
    }

    private static double[][] extractMatrix(List<RawFactorRow> rows, List<String> cols)
    {
        double[][] x = new double[rows.size()][cols.size()];
        for (int i = 0; i < rows.size(); i++)
        {
            RawFactorRow row = rows.get(i);
            for (int j = 0; j < cols.size(); j++)
            {
                x[i][j] = row.get(cols.get(j));
            }
        }
        return x;
    }

    private static class EigenPair
    {
        double eigenvalue;
        double[] eigenvector;

        EigenPair(double eigenvalue, double[] eigenvector)
        {
            this.eigenvalue = eigenvalue;
            this.eigenvector = eigenvector;
        }
    }
}
