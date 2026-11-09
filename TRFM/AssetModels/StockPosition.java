package TRFM.AssetModels;

public class StockPosition
{
    private final String instrumentId;
    private final String spotId;
    private final String fxId;
    private final double quantity;

    public StockPosition(String instrumentId, String spotId, String fxId, double quantity)
    {
        this.instrumentId = instrumentId;
        this.spotId = spotId;
        this.fxId = fxId;
        this.quantity = quantity;
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
}
