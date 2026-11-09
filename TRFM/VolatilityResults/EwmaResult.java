package TRFM.VolatilityResults;

import java.time.LocalDate;
import java.util.List;

public class EwmaResult implements VolatilityResult
{
    private final List<String> columnNames;
    private final List<LocalDate> dates;
    private final double[][] sigma;   // T x d
    private final double lambda;

    public EwmaResult(List<String> columnNames, List<LocalDate> dates, double[][] sigma, double lambda)
    {
        this.columnNames = columnNames;
        this.dates = dates;
        this.sigma = sigma;
        this.lambda = lambda;
    }

    public List<String> getColumnNames()
    {
        return columnNames;
    }

    public List<LocalDate> getDates()
    {
        return dates;
    }

    public double[][] getSigma()
    {
        return sigma;
    }

    public double getLambda()
    {
        return lambda;
    }

    public int rowCount()
    {
        return sigma.length;
    }

    public int colCount()
    {
        return sigma.length == 0 ? 0 : sigma[0].length;
    }

    @Override
    public double[] getLastSigma() {
        return getLastSigmaRow();
    }

    public double[] getLastSigmaRow()
    {
        return sigma[sigma.length - 1];
    }
}
