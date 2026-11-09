package TRFM.AssetModels;

public class PortfolioOption
{
    private final String instrumentId;
    private final String spotId;
    private final String fxId;
    private final String volSurfaceId;
    private final OptionType optionType;
    private final double quantity;
    private final double strike;
    private final double maturityDays;
    private final double dividendYieldDecimal;

    public PortfolioOption(String instrumentId, String spotId, String fxId, String volSurfaceId, OptionType optionType,
                           double quantity, double strike, double maturityDays, double dividendYieldDecimal)
    {
        this.instrumentId = instrumentId;
        this.spotId = spotId;
        this.fxId = fxId;
        this.volSurfaceId = volSurfaceId;
        this.optionType = optionType;
        this.quantity = quantity;
        this.strike = strike;
        this.maturityDays = maturityDays;
        this.dividendYieldDecimal = dividendYieldDecimal;
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

    public String getVolSurfaceId()
    {
        return volSurfaceId;
    }

    public OptionType getOptionType()
    {
        return optionType;
    }

    public double getQuantity()
    {
        return quantity;
    }

    public double getStrike()
    {
        return strike;
    }

    public double getMaturityDays()
    {
        return maturityDays;
    }

    public double getDividendYieldDecimal()
    {
        return dividendYieldDecimal;
    }

    public double getMaturityYears()
    {
        return maturityDays / 365.0;
    }

    public double getMoneynessPct(double spot)
    {
        return 100.0 * strike / spot;
    }
}
