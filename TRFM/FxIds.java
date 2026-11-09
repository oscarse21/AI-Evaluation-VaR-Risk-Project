package TRFM;

public class FxIds
{
    public static final String NO_FX_ID = "__NO_FX__";

    public static boolean isNoFx(String fxId)
    {
        return fxId == null || fxId.isBlank() || NO_FX_ID.equals(fxId);
    }
}
