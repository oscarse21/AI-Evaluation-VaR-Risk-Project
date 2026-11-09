package TRFM;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class PcaModelRegistry
{
    private final Map<String, PcaResult> byVolSurfaceId = new LinkedHashMap<>();

    public void put(String volSurfaceId, PcaResult pcaResult)
    {
        byVolSurfaceId.put(volSurfaceId, pcaResult);
    }

    public PcaResult get(String volSurfaceId)
    {
        PcaResult result = byVolSurfaceId.get(volSurfaceId);
        if (result == null)
        {
            throw new IllegalArgumentException("Ingen PCA-modell hittades för volSurfaceId=" + volSurfaceId);
        }
        return result;
    }

    public Set<String> getVolSurfaceIds()
    {
        return byVolSurfaceId.keySet();
    }
}
