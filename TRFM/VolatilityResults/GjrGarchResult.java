package TRFM.VolatilityResults;

public class GjrGarchResult implements VolatilityResult
{
    private final double[][] sigma;
    private final double[] lastSigma;

    // [factor][0..4] = omega, alpha, gamma, beta, nu
    private final double[][] gjrParams;
    private final double[] unconditionalVariance;

    public GjrGarchResult(double[][] sigma, double[][] gjrParams, double[] unconditionalVariance)
    {
        this.sigma = sigma;
        this.lastSigma = sigma[sigma.length - 1].clone();
        this.gjrParams = gjrParams;
        this.unconditionalVariance = unconditionalVariance;
    }

    @Override
    public double[][] getSigma()
    {
        return sigma;
    }

    @Override
    public double[] getLastSigma()
    {
        return lastSigma;
    }

    @Override
    public int rowCount()
    {
        return sigma.length;
    }

    @Override
    public int colCount()
    {
        return sigma[0].length;
    }

    public double[][] getGjrParams()
    {
        return gjrParams;
    }

    public double[] getUnconditionalVariance()
    {
        return unconditionalVariance;
    }
}
