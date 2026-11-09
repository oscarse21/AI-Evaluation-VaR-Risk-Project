package TRFM;

import TRFM.AssetModels.OptionPortfolio;
import TRFM.AssetModels.PortfolioOption;

import java.util.ArrayList;
import java.util.List;

public class AgedOptionPortfolio {
    private final OptionPortfolio original;
    private final int daysOffset;   // positive = older (less time to expiry)

    private AgedOptionPortfolio(OptionPortfolio original, int daysOffset) {
        this.original = original;
        this.daysOffset = daysOffset;
    }

    public static OptionPortfolio of(OptionPortfolio original, int daysOffset) {
        if (daysOffset == 0) return original;   // avoid copying if no change
        List<PortfolioOption> agedPositions = new ArrayList<>();
        for (PortfolioOption opt : original.getPositions()) {
            double newMaturity = opt.getMaturityDays() - daysOffset;
            if (newMaturity < 0) newMaturity = 0;   // expired – you may handle separately
            // Create a new option with the same properties but reduced maturity
            PortfolioOption aged = new PortfolioOption(
                    opt.getInstrumentId(),
                    opt.getSpotId(),
                    opt.getFxId(),
                    opt.getVolSurfaceId(),
                    opt.getOptionType(),
                    opt.getQuantity(),
                    opt.getStrike(),
                    newMaturity,
                    opt.getDividendYieldDecimal()
            );
            agedPositions.add(aged);
        }
        return new OptionPortfolio(agedPositions);
    }
}