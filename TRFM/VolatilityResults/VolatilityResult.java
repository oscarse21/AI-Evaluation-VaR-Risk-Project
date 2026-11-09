package TRFM.VolatilityResults;

public interface VolatilityResult {
    /** Conditional volatilities sigma[t][j] for t=0..T-1, j=0..d-1. */
    double[][] getSigma();

    /** The last day's conditional volatilities (sigma[T-1][j]). */
    double[] getLastSigma();

    /** Number of time steps (rows). */
    int rowCount();

    /** Number of risk factors (columns). */
    int colCount();
}
