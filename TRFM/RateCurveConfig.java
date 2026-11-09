package TRFM;

import java.util.List;

public class RateCurveConfig
{
    private final String curveId;
    private final List<String> rateColumns;

    public RateCurveConfig(String curveId, List<String> rateColumns)
    {
        this.curveId = curveId;
        this.rateColumns = rateColumns;
    }

    public String getCurveId()
    {
        return curveId;
    }

    public List<String> getRateColumns()
    {
        return rateColumns;
    }

    public String extractTenor(String rateColumn)
    {
        if (rateColumn.startsWith("rate_")) {
            return rateColumn.substring("rate_".length());
        }
        return rateColumn;
    }

    public String getFactorId(String rateColumn)
    {
        return FactorIds.rate(curveId, extractTenor(rateColumn));
    }
}