package TRFM.AssetModels;

import TRFM.OisCurve;

import java.util.ArrayList;
import java.util.List;

public class BondPricingService
{
    private static final double EPS = 1e-12;

    public static double priceLocal(BondPosition bond, OisCurve curve)
    {
        double pv = 0.0;

        for (BondCashFlow cf : buildCashFlows(bond))
        {
            pv += cf.getAmount() * curve.discountFactor(cf.getTimeYears());
        }

        return pv;
    }

    public static List<BondCashFlow> buildCashFlows(BondPosition bond)
    {
        List<BondCashFlow> cashFlows = new ArrayList<>();

        if (bond.getType() == BondType.ZERO_COUPON)
        {
            cashFlows.add(new BondCashFlow(bond.getMaturityYears(), bond.getNotional()));
            return cashFlows;
        }

        double maturity = bond.getMaturityYears();
        int frequency = bond.getCouponFrequency();
        double couponAmount = bond.getNotional() * bond.getCouponRate() / frequency;

        for (int i = 1; ; i++)
        {
            double t = i / (double) frequency;

            if (t >= maturity - EPS)
            {
                break;
            }

            cashFlows.add(new BondCashFlow(t, couponAmount));
        }

        cashFlows.add(new BondCashFlow(
                maturity,
                couponAmount + bond.getNotional()
        ));

        return cashFlows;
    }
}
