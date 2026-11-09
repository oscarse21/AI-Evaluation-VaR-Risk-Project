package TRFM.AssetModels;

import java.util.List;

public class OptionPortfolio
{
    private final List<PortfolioOption> positions;

    public OptionPortfolio(List<PortfolioOption> positions)
    {
        this.positions = positions;
    }

    public List<PortfolioOption> getPositions()
    {
        return positions;
    }

    public int size()
    {
        return positions.size();
    }
}
