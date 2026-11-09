package TRFM;

import java.util.List;

public class VolSurfaceConfig
{
    private final String volSurfaceId;
    private final List<String> nodeColumns;
    private final int nComponents;

    public VolSurfaceConfig(String volSurfaceId, List<String> nodeColumns, int nComponents)
    {
        this.volSurfaceId = volSurfaceId;
        this.nodeColumns = nodeColumns;
        this.nComponents = nComponents;
    }

    public String getVolSurfaceId()
    {
        return volSurfaceId;
    }

    public List<String> getNodeColumns()
    {
        return nodeColumns;
    }

    public int getNComponents()
    {
        return nComponents;
    }
}