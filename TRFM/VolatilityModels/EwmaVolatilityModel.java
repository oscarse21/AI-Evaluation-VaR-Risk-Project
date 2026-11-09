package TRFM.VolatilityModels;

import TRFM.RiskFactorMatrix;
import TRFM.VolatilityEstimators.EwmaVolatilityEstimator;
import TRFM.VolatilityResults.EwmaResult;
import TRFM.VolatilityResults.VolatilityResult;


public class EwmaVolatilityModel implements VolatilityModel {
    private final double lambda;

    public EwmaVolatilityModel(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public VolatilityResult estimate(RiskFactorMatrix matrix) {
        return EwmaVolatilityEstimator.estimate(matrix, lambda);
    }

    @Override
    public double[] forecastNextSigma(RiskFactorMatrix matrix, VolatilityResult result) {
        if (!(result instanceof EwmaResult)) {
            throw new IllegalArgumentException("Result must be an EwmaResult");
        }
        return EwmaVolatilityEstimator.forecastNextSigma(matrix, (EwmaResult) result);
    }
}
