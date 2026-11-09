package TRFM.AssetModels;

import TRFM.*;

import java.util.*;

public class BondModel {}

enum BondType
{
    ZERO_COUPON,
    FIXED_COUPON
}

class BondCashFlow
{
    private final double timeYears;
    private final double amount;

    public BondCashFlow(double timeYears, double amount)
    {
        this.timeYears = timeYears;
        this.amount = amount;
    }

    public double getTimeYears()
    {
        return timeYears;
    }

    public double getAmount()
    {
        return amount;
    }
}

enum BondPortfolioFamily
{
    ZCB_ONLY,
    FCB_ONLY,
    SHORT_DURATION,
    LONG_DURATION,
    BARBELL
}

class NamedBondPortfolio
{
    private final String portfolioId;
    private final BondPortfolioFamily family;
    private final List<BondPosition> bonds;

    public NamedBondPortfolio(String portfolioId, BondPortfolioFamily family, List<BondPosition> bonds)
    {
        this.portfolioId = portfolioId;
        this.family = family;
        this.bonds = bonds;
    }

    public String getPortfolioId()
    {
        return portfolioId;
    }

    public BondPortfolioFamily getFamily()
    {
        return family;
    }

    public List<BondPosition> getBonds()
    {
        return bonds;
    }

    public int size()
    {
        return bonds.size();
    }
}

class BondPortfolioGenerator
{
    private static final String FX_ID = "EURUSD";

    public static List<NamedBondPortfolio> generateValidationPortfolios(long seed,
                                                                        int portfoliosPerFamily)
    {
        Random rng = new Random(seed);
        List<NamedBondPortfolio> result = new ArrayList<>();

        for (BondPortfolioFamily family : BondPortfolioFamily.values())
        {
            for (int i = 0; i < portfoliosPerFamily; i++)
            {
                String portfolioId = family.name() + "_" + String.format("%02d", i + 1);

                List<BondPosition> bonds = generatePortfolioBonds(portfolioId, family, rng);

                result.add(new NamedBondPortfolio(portfolioId, family, bonds));
            }
        }

        return result;
    }

    private static List<BondPosition> generatePortfolioBonds(String portfolioId,
                                                             BondPortfolioFamily family,
                                                             Random rng)
    {
        int nBonds = randomInt(rng, 6, 25);
        List<BondPosition> bonds = new ArrayList<>();

        for (int i = 0; i < nBonds; i++)
        {
            BondPosition bond;

            switch (family)
            {
                case ZCB_ONLY:
                    bond = randomZcb(portfolioId, i, rng, 0.25, 5.0);
                    break;

                case FCB_ONLY:
                    bond = randomFcb(portfolioId, i, rng, 0.5, 5.0);
                    break;

                case SHORT_DURATION:
                    bond = randomShortDurationBond(portfolioId, i, rng);
                    break;

                case LONG_DURATION:
                    bond = randomLongDurationBond(portfolioId, i, rng);
                    break;

                case BARBELL:
                    bond = randomBarbellBond(portfolioId, i, rng);
                    break;

                default:
                    throw new IllegalArgumentException("Unknown bond family: " + family);
            }

            bonds.add(bond);
        }

        return bonds;
    }

    private static BondPosition randomShortDurationBond(String portfolioId, int index, Random rng)
    {
        boolean zcb = rng.nextDouble() < 0.45;

        if (zcb)
        {
            return randomZcb(portfolioId, index, rng, 1.0 / 12.0, 1.25);
        }

        return randomFcb(portfolioId, index, rng, 0.25, 1.5);
    }

    private static BondPosition randomLongDurationBond(String portfolioId, int index, Random rng)
    {
        boolean zcb = rng.nextDouble() < 0.25;

        if (zcb)
        {
            return randomZcb(portfolioId, index, rng, 3.0, 5.0);
        }

        return randomFcb(portfolioId, index, rng, 3.0, 5.0);
    }

    private static BondPosition randomBarbellBond(String portfolioId, int index, Random rng)
    {
        boolean shortEnd = rng.nextDouble() < 0.50;
        boolean zcb = rng.nextDouble() < 0.35;

        if (shortEnd)
        {
            if (zcb)
            {
                return randomZcb(portfolioId, index, rng, 1.0 / 12.0, 1.0);
            }

            return randomFcb(portfolioId, index, rng, 0.25, 1.25);
        }

        if (zcb)
        {
            return randomZcb(portfolioId, index, rng, 3.0, 5.0);
        }

        return randomFcb(portfolioId, index, rng, 3.0, 5.0);
    }

    private static BondPosition randomZcb(String portfolioId, int index, Random rng, double minMaturity, double maxMaturity)
    {
        double quantity = randomSignedQuantity(rng);
        double notional = randomNotional(rng);
        double maturity = randomMaturityYears(rng, minMaturity, maxMaturity);

        return BondPosition.zeroCoupon(portfolioId + "_ZCB_" + index, FX_ID, quantity, notional, maturity);
    }

    private static BondPosition randomFcb(String portfolioId, int index, Random rng, double minMaturity, double maxMaturity)
    {
        double quantity = randomSignedQuantity(rng);
        double notional = randomNotional(rng);
        double maturity = randomMaturityYears(rng, minMaturity, maxMaturity);
        double couponRate = randomCouponRate(rng);
        int couponFrequency = randomCouponFrequency(rng);

        return BondPosition.fixedCoupon(portfolioId + "_FCB_" + index, FX_ID, quantity, notional, maturity, couponRate, couponFrequency);
    }

    private static double randomSignedQuantity(Random rng)
    {
        double sign = rng.nextDouble() < 0.70 ? 1.0 : -1.0;

        /*
         * Quantity är positionens storlek.
         * Notional ligger i själva bonden.
         */
        double magnitude = 0.25 + 2.75 * rng.nextDouble();

        return sign * magnitude;
    }

    private static double randomNotional(Random rng)
    {
        /*
         * 100k till 2m.
         */
        double min = 100_000.0;
        double max = 2_000_000.0;

        return min + (max - min) * rng.nextDouble();
    }

    private static double randomCouponRate(Random rng)
    {
        /*
         * 0.5% till 6%.
         */
        return 0.005 + 0.055 * rng.nextDouble();
    }

    private static int randomCouponFrequency(Random rng)
    {
        int[] frequencies = new int[] {1, 2, 4};
        return frequencies[rng.nextInt(frequencies.length)];
    }

    private static double randomMaturityYears(Random rng, double minMaturity, double maxMaturity)
    {
        /*
         * Blandar exakta OIS-tenorer med interpolerade maturities.
         * Det testar både rena tenor-exponeringar och kurvinterpolation.
         */
        double[] commonTenors = new double[]
                {
                        1.0 / 12.0,
                        0.25,
                        0.5,
                        1.0,
                        2.0,
                        3.0,
                        4.0,
                        5.0
                };

        if (rng.nextDouble() < 0.60)
        {
            List<Double> candidates = new ArrayList<>();

            for (double t : commonTenors)
            {
                if (t >= minMaturity && t <= maxMaturity)
                {
                    candidates.add(t);
                }
            }

            if (!candidates.isEmpty())
            {
                return candidates.get(rng.nextInt(candidates.size()));
            }
        }

        return minMaturity + (maxMaturity - minMaturity) * rng.nextDouble();
    }

    private static int randomInt(Random rng, int minInclusive, int maxInclusive)
    {
        return minInclusive + rng.nextInt(maxInclusive - minInclusive + 1);
    }
}

