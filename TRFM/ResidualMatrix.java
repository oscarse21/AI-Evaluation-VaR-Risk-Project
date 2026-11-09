package TRFM;

import java.time.LocalDate;
import java.util.List;

public class ResidualMatrix
{
    private final List<LocalDate> dates;
    private final List<String> columnNames;
    private final double[][] residuals;   // T x d

    public ResidualMatrix(List<LocalDate> dates, List<String> columnNames, double[][] residuals)
    {
        this.dates = dates;
        this.columnNames = columnNames;
        this.residuals = residuals;
    }

    public List<LocalDate> getDates()
    {
        return dates;
    }

    public List<String> getColumnNames()
    {
        return columnNames;
    }

    public double[][] getResiduals()
    {
        return residuals;
    }

    public int rowCount()
    {
        return residuals.length;
    }

    public int colCount()
    {
        return residuals.length == 0 ? 0 : residuals[0].length;
    }

    public double[] getRow(int i)
    {
        return residuals[i];
    }
}