# AI Evaluation VaR Risk Project

A Java project for exploring financial risk-factor modelling, option pricing, filtered historical simulation (FHS), and Value-at-Risk (VaR).

## Requirements

- Java Development Kit (JDK) 17 or later
- Apache Maven 3.9 or later

Maven automatically downloads the required Apache Commons Math 3.6.1 dependency.

## Build and run

From the repository root:

```bash
mvn clean compile
mvn exec:java
```

Alternatively, run `TRFM.RiskFactorPipelineApp.main()` in IntelliJ IDEA after importing the project as Maven.

The default main method runs a **synthetic one-day VaR demo** with 500 simulated historical daily returns, 5,000 FHS scenarios, and a simple 100-share equity portfolio. It reports 95% and 99% one-day VaR. **The default demo does not use real market data and is not a production risk model.**

## Project structure

The existing Java packages live under `TRFM/` in the repository root. `pom.xml` explicitly configures this layout; you do not need to move source files to `src/main/java` to use Maven.

Notable components:

- `TRFM/RiskFactorPipelineApp.java` — runnable one-day VaR example
- `TRFM/ScenarioGeneratorModel.java` — state-independent FHS scenario generation
- `TRFM/Calibration/` — risk-factor and volatility calibration
- `TRFM/VolatilityModels/` — EWMA, GARCH and GJR-GARCH models
- `TRFM/AssetModels/` — portfolio instruments and pricing
- `TRFM/Interpolation/` — volatility-surface interpolation

## License

MIT; see [LICENSE](LICENSE).
