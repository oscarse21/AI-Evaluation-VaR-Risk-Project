package TRFM.VolatilityResults;

public class GarchResult implements VolatilityResult {

    private final double[][] sigma;
    private final double[] lastSigma;
    private final double[][] garchParams; // [factor][3/4]: omega, alpha, beta, dof
    private final double[] unconditionalVariance; // optional

    public GarchResult(double[][] sigma, double[][] garchParams, double[] unconditionalVariance) {
        this.sigma = sigma;
        this.lastSigma = sigma[sigma.length - 1].clone();
        this.garchParams = garchParams;
        this.unconditionalVariance = unconditionalVariance;
    }

    @Override public double[][] getSigma() { return sigma; }
    @Override public double[] getLastSigma() { return lastSigma; }
    @Override public int rowCount() { return sigma.length; }
    @Override public int colCount() { return sigma[0].length; }

    public double[][] getGarchParams() { return garchParams; }
    public double[] getUnconditionalVariance() { return unconditionalVariance; }
}
