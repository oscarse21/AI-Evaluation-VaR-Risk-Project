package TRFM;

public class FxRiskConfig
{
    private final String fxId;
    private final String columnName;

    public FxRiskConfig(String fxId, String columnName)
    {
        this.fxId = fxId;
        this.columnName = columnName;
    }

    public String getFxId()
    {
        return fxId;
    }

    public String getColumnName()
    {
        return columnName;
    }
}