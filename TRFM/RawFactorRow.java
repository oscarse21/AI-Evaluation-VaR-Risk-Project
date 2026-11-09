package TRFM;

import java.time.LocalDate;
import java.util.Map;

/** ---------------- STEP 2-3: BUILD RAW FACTOR CHANGES ---------------- */

public class RawFactorRow
{
    private final LocalDate date;
    private final Map<String, Double> factors;

    public RawFactorRow(LocalDate date, Map<String, Double> factors)
    {
        this.date = date;
        this.factors = factors;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public double get(String factorName)
    {
        Double v = factors.get(factorName);
        if (v == null)
        {
            throw new IllegalArgumentException("Saknar faktor: " + factorName + " på datum " + date);
        }
        return v;
    }

    public Map<String, Double> getFactors()
    {
        return factors;
    }
}