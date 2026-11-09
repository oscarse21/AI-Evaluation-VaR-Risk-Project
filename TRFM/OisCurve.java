package TRFM;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class OisCurve
{
    private final NavigableMap<Double, Double> zeroRatesDecimal; // maturityYears -> zero rate in decimal

    public OisCurve(double rate1mPct, double rate3mPct, double rate6mPct, double rate1yPct, double rate2yPct, double rate3yPct, double rate4yPct, double rate5yPct)
    {
        zeroRatesDecimal = new TreeMap<>();
        zeroRatesDecimal.put(1.0 / 12.0, rate1mPct);
        zeroRatesDecimal.put(3.0 / 12.0, rate3mPct);
        zeroRatesDecimal.put(6.0 / 12.0, rate6mPct);
        zeroRatesDecimal.put(1.0, rate1yPct);
        zeroRatesDecimal.put(2.0, rate2yPct);
        zeroRatesDecimal.put(3.0, rate3yPct);
        zeroRatesDecimal.put(4.0, rate4yPct);
        zeroRatesDecimal.put(5.0, rate5yPct);
    }

    public double zeroRate(double maturityYears)
    {
        Map.Entry<Double, Double> floor = zeroRatesDecimal.floorEntry(maturityYears);
        Map.Entry<Double, Double> ceil = zeroRatesDecimal.ceilingEntry(maturityYears);

        if (floor == null) return zeroRatesDecimal.firstEntry().getValue();
        if (ceil == null) return zeroRatesDecimal.lastEntry().getValue();
        if (floor.getKey().equals(ceil.getKey())) return floor.getValue();

        double t0 = floor.getKey();
        double r0 = floor.getValue();
        double t1 = ceil.getKey();
        double r1 = ceil.getValue();

        double w = (maturityYears - t0) / (t1 - t0);
        return r0 + w * (r1 - r0);
    }

    public double discountFactor(double maturityYears)
    {
        double r = zeroRate(maturityYears);
        return Math.exp(-r * maturityYears);
    }

    public OisCurve bumped(String tenor, double bumpDecimal)
    {
        double r1m = zeroRatesDecimal.get(1.0 / 12.0);
        double r3m = zeroRatesDecimal.get(3.0 / 12.0);
        double r6m = zeroRatesDecimal.get(6.0 / 12.0);
        double r1y = zeroRatesDecimal.get(1.0);
        double r2y = zeroRatesDecimal.get(2.0);
        double r3y = zeroRatesDecimal.get(3.0);
        double r4y = zeroRatesDecimal.get(4.0);
        double r5y = zeroRatesDecimal.get(5.0);

        switch (tenor)
        {
            case "1m" -> r1m += bumpDecimal;
            case "3m" -> r3m += bumpDecimal;
            case "6m" -> r6m += bumpDecimal;
            case "1y" -> r1y += bumpDecimal;
            case "2y" -> r2y += bumpDecimal;
            case "3y" -> r3y += bumpDecimal;
            case "4y" -> r4y += bumpDecimal;
            case "5y" -> r5y += bumpDecimal;
            default -> throw new IllegalArgumentException("Unknown tenor: " + tenor);
        }

        return new OisCurve(r1m, r3m, r6m, r1y, r2y, r3y, r4y, r5y);
    }
}