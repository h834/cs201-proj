import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Main class to run a performance and statistical analysis on different
 * ISet (HashSet) implementations.
 *
 * This class performs three main analyses:
 * 1. Statistical Analysis: Full dataset Intersection comparison (Mean, Variance, CI).
 * 2. Subset Analysis: Intersection time vs. Total items (for line graphs).
 * 3. Insertion Median Analysis: Pure insertion time for different sample sizes.
 *
 * Assumes the following files are in the same directory:
 * - ISet.java, HashSetSeparateChaining.java, HashSetLinearProbing.java, HashSetQuadraticProbing.java
 * - airline.csv, lounge.csv
 */
public class HashPerformanceAnalysis {

    // --- CONFIGURATION ---
    private static final String AIRLINE_FILE = "airline.csv";
    private static final String LOUNGE_FILE = "lounge.csv";
    private static final int AIRLINE_USER_ID_COLUMN = 0;
    private static final int LOUNGE_USER_ID_COLUMN = 0;

    // Case 1: Number of trials for full statistical analysis
    private static final int STATISTICAL_TRIALS = 30;

    // Case 2 & 3: Subset percentages {0.1, 0.2, ..., 1.0}
    private static final double[] SUBSET_PERCENTAGES = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};

    // Case 3: Number of trials per subset to find the median (odd number is best)
    private static final int INSERTION_TRIALS = 15;
    // --- END CONFIGURATION ---


    public static void main(String[] args) {
        System.out.println("Loading data...");
        List<String> airlineData = loadUserIDs(AIRLINE_FILE, AIRLINE_USER_ID_COLUMN);
        List<String> loungeData = loadUserIDs(LOUNGE_FILE, LOUNGE_USER_ID_COLUMN);
        System.out.printf("Loaded %d airline records and %d lounge records.\n", airlineData.size(), loungeData.size());

        if (airlineData.isEmpty() || loungeData.isEmpty()) {
            System.err.println("Error: Data files empty or missing. Aborting.");
            return;
        }

        // --- WARM-UP ---
        System.out.println("Warming up JVM...");
        runIntersectionProcess(HashSetSeparateChaining::new, airlineData, loungeData);
        runIntersectionProcess(HashSetLinearProbing::new, airlineData, loungeData);
        runIntersectionProcess(HashSetQuadraticProbing::new, airlineData, loungeData);
        System.out.println("Warm-up complete.\n");

        // --- CASE 1: STATISTICAL ANALYSIS (INTERSECTION) ---
        System.out.println("=== [Case 1] Statistical Analysis (Intersection) ===");
        runStatisticalAnalysis(airlineData, loungeData);
        System.out.println("\n");

        // --- CASE 2: SUBSET ANALYSIS (INTERSECTION RATE) ---
        System.out.println("=== [Case 2] Subset Analysis (Intersection Rate of Increase) ===");
        runSubsetAnalysis(airlineData, loungeData);
        System.out.println("\n");

        // --- CASE 3: INSERTION MEDIAN ANALYSIS ---
        System.out.println("=== [Case 3] Median Insertion Time Analysis ===");
        // We only need one dataset for pure insertion testing, airline is larger.
        runInsertionMedianAnalysis(airlineData);
    }

    // ==================================================================
    // CASE 1 HELPER METHODS
    // ==================================================================

    private static void runStatisticalAnalysis(List<String> airlineData, List<String> loungeData) {
        List<Double> scTimes = new ArrayList<>();
        List<Double> lpTimes = new ArrayList<>();
        List<Double> qpTimes = new ArrayList<>();

        System.out.printf("Running %d trials for each implementation...\n", STATISTICAL_TRIALS);
        for (int i = 0; i < STATISTICAL_TRIALS; i++) {
            System.out.print("\rProgress: " + (i + 1) + "/" + STATISTICAL_TRIALS);
            scTimes.add(runIntersectionProcess(HashSetSeparateChaining::new, airlineData, loungeData) / 1e6);
            lpTimes.add(runIntersectionProcess(HashSetLinearProbing::new, airlineData, loungeData) / 1e6);
            qpTimes.add(runIntersectionProcess(HashSetQuadraticProbing::new, airlineData, loungeData) / 1e6);
        }
        System.out.println("\nCalculating statistics...");
        printStats("Separate Chaining", scTimes);
        printStats("Linear Probing", lpTimes);
        printStats("Quadratic Probing", qpTimes);
    }

    private static void printStats(String name, List<Double> times) {
        Collections.sort(times);
        double sum = 0; for (double t : times) sum += t;
        double mean = sum / times.size();
        double sumSqDiff = 0; for (double t : times) sumSqDiff += Math.pow(t - mean, 2);
        double stdDev = Math.sqrt(sumSqDiff / (times.size() - 1));
        double ci = 1.96 * (stdDev / Math.sqrt(times.size()));

        System.out.println("\n--- " + name + " ---");
        System.out.println("Mean: " + String.format("%.4f", mean) + " ms");
        System.out.println("Std Dev: " + String.format("%.4f", stdDev) + " ms");
        System.out.println("95% CI: [" + String.format("%.4f", mean - ci) + ", " + String.format("%.4f", mean + ci) + "]");
        System.out.println("Median (Q2): " + String.format("%.4f", getQuantile(times, 0.5)) + " ms");
    }

    // ==================================================================
    // CASE 2 HELPER METHODS
    // ==================================================================

    private static void runSubsetAnalysis(List<String> aData, List<String> lData) {
        System.out.println("TotalItems,Separated(ms),Linear(ms),Quadratic(ms)");
        for (double pct : SUBSET_PERCENTAGES) {
            List<String> aSub = aData.subList(0, (int) (aData.size() * pct));
            List<String> lSub = lData.subList(0, (int) (lData.size() * pct));
            int total = aSub.size() + lSub.size();

            double sc = runIntersectionProcess(HashSetSeparateChaining::new, aSub, lSub) / 1e6;
            double lp = runIntersectionProcess(HashSetLinearProbing::new, aSub, lSub) / 1e6;
            double qp = runIntersectionProcess(HashSetQuadraticProbing::new, aSub, lSub) / 1e6;

            System.out.println(total + "," + String.format("%.4f", sc) + "," +
                    String.format("%.4f", lp) + "," + String.format("%.4f", qp));
        }
    }

    // ==================================================================
    // CASE 3 HELPER METHODS (NEW: INSERTION MEDIAN)
    // ==================================================================

    private static void runInsertionMedianAnalysis(List<String> data) {
        System.out.println("Running " + INSERTION_TRIALS + " trials per size to find median insertion time...");
        System.out.println("\n--- INSERTION MEDIAN DATA TABLE ---");
        // Header for the table
        System.out.println("SampleSize,SC_Median(ms),LP_Median(ms),QP_Median(ms)");

        for (double pct : SUBSET_PERCENTAGES) {
            int size = (int) (data.size() * pct);
            List<String> subset = data.subList(0, size);

            System.err.print("Processing size " + size + "...\r"); // Use err to not mess up final output

            double scMedian = getMedianInsertionTime(HashSetSeparateChaining::new, subset);
            double lpMedian = getMedianInsertionTime(HashSetLinearProbing::new, subset);
            double qpMedian = getMedianInsertionTime(HashSetQuadraticProbing::new, subset);

            // The final table row
            System.out.println(size + "," + String.format("%.4f", scMedian) + "," +
                    String.format("%.4f", lpMedian) + "," + String.format("%.4f", qpMedian));
        }
        System.err.println("Done.                               ");
    }

    private static double getMedianInsertionTime(Supplier<ISet<String>> factory, List<String> data) {
        List<Double> times = new ArrayList<>();
        for (int i = 0; i < INSERTION_TRIALS; i++) {
            ISet<String> set = factory.get();
            long start = System.nanoTime();
            for (String item : data) {
                set.add(item);
            }
            long end = System.nanoTime();
            times.add((end - start) / 1_000_000.0); // Convert to ms
        }
        Collections.sort(times);
        return times.get(INSERTION_TRIALS / 2); // Return the median
    }

    // ==================================================================
    // GENERAL UTILITIES
    // ==================================================================

    // Measures: Populate Set A + Populate Set B + Intersection(A, B)
    private static long runIntersectionProcess(Supplier<ISet<String>> factory, List<String> aData, List<String> lData) {
        ISet<String> setA = factory.get();
        ISet<String> setB = factory.get();
        long start = System.nanoTime();
        for (String s : aData) setA.add(s);
        for (String s : lData) setB.add(s);
        setA.intersection(setB);
        return System.nanoTime() - start;
    }

    private static List<String> loadUserIDs(String path, int colIdx) {
        List<String> ids = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > colIdx) ids.add(parts[colIdx].trim());
            }
        } catch (IOException e) { System.err.println("Error reading " + path); }
        return ids;
    }

    private static double getQuantile(List<Double> sorted, double q) {
        double pos = q * (sorted.size() - 1);
        int idx = (int) pos;
        double frac = pos - idx;
        if (idx + 1 < sorted.size()) {
             return sorted.get(idx) + (sorted.get(idx + 1) - sorted.get(idx)) * frac;
        }
        return sorted.get(idx);
    }
}