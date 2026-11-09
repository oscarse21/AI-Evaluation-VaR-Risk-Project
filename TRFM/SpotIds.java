package TRFM;

public class SpotIds
{
    public static final String NO_SPOT_ID = "__NO_SPOT__";

    public static boolean isNoSpot(String spotId)
    {
        return spotId == null || spotId.isBlank() || NO_SPOT_ID.equals(spotId);
    }
}
