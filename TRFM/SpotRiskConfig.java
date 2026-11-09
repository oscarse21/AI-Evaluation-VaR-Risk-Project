package TRFM;

public class SpotRiskConfig
{
    private final String spotId;
    private final String columnName;

    public SpotRiskConfig(String spotId, String columnName)
    {
        this.spotId = spotId;
        this.columnName = columnName;
    }

    public String getSpotId()
    {
        return spotId;
    }

    public String getColumnName()
    {
        return columnName;
    }
}