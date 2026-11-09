package TRFM;

import java.time.LocalDate;
import java.util.*;

public class ScenarioGeneratorModel {}

// Step 6-8 EWMA-FHS

// new class EwmaResult

// new class EwmaVolatilityEstimator§

class FhsScenario
{
    private final int historicalDrawIndex;
    private final LocalDate historicalDate;
    private final double[] residualDraw;
    private final double[] simulatedShock;

    public FhsScenario(int historicalDrawIndex, LocalDate historicalDate, double[] residualDraw, double[] simulatedShock)
    {
        this.historicalDrawIndex = historicalDrawIndex;
        this.historicalDate = historicalDate;
        this.residualDraw = residualDraw;
        this.simulatedShock = simulatedShock;
    }

    public int getHistoricalDrawIndex()
    {
        return historicalDrawIndex;
    }

    public LocalDate getHistoricalDate()
    {
        return historicalDate;
    }

    public double[] getResidualDraw()
    {
        return residualDraw;
    }

    public double[] getSimulatedShock()
    {
        return simulatedShock;
    }
}

/**
 * State-independent Filtered Historical Simulation (FHS).
 * Samples standardized historical residual vectors with replacement,
 * then scales each vector by the next-period factor volatility forecast.
 */
class FhsScenarioGenerator {
    private final ResidualMatrix residualMatrix;
    private final Random random;

    public FhsScenarioGenerator(ResidualMatrix residualMatrix, long seed) {
        if (residualMatrix == null || residualMatrix.rowCount() == 0) {
            throw new IllegalArgumentException("Residual matrix must not be empty.");
        }
        this.residualMatrix = residualMatrix;
        this.random = new Random(seed);
    }

    public FhsScenario drawScenario(double[] currentSigma) {
        if (currentSigma == null || currentSigma.length != residualMatrix.colCount()) {
            throw new IllegalArgumentException("Volatility forecast dimension does not match residual matrix.");
        }

        int idx = random.nextInt(residualMatrix.rowCount());
        double[] residualDraw = residualMatrix.getRow(idx).clone();
        double[] shock = new double[residualDraw.length];

        for (int j = 0; j < residualDraw.length; j++) {
            shock[j] = currentSigma[j] * residualDraw[j];
        }

        return new FhsScenario(
                idx,
                residualMatrix.getDates().get(idx),
                residualDraw,
                shock
        );
    }

    public List<FhsScenario> drawScenarios(double[] currentSigma, int nScenarios) {
        if (nScenarios <= 0) {
            throw new IllegalArgumentException("Number of scenarios must be positive.");
        }

        List<FhsScenario> scenarios = new ArrayList<>(nScenarios);
        for (int i = 0; i < nScenarios; i++) {
            scenarios.add(drawScenario(currentSigma));
        }
        return scenarios;
    }
}
