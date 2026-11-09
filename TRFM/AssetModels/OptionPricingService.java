package TRFM.AssetModels;

import TRFM.BlackScholesPricer;
import TRFM.Interpolation.AkimaVolSurfaceInterpolator;
import TRFM.Interpolation.CubicSplineVolSurfaceInterpolator;
import TRFM.Interpolation.VolInterpolationMethod;
import TRFM.Interpolation.VolSurfaceInterpolator;
import TRFM.OisCurve;
import TRFM.VolSurfaceConfig;

public class OptionPricingService
{
    private static VolInterpolationMethod interpolationMethod = VolInterpolationMethod.BILINEAR;

    public static void setInterpolationMethod(VolInterpolationMethod method)
    {
        interpolationMethod = method;
    }

    public static VolInterpolationMethod getInterpolationMethod()
    {
        return interpolationMethod;
    }

    public static double price(PortfolioOption option, double spot, double[] currentNodeVols, VolSurfaceConfig volSurfaceConfig, OisCurve curve)
    {
        double moneynessPct = option.getMoneynessPct(spot);

        double sigma = switch (interpolationMethod)
        {
            case BILINEAR -> VolSurfaceInterpolator.interpolateVol(currentNodeVols, volSurfaceConfig, moneynessPct, option.getMaturityDays());
            case CUBIC_SPLINE -> CubicSplineVolSurfaceInterpolator.interpolateVol(currentNodeVols, volSurfaceConfig, moneynessPct, option.getMaturityDays());
            case AKIMA -> AkimaVolSurfaceInterpolator.interpolateVol(currentNodeVols, volSurfaceConfig, moneynessPct, option.getMaturityDays());
        };

        double maturityYears = option.getMaturityDays() / 365.0;
        double rateDecimal = curve.zeroRate(maturityYears);
        double dividendYield = option.getDividendYieldDecimal();

        return BlackScholesPricer.priceEuropean(option.getOptionType(), spot, option.getStrike(), maturityYears, sigma, rateDecimal, dividendYield);
    }
}