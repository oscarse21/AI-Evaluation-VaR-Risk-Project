package TRFM.Calibration;

import TRFM.OisCurve;
import TRFM.PcaModelRegistry;
import TRFM.PcaResult;
import TRFM.ResidualMatrix;
import TRFM.RiskFactorMatrix;

import java.util.List;
import java.util.Map;

public class CalibrationResult
{
    private final RiskFactorMatrix riskFactorMatrix;
    private final ResidualMatrix residuals;
    private final double[] nextSigma;
    private final PcaModelRegistry pcaRegistry;
    private final PcaResult volPca;

    private final Map<String, Double> currentSpotById;
    private final Map<String, Double> currentFxById;
    private final double[] currentNodeVols;
    private final OisCurve baseCurve;

    private final PcaResult ratePca;

    public CalibrationResult(
            RiskFactorMatrix riskFactorMatrix,
            ResidualMatrix residuals,
            double[] nextSigma,
            PcaModelRegistry pcaRegistry,
            PcaResult volPca,
            Map<String, Double> currentSpotById,
            Map<String, Double> currentFxById,
            double[] currentNodeVols,
            OisCurve baseCurve,
            PcaResult ratePca)
    {
        this.riskFactorMatrix = riskFactorMatrix;
        this.residuals = residuals;
        this.nextSigma = nextSigma;
        this.pcaRegistry = pcaRegistry;
        this.volPca = volPca;
        this.ratePca = ratePca;

        this.currentSpotById = currentSpotById;
        this.currentFxById = currentFxById;
        this.currentNodeVols = currentNodeVols;
        this.baseCurve = baseCurve;

    }

    public RiskFactorMatrix getRiskFactorMatrix()
    {
        return riskFactorMatrix;
    }

    public ResidualMatrix getResiduals()
    {
        return residuals;
    }

    public double[] getNextSigma()
    {
        return nextSigma;
    }

    public PcaModelRegistry getPcaRegistry()
    {
        return pcaRegistry;
    }

    public PcaResult getVolPca()
    {
        return volPca;
    }

    public Map<String, Double> getCurrentSpotById()
    {
        return currentSpotById;
    }

    public Map<String, Double> getCurrentFxById()
    {
        return currentFxById;
    }

    public double[] getCurrentNodeVols()
    {
        return currentNodeVols;
    }

    public OisCurve getBaseCurve()
    {
        return baseCurve;
    }
}