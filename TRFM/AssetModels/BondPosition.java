package TRFM.AssetModels;

public class BondPosition
{
    private final String instrumentId;
    private final String fxId;
    private final BondType type;
    private final double quantity;
    private final double notional;
    private final double maturityYears;
    private final double couponRate;
    private final int couponFrequency;

    public BondPosition(String instrumentId,
                        String fxId,
                        BondType type,
                        double quantity,
                        double notional,
                        double maturityYears,
                        double couponRate,
                        int couponFrequency)
    {
        if (instrumentId == null || instrumentId.isBlank())
        {
            throw new IllegalArgumentException("instrumentId får inte vara tom.");
        }
        if (fxId == null || fxId.isBlank())
        {
            throw new IllegalArgumentException("fxId får inte vara tom.");
        }
        if (type == null)
        {
            throw new IllegalArgumentException("BondType får inte vara null.");
        }
        if (notional <= 0.0)
        {
            throw new IllegalArgumentException("Notional måste vara positiv.");
        }
        if (maturityYears <= 0.0)
        {
            throw new IllegalArgumentException("Maturity måste vara positiv.");
        }
        if (couponRate < 0.0)
        {
            throw new IllegalArgumentException("Coupon rate får inte vara negativ.");
        }

        if (type == BondType.ZERO_COUPON)
        {
            couponRate = 0.0;
            couponFrequency = 0;
        }
        else
        {
            if (couponFrequency <= 0)
            {
                throw new IllegalArgumentException("Coupon frequency måste vara positiv för FCB.");
            }
        }

        this.instrumentId = instrumentId;
        this.fxId = fxId;
        this.type = type;
        this.quantity = quantity;
        this.notional = notional;
        this.maturityYears = maturityYears;
        this.couponRate = couponRate;
        this.couponFrequency = couponFrequency;
    }

    public static BondPosition zeroCoupon(String instrumentId,
                                          String fxId,
                                          double quantity,
                                          double notional,
                                          double maturityYears)
    {
        return new BondPosition(instrumentId, fxId, BondType.ZERO_COUPON, quantity, notional, maturityYears, 0.0, 0);
    }

    public static BondPosition fixedCoupon(String instrumentId, String fxId, double quantity, double notional, double maturityYears,
                                           double couponRate, int couponFrequency)
    {
        return new BondPosition(instrumentId, fxId, BondType.FIXED_COUPON, quantity, notional, maturityYears, couponRate, couponFrequency);
    }

    public String getInstrumentId()
    {
        return instrumentId;
    }

    public String getFxId()
    {
        return fxId;
    }

    public BondType getType()
    {
        return type;
    }

    public double getQuantity()
    {
        return quantity;
    }

    public double getNotional()
    {
        return notional;
    }

    public double getMaturityYears()
    {
        return maturityYears;
    }

    public double getCouponRate()
    {
        return couponRate;
    }

    public int getCouponFrequency()
    {
        return couponFrequency;
    }
}