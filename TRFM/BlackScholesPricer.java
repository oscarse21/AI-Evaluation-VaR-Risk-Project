package TRFM;

import TRFM.AssetModels.OptionType;
import org.apache.commons.math3.distribution.NormalDistribution;

public class BlackScholesPricer
{
    private static final NormalDistribution N = new NormalDistribution();

    public static double priceEuropean(
            OptionType optionType,
            double spot,
            double strike,
            double maturityYears,
            double sigma,
            double rateDecimal,
            double dividendYieldDecimal)
    {
        if (maturityYears <= 0.0)
        {
            double intrinsic = switch (optionType)
            {
                case CALL -> Math.max(spot - strike, 0.0);
                case PUT -> Math.max(strike - spot, 0.0);
            };
            return intrinsic;
        }

        if (sigma <= 1e-12)
        {
            double forwardLike = spot * Math.exp(-dividendYieldDecimal * maturityYears)
                    - strike * Math.exp(-rateDecimal * maturityYears);
            return switch (optionType)
            {
                case CALL -> Math.max(forwardLike, 0.0);
                case PUT -> Math.max(-forwardLike, 0.0);
            };
        }

        double sqrtT = Math.sqrt(maturityYears);
        double d1 = (Math.log(spot / strike)
                + (rateDecimal - dividendYieldDecimal + 0.5 * sigma * sigma) * maturityYears)
                / (sigma * sqrtT);
        double d2 = d1 - sigma * sqrtT;

        return switch (optionType)
        {
            case CALL -> spot * Math.exp(-dividendYieldDecimal * maturityYears) * N.cumulativeProbability(d1)
                    - strike * Math.exp(-rateDecimal * maturityYears) * N.cumulativeProbability(d2);
            case PUT -> strike * Math.exp(-rateDecimal * maturityYears) * N.cumulativeProbability(-d2)
                    - spot * Math.exp(-dividendYieldDecimal * maturityYears) * N.cumulativeProbability(-d1);
        };
    }
}
