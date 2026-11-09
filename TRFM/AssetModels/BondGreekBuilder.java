package TRFM.AssetModels;

import TRFM.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BondGreekBuilder
{
    private static final String BOND_NO_SPOT_ID = "__BOND_NO_SPOT__";

    public static OptionGreekRow buildGreekRow(
            BondPosition bond,
            OisCurve baseCurve,
            PipelineConfig config,
            double currentFx,
            boolean includeRateFactors,
            boolean useRatePca,
            PcaResult ratePca,
            double rateBump,
            double ratePcBump,
            boolean includeConvexity)
    {
        Map<String, Double> factorSensitivities = new LinkedHashMap<>();
        Map<String, Double> factorSecondOrderSensitivities = new LinkedHashMap<>();
        Map<String, Double> spotFactorCrossSensitivities = new LinkedHashMap<>();

        double baseLocalPrice = BondPricingService.priceLocal(bond, baseCurve);

        if (includeRateFactors)
        {
            if (useRatePca)
            {
                addRatePcSensitivities(bond, baseCurve, config, ratePca, currentFx, ratePcBump, includeConvexity, factorSensitivities, factorSecondOrderSensitivities);
            }
            else
            {
                addBucketRateSensitivities(bond, baseCurve, config, currentFx, rateBump, includeConvexity, factorSensitivities, factorSecondOrderSensitivities);
            }
        }

        /*
         * P_base = FX * P_local
         *
         * dP/dFX = P_local
         *
         * PortfolioPnLCalculator multiplicerar sedan med quantity.
         */
        double fxDelta = FxIds.isNoFx(bond.getFxId()) ? 0.0 : baseLocalPrice;

        return new OptionGreekRow(bond.getInstrumentId(), SpotIds.NO_SPOT_ID, bond.getFxId(), bond.getQuantity(), 0.0, 0.0,
                fxDelta, factorSensitivities, factorSecondOrderSensitivities, spotFactorCrossSensitivities);
    }

    private static void addBucketRateSensitivities(BondPosition bond, OisCurve baseCurve, PipelineConfig config, double currentFx,
                                                   double rateBump, boolean includeConvexity, Map<String, Double> factorSensitivities, Map<String, Double> factorSecondOrderSensitivities)
    {
        double baseLocalPrice = BondPricingService.priceLocal(bond, baseCurve);

        for (String rateCol : config.getRateColumns())
        {
            String tenor = config.extractTenor(rateCol);
            String factorId = config.getRateFactorId(rateCol);

            OisCurve upCurve = baseCurve.bumped(tenor, rateBump);
            OisCurve downCurve = baseCurve.bumped(tenor, -rateBump);

            double upLocalPrice = BondPricingService.priceLocal(bond, upCurve);
            double downLocalPrice = BondPricingService.priceLocal(bond, downCurve);

            double rhoLocal = (upLocalPrice - downLocalPrice) / (2.0 * rateBump);

            factorSensitivities.put(factorId, currentFx * rhoLocal);

            if (includeConvexity)
            {
                double convexityLocal = (upLocalPrice - 2.0 * baseLocalPrice + downLocalPrice) / (rateBump * rateBump);
                factorSecondOrderSensitivities.put(factorId, currentFx * convexityLocal);
            }
        }
    }

    private static void addRatePcSensitivities(BondPosition bond, OisCurve baseCurve, PipelineConfig config, PcaResult ratePca,
                                               double currentFx, double ratePcBump, boolean includeConvexity, Map<String, Double> factorSensitivities, Map<String, Double> factorSecondOrderSensitivities)
    {
        if (ratePca == null)
        {
            throw new IllegalArgumentException("ratePca får inte vara null när useRatePca=true.");
        }

        double[][] loadings = ratePca.getLoadings();
        double[] stdDevs = ratePca.getStdDevs();

        int nTenors = config.getRateColumns().size();
        int nPc = loadings[0].length;

        if (loadings.length != nTenors)
        {
            throw new IllegalArgumentException("Dimension mismatch: ratePca.loadings.length != antal rate columns.");
        }

        double baseLocalPrice = BondPricingService.priceLocal(bond, baseCurve);

        for (int pc = 0; pc < nPc; pc++)
        {
            String factorId = FactorIds.ratePc(config.getRateCurveId(), pc + 1);

            OisCurve upCurve = bumpCurveByRatePc(baseCurve, config, ratePca, pc, ratePcBump);
            OisCurve downCurve = bumpCurveByRatePc(baseCurve, config, ratePca, pc, -ratePcBump);

            double upLocalPrice = BondPricingService.priceLocal(bond, upCurve);
            double downLocalPrice = BondPricingService.priceLocal(bond, downCurve);

            double pcRhoLocal = (upLocalPrice - downLocalPrice) / (2.0 * ratePcBump);

            factorSensitivities.put(factorId, currentFx * pcRhoLocal);

            if (includeConvexity)
            {
                double pcConvexityLocal = (upLocalPrice - 2.0 * baseLocalPrice + downLocalPrice) / (ratePcBump * ratePcBump);

                factorSecondOrderSensitivities.put(factorId, currentFx * pcConvexityLocal);
            }
        }
    }

    private static OisCurve bumpCurveByRatePc(OisCurve baseCurve, PipelineConfig config, PcaResult ratePca, int pcIndexZeroBased, double pcShock)
    {
        OisCurve bumpedCurve = baseCurve;

        double[][] loadings = ratePca.getLoadings();
        double[] stdDevs = ratePca.getStdDevs();

        List<String> rateColumns = config.getRateColumns();

        for (int j = 0; j < rateColumns.size(); j++)
        {
            String tenor = config.extractTenor(rateColumns.get(j));
            double dr = pcShock * stdDevs[j] * loadings[j][pcIndexZeroBased];

            bumpedCurve = bumpedCurve.bumped(tenor, dr);
        }

        return bumpedCurve;
    }
}
