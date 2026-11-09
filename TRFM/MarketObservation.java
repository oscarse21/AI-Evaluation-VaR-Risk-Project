package TRFM;

import java.time.LocalDate;
import java.util.Map;

public class MarketObservation
{
    private final LocalDate date;
    private final Map<String, Double> values;

    public MarketObservation(LocalDate date, Map<String, Double> values)
    {
        this.date = date;
        this.values = values;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public double get(String column)
    {
        Double v = values.get(column);
        if (v == null)
        {
            throw new IllegalArgumentException("Saknar kolumnvärde: " + column + " på datum " + date);
        }
        return v;
    }

    public boolean has(String column)
    {
        return values.containsKey(column);
    }

    public Map<String, Double> getValues()
    {
        return values;
    }
}