package TRFM;

import java.util.Map;

public class OptionGreekRow
{
    private final String instrumentId;
    private final String spotId;
    private final String fxId;
    private final double quantity;
    private final double delta;
    private final double gamma;
    private final double fxDelta;
    private final Map<String, Double> factorSensitivities;
    private final Map<String, Double> factorSecondOrderSensitivities;
    private final Map<String, Double> spotFactorCrossSensitivities;

    public OptionGreekRow(String instrumentId,
                          String spotId,
                          String fxId,
                          double quantity,
                          double delta,
                          double gamma,
                          double fxDelta,
                          Map<String, Double> factorSensitivities,
                          Map<String, Double> factorSecondOrderSensitivities,
                          Map<String, Double> spotFactorCrossSensitivities)
    {
        this.instrumentId = instrumentId;
        this.spotId = spotId;
        this.fxId = fxId;
        this.quantity = quantity;
        this.delta = delta;
        this.gamma = gamma;
        this.fxDelta = fxDelta;
        this.factorSensitivities = factorSensitivities;
        this.factorSecondOrderSensitivities = factorSecondOrderSensitivities;
        this.spotFactorCrossSensitivities = spotFactorCrossSensitivities;
    }

    public String getInstrumentId()
    {
        return instrumentId;
    }

    public String getSpotId()
    {
        return spotId;
    }

    public String getFxId()
    {
        return fxId;
    }

    public double getQuantity()
    {
        return quantity;
    }

    public double getDelta()
    {
        return delta;
    }

    public double getGamma()
    {
        return gamma;
    }

    public double getFxDelta()
    {
        return fxDelta;
    }

    public Map<String, Double> getFactorSensitivities()
    {
        return factorSensitivities;
    }

    public Map<String, Double> getFactorSecondOrderSensitivities()
    {
        return factorSecondOrderSensitivities;
    }

    public Map<String, Double> getSpotFactorCrossSensitivities()
    {
        return spotFactorCrossSensitivities;
    }

    public double getSensitivity(String factorName)
    {
        return factorSensitivities.getOrDefault(factorName, 0.0);
    }

    public double getSecondOrderSensitivity(String factorName)
    {
        return factorSecondOrderSensitivities.getOrDefault(factorName, 0.0);
    }

    public double getSpotCrossSensitivity(String factorName)
    {
        return spotFactorCrossSensitivities.getOrDefault(factorName, 0.0);
    }
}
