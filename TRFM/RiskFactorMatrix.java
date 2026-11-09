package TRFM;

import java.time.LocalDate;
import java.util.List;

public class RiskFactorMatrix
{
    private final List<LocalDate> dates;
    private final List<String> columnNames;
    private final double[][] data;

    public RiskFactorMatrix(List<LocalDate> dates, List<String> columnNames, double[][] data)
    {
        this.dates = dates;
        this.columnNames = columnNames;
        this.data = data;
    }

    public List<LocalDate> getDates()
    {
        return dates;
    }

    public List<String> getColumnNames()
    {
        return columnNames;
    }

    public double[][] getData()
    {
        return data;
    }

    public int rowCount()
    {
        return data.length;
    }

    public int colCount()
    {
        return data.length == 0 ? 0 : data[0].length;
    }
}