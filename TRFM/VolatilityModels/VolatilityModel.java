package TRFM.VolatilityModels;

import TRFM.RiskFactorMatrix;
import TRFM.VolatilityResults.VolatilityResult;


public interface VolatilityModel {
    /**
     * Estimates conditional volatilities for each day and each factor.
     * @param matrix RiskFactorMatrix (returns)
     * @return a VolatilityResult containing sigma matrix, last sigma, and any parameters
     */
    VolatilityResult estimate(RiskFactorMatrix matrix);

    /**
     * Forecasts next day's volatility for each factor.
     * @param matrix original returns (needed for last residuals)
     * @param result estimated volatility result
     * @return array of next sigma values
     */
    double[] forecastNextSigma(RiskFactorMatrix matrix, VolatilityResult result);
}