package TRFM;

public class FactorIds
{
    public static String spot(String spotId)
    {
        return spotId + "_spot_log_return";
    }

    public static String fx(String fxId)
    {
        return fxId + "_fx_log_return";
    }

    public static String rate(String curveId, String tenor)
    {
        return curveId + "_rate_" + tenor + "_diff";
    }

    public static String volNodeLogChange(String volSurfaceId, String nodeColumn)
    {
        return volSurfaceId + "_" + nodeColumn + "_log_change";
    }

    public static String volPc(String volSurfaceId, int pcIndex1Based)
    {
        return volSurfaceId + "_vol_pc" + pcIndex1Based;
    }

    public static String ratePc(String curveId, int pcIndex1Based)
    {
        return curveId + "_rate_pc" + pcIndex1Based;
    }
}
