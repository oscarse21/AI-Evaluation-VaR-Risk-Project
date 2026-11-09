package TRFM.Optimize;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

public class Optimizer_NelderMead {

    private final double alpha; // reflection   alpha > 1
    private final double beta;  // expansion    beta > 2
    private final double gamma; // contraction  1 < gamma < 2
    private final double delta; // shrink       0 < delta < 1
    private final double tolerance;  // tolerance for criteria
    private final int maxIterations; // max iterations before stopping

    // Construct with parameters
    public Optimizer_NelderMead(double alpha, double beta, double gamma, double delta,
                                double tolerance, int maxIterations) {
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.delta = delta;
        this.tolerance = tolerance;
        this.maxIterations = maxIterations;
    }
    // Construct with standard parameters
    public Optimizer_NelderMead() {
        this.alpha = 2;
        this.beta = 3;
        this.gamma = 1.5;
        this.delta = 0.5;
        this.tolerance = 1e-5;
        this.maxIterations = 100000;
    }

    public double[] minimize(Function<double[], Double> f, double[][] simplex) {
        int n = simplex[0].length;

        for (int iter = 0; iter < maxIterations; iter++) {

            // 1. Sort simplex by function value
            Arrays.sort(simplex, Comparator.comparingDouble(f::apply));

            double[] best = simplex[0];
            double[] worst = simplex[n];

            // 2. Check termination (simplex size)
            if (simplexSize(simplex, best) < tolerance) {
                return best;
            }

            // 3. Compute centroid of all but worst
            double[] centroid = centroid(simplex, n);

            // 4. Reflection
            double[] xr = add(centroid, scale(subtract(centroid, worst), alpha));
            double fr = f.apply(xr);

            double fBest = f.apply(simplex[0]);
            double fSecondWorst = f.apply(simplex[n - 1]);
            double fWorst = f.apply(worst);

            if (fr < fSecondWorst && fr >= fBest) {
                simplex[n] = xr;
                continue;
            }

            // 5. Expansion
            if (fr < fBest) {
                double[] xe = add(centroid, scale(subtract(xr, centroid), beta));
                if (f.apply(xe) < fr) {
                    simplex[n] = xe;
                } else {
                    simplex[n] = xr;
                }
                continue;
            }

            // 6. Contraction
            double[] xc;
            if (fr < fWorst) {
                // outside contraction
                xc = add(centroid, scale(subtract(xr, centroid), gamma));
            } else {
                // inside contraction
                xc = add(centroid, scale(subtract(worst, centroid), -gamma));
            }

            if (f.apply(xc) < fWorst) {
                simplex[n] = xc;
                continue;
            }

            // 7. Shrink
            for (int i = 1; i < simplex.length; i++) {
                simplex[i] = add(best,
                        scale(subtract(simplex[i], best), delta));
            }
        }

        // Return best found
        Arrays.sort(simplex, Comparator.comparingDouble(f::apply));
        return simplex[0];
    }

    // private stuff

    private double simplexSize(double[][] simplex, double[] best) {
        double maxDist = 0.0;
        for (double[] point : simplex) {
            maxDist = Math.max(maxDist, norm(subtract(point, best)));
        }
        return maxDist;
    }

    private double[] centroid(double[][] simplex, int excludeIndex) {
        int n = simplex[0].length;
        double[] c = new double[n];

        for (int i = 0; i < simplex.length; i++) {
            if (i == excludeIndex) continue;
            for (int j = 0; j < n; j++) {
                c[j] += simplex[i][j];
            }
        }

        for (int j = 0; j < n; j++) {
            c[j] /= (simplex.length - 1);
        }

        return c;
    }

    private double[] add(double[] a, double[] b) {
        double[] res = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = a[i] + b[i];
        }
        return res;
    }

    private double[] subtract(double[] a, double[] b) {
        double[] res = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = a[i] - b[i];
        }
        return res;
    }

    private double[] scale(double[] a, double s) {
        double[] res = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = a[i] * s;
        }
        return res;
    }

    private double norm(double[] a) {
        double sum = 0.0;
        for (double v : a) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }
}
