package TRFM;

import java.util.List;
import java.util.stream.Collectors;

/** ---------------- CONFIG ---------------- */

public class PipelineConfig
{
    private final String dateColumn;
    private final List<SpotRiskConfig> spots;
    private final List<FxRiskConfig> fxs;
    private final List<RateCurveConfig> rateCurves;
    private final List<VolSurfaceConfig> volSurfaces;

    public PipelineConfig(String dateColumn, List<SpotRiskConfig> spots, List<FxRiskConfig> fxs, List<RateCurveConfig> rateCurves, List<VolSurfaceConfig> volSurfaces)
    {
        this.dateColumn = dateColumn;
        this.spots = spots == null ? List.of() : List.copyOf(spots);
        this.fxs = fxs == null ? List.of() : List.copyOf(fxs);
        this.rateCurves = rateCurves == null ? List.of() : List.copyOf(rateCurves);
        this.volSurfaces = volSurfaces == null ? List.of() : List.copyOf(volSurfaces);
    }

    public String getDateColumn()
    {
        return dateColumn;
    }

    public boolean hasSpots()
    {
        return !spots.isEmpty();
    }

    public SpotRiskConfig getPrimarySpot()
    {
        if (!hasSpots())
        {
            throw new IllegalStateException("Ingen spot-konfiguration finns.");
        }

        return spots.get(0);
    }

    public String getSpotId()
    {
        return getPrimarySpot().getSpotId();
    }

    public String getSpotColumn()
    {
        return getPrimarySpot().getColumnName();
    }

    public List<SpotRiskConfig> getSpots()
    {
        return spots;
    }

    public List<FxRiskConfig> getFxs()
    {
        return fxs;
    }

    public List<RateCurveConfig> getRateCurves()
    {
        return rateCurves;
    }

    public List<VolSurfaceConfig> getVolSurfaces()
    {
        return volSurfaces;
    }

    public boolean hasFx() { return !fxs.isEmpty(); }

    public FxRiskConfig getPrimaryFx()
    {
        if (!hasFx())
        {
            throw new IllegalStateException("Ingen FX-konfiguration finns i PipelineConfig.");
        }
        return fxs.get(0);
    }

    public VolSurfaceConfig getPrimaryVolSurface()
    {
        if (!hasVolSurfaces())
        {
            throw new IllegalStateException("Ingen vol surface-konfiguration finns.");
        }
        return volSurfaces.get(0);
    }

    public String getFxColumn()
    {
        return getPrimaryFx().getColumnName();
    }

    public String getFxId()
    {
        return getPrimaryFx().getFxId();
    }

    public boolean hasRateCurves()
    {
        return !rateCurves.isEmpty();
    }

    public RateCurveConfig getPrimaryRateCurve()
    {
        if (!hasRateCurves())
        {
            throw new IllegalStateException("Ingen rate curve-konfiguration finns.");
        }

        return rateCurves.get(0);
    }

    public String getRateCurveId()
    {
        return getPrimaryRateCurve().getCurveId();
    }

    public List<String> getRateColumns()
    {
        return getPrimaryRateCurve().getRateColumns();
    }

    public String extractTenor(String rateColumn)
    {
        return getPrimaryRateCurve().extractTenor(rateColumn);
    }

    public String getRateFactorId(String rateColumn)
    {
        return getPrimaryRateCurve().getFactorId(rateColumn);
    }

    public boolean hasVolSurfaces()
    {
        return !volSurfaces.isEmpty();
    }

    public String getVolSurfaceId()
    {
        return getPrimaryVolSurface().getVolSurfaceId();
    }

    public List<String> getVolColumns()
    {
        return getPrimaryVolSurface().getNodeColumns();
    }

    public static PipelineConfig spx500Run(List<String> headers)
    {
        List<String> rateColumns = headers.stream()
                .filter(h -> h.startsWith("rate_"))
                .collect(Collectors.toList());

        List<String> volColumns = headers.stream()
                .filter(h -> h.startsWith("vol_"))
                .collect(Collectors.toList());

        return new PipelineConfig(
                "date",
                List.of(new SpotRiskConfig("SPX", "spot")),
                List.of(new FxRiskConfig("EURUSD", "eurusd")),
                List.of(new RateCurveConfig("USD", rateColumns)),
                List.of(new VolSurfaceConfig("SPX_IV", volColumns, 4)));
    }
}