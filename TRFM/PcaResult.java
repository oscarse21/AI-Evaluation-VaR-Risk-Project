package TRFM;

/** ---------------- STEP 4: PCA ON VOL CHANGES ---------------- */

public class PcaResult
{
    private final String[] inputColumns;
    private final double[][] scores;              // T x k
    private final double[][] loadings;            // m x k
    private final double[] means;                // m
    private final double[] stdDevs;              // m
    private final double[] eigenvalues;
    private final double[] explainedVarianceRatio;

    public PcaResult(String[] inputColumns,
                     double[][] scores,
                     double[][] loadings,
                     double[] means,
                     double[] stdDevs,
                     double[] eigenvalues,
                     double[] explainedVarianceRatio)
    {
        this.inputColumns = inputColumns;
        this.scores = scores;
        this.loadings = loadings;
        this.means = means;
        this.stdDevs = stdDevs;
        this.eigenvalues = eigenvalues;
        this.explainedVarianceRatio = explainedVarianceRatio;
    }

    public String[] getInputColumns()
    {
        return inputColumns;
    }

    public double[][] getScores()
    {
        return scores;
    }

    public double[][] getLoadings()
    {
        return loadings;
    }

    public double[] getMeans()
    {
        return means;
    }

    public double[] getStdDevs()
    {
        return stdDevs;
    }

    public double[] getEigenvalues()
    {
        return eigenvalues;
    }

    public double[] getExplainedVarianceRatio()
    {
        return explainedVarianceRatio;
    }
}