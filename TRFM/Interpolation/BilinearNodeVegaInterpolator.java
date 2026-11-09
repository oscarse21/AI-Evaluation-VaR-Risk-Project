package TRFM.Interpolation;

import TRFM.VolSurfaceConfig;

import java.util.*;

public class BilinearNodeVegaInterpolator
{
    public static double maturityDaysToMonths(double maturityDays)
    {
        return maturityDays * 12.0 / 365.0;
    }

    public static double[] buildNodeVegas(double scalarVega,
                                          double optionMoneynessPct,
                                          double optionMaturityDays,
                                          VolSurfaceConfig volSurfaceConfig)
    {
        double optionMaturityMonths = maturityDaysToMonths(optionMaturityDays);

        Map<String, Double> weights = buildWeightMapFromMonths(optionMoneynessPct,optionMaturityMonths,volSurfaceConfig);

        double[] nodeVegas = new double[volSurfaceConfig.getNodeColumns().size()];

        for (int i = 0; i < volSurfaceConfig.getNodeColumns().size(); i++)
        {
            String nodeCol = volSurfaceConfig.getNodeColumns().get(i);
            double w = weights.getOrDefault(nodeCol, 0.0);
            nodeVegas[i] = scalarVega * w;
        }

        return nodeVegas;
    }

    public static Map<String, Double> buildWeightMap(double optionMoneynessPct,
                                                     double optionMaturityDays,
                                                     VolSurfaceConfig volSurfaceConfig)
    {
        double optionMaturityMonths = maturityDaysToMonths(optionMaturityDays);
        return buildWeightMapFromMonths(optionMoneynessPct, optionMaturityMonths, volSurfaceConfig);
    }

    private static Map<String, Double> buildWeightMapFromMonths(double optionMoneynessPct,
                                                                double optionMaturityMonths,
                                                                VolSurfaceConfig volSurfaceConfig)
    {
        NavigableSet<Double> maturityGrid = new TreeSet<>();
        NavigableSet<Double> moneynessGrid = new TreeSet<>();
        Map<String, String> columnByKey = new HashMap<>();

        for (String nodeCol : volSurfaceConfig.getNodeColumns())
        {
            NodeInfo info = parseNodeColumn(nodeCol);
            maturityGrid.add(info.maturityMonths);
            moneynessGrid.add(info.moneynessPct);
            columnByKey.put(key(info.maturityMonths, info.moneynessPct), nodeCol);
        }

        double m0 = floorOrFirst(moneynessGrid, optionMoneynessPct);
        double m1 = ceilOrLast(moneynessGrid, optionMoneynessPct);

        double t0 = floorOrFirst(maturityGrid, optionMaturityMonths);
        double t1 = ceilOrLast(maturityGrid, optionMaturityMonths);

        double alpha = (m1 == m0) ? 0.0 : (optionMoneynessPct - m0) / (m1 - m0);
        double beta = (t1 == t0) ? 0.0 : (optionMaturityMonths - t0) / (t1 - t0);

        Map<String, Double> weightsByKey = new LinkedHashMap<>();
        addWeight(weightsByKey, key(t0, m0), (1.0 - alpha) * (1.0 - beta));
        addWeight(weightsByKey, key(t0, m1), alpha * (1.0 - beta));
        addWeight(weightsByKey, key(t1, m0), (1.0 - alpha) * beta);
        addWeight(weightsByKey, key(t1, m1), alpha * beta);

        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : weightsByKey.entrySet())
        {
            String nodeCol = columnByKey.get(entry.getKey());
            if (nodeCol == null)
            {
                throw new IllegalArgumentException("Kunde inte hitta volnod för key=" + entry.getKey());
            }
            result.put(nodeCol, entry.getValue());
        }

        return result;
    }

    private static void addWeight(Map<String, Double> weightsByKey, String key, double weight)
    {
        weightsByKey.put(key, weightsByKey.getOrDefault(key, 0.0) + weight);
    }

    private static double floorOrFirst(NavigableSet<Double> set, double value)
    {
        Double floor = set.floor(value);
        return floor != null ? floor : set.first();
    }

    private static double ceilOrLast(NavigableSet<Double> set, double value)
    {
        Double ceil = set.ceiling(value);
        return ceil != null ? ceil : set.last();
    }

    private static String key(double maturityMonths, double moneynessPct)
    {
        return String.format(Locale.US, "%.8f|%.8f", maturityMonths, moneynessPct);
    }

    private static NodeInfo parseNodeColumn(String nodeCol)
    {
        String[] parts = nodeCol.split("_");
        if (parts.length < 3)
        {
            throw new IllegalArgumentException("Kan inte parsa volkolumn: " + nodeCol);
        }

        String tenorLabel = parts[1].toLowerCase(Locale.ROOT);
        String moneynessLabel = parts[2];

        double maturityMonths = tenorToMonths(tenorLabel);
        double moneynessPct = Double.parseDouble(moneynessLabel);

        return new NodeInfo(nodeCol, maturityMonths, moneynessPct);
    }

    private static double tenorToMonths(String tenorLabel)
    {
        tenorLabel = tenorLabel.toLowerCase(Locale.ROOT);

        if (tenorLabel.endsWith("d"))
        {
            double days = Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
            return days * 12.0 / 365.0;
        }
        if (tenorLabel.endsWith("m"))
        {
            return Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
        }
        if (tenorLabel.endsWith("y"))
        {
            return 12.0 * Double.parseDouble(tenorLabel.substring(0, tenorLabel.length() - 1));
        }

        throw new IllegalArgumentException("Okänd tenor-label: " + tenorLabel);
    }

    private static class NodeInfo
    {
        private final String nodeCol;
        private final double maturityMonths;
        private final double moneynessPct;

        private NodeInfo(String nodeCol, double maturityMonths, double moneynessPct)
        {
            this.nodeCol = nodeCol;
            this.maturityMonths = maturityMonths;
            this.moneynessPct = moneynessPct;
        }
    }
}