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
 * DIFFERENTIATED ANALYSIS VERSION:
 * - Separately measures Insertion (add) and Comparison (intersection/contains) times.
 *
 * Analyses:
 * 1. Statistical Analysis: Mean/Variance for BOTH Insertion and Intersection at full size.
 * 2. Intersection Scaling: Line graph data for pure intersection time.
 * 3. Insertion Scaling: Line graph data for pure insertion time.
 */
public class HashPerformanceAnalysis {

    // --- CONFIGURATION ---
    private static final String AIRLINE_FILE = "airline.csv";
    private static final String LOUNGE_FILE = "lounge.csv";
    private static final int AIRLINE_USER_ID_COLUMN = 0;
    private static final int LOUNGE_USER_ID_COLUMN = 0;

    private static final int STATISTICAL_TRIALS = 30;
    private static final double[] SUBSET_PERCENTAGES = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
    private static final int MEDIAN_TRIALS = 15; // Used for both scaling analyses now
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
        warmUp(HashSetSeparateChaining::new, airlineData, loungeData);
        warmUp(HashSetLinearProbing::new, airlineData, loungeData);
        warmUp(HashSetQuadraticProbing::new, airlineData, loungeData);
        System.out.println("Warm-up complete.\n");

        // --- CASE 1: FULL STATISTICAL BREAKDOWN ---
        System.out.println("=== [Case 1] Statistical Analysis (Differentiated) ===");
        runDifferentiatedStats(airlineData, loungeData);
        System.out.println("\n");

        // --- CASE 2: INTERSECTION SCALING ---
        System.out.println("=== [Case 2] Pure Intersection Time Scaling (Comparison) ===");
        runIntersectionScaling(airlineData, loungeData);
        System.out.println("\n");

        // --- CASE 3: INSERTION SCALING ---
        System.out.println("=== [Case 3] Pure Insertion Time Scaling ===");
        runInsertionScaling(airlineData);
    }

    // ==================================================================
    // CASE 1: DIFFERENTIATED STATISTICS
    // ==================================================================

    private static void runDifferentiatedStats(List<String> aData, List<String> lData) {
        System.out.printf("Running %d trials for each implementation...\n", STATISTICAL_TRIALS);

        // Storage for times [0]=Insertion, [1]=Intersection
        List<double[]> scTimes = new ArrayList<>();
        List<double[]> lpTimes = new ArrayList<>();
        List<double[]> qpTimes = new ArrayList<>();

        for (int i = 0; i < STATISTICAL_TRIALS; i++) {
            System.out.print("\rProgress: " + (i + 1) + "/" + STATISTICAL_TRIALS);
            scTimes.add(runSingleTrial(HashSetSeparateChaining::new, aData, lData));
            lpTimes.add(runSingleTrial(HashSetLinearProbing::new, aData, lData));
            qpTimes.add(runSingleTrial(HashSetQuadraticProbing::new, aData, lData));
        }
        System.out.println("\nCalculating differentiated statistics...");

        printDifferentiatedStats("Separate Chaining", scTimes);
        printDifferentiatedStats("Linear Probing", lpTimes);
        printDifferentiatedStats("Quadratic Probing", qpTimes);
    }

    private static double[] runSingleTrial(Supplier<ISet<String>> factory, List<String> aData, List<String> lData) {
        ISet<String> setA = factory.get();
        ISet<String> setB = factory.get();

        // 1. Measure Insertion (Airline data)
        long startInsert = System.nanoTime();
        for (String s : aData) setA.add(s);
        long endInsert = System.nanoTime();

        // 2. Setup Set B (untimed for this specific metric, just setup)
        for (String s : lData) setB.add(s);

        // 3. Measure Intersection (Comparison dominant)
        long startIntersect = System.nanoTime();
        setA.intersection(setB);
        long endIntersect = System.nanoTime();

        return new double[] { (endInsert - startInsert) / 1e6, (endIntersect - startIntersect) / 1e6 };
    }

    private static void printDifferentiatedStats(String name, List<double[]> times) {
        List<Double> insertTimes = new ArrayList<>();
        List<Double> intersectTimes = new ArrayList<>();
        for (double[] pair : times) {
            insertTimes.add(pair[0]);
            intersectTimes.add(pair[1]);
        }

        System.out.println("\n--- " + name + " ---");
        System.out.println(">> INSERTION (Pure `add` time for " + 41455 + " items)");
        printSubStats(insertTimes);
        System.out.println(">> COMPARISON (Pure `intersection` time)");
        printSubStats(intersectTimes);
    }

    private static void printSubStats(List<Double> times) {
        Collections.sort(times);
        double mean = times.stream().mapToDouble(val -> val).average().orElse(0.0);
        double stdDev = Math.sqrt(times.stream().mapToDouble(val -> Math.pow(val - mean, 2)).sum() / (times.size() - 1));
        double ci = 1.96 * (stdDev / Math.sqrt(times.size()));

        System.out.println("   Mean: " + String.format("%.4f", mean) + " ms");
        System.out.println("   Std Dev: " + String.format("%.4f", stdDev) + " ms");
        System.out.println("   95% CI: [" + String.format("%.4f", mean - ci) + ", " + String.format("%.4f", mean + ci) + "]");
        System.out.println("   Median: " + String.format("%.4f", getQuantile(times, 0.5)) + " ms");
    }

    // ==================================================================
    // CASE 2: INTERSECTION SCALING (COMPARISON FOCUS)
    // ==================================================================

    private static void runIntersectionScaling(List<String> aData, List<String> lData) {
        System.out.println("AirlineSize,LoungeSize,SC_Intersect(ms),LP_Intersect(ms),QP_Intersect(ms)");
        for (double pct : SUBSET_PERCENTAGES) {
            List<String> aSub = aData.subList(0, (int) (aData.size() * pct));
            List<String> lSub = lData.subList(0, (int) (lData.size() * pct));

            System.err.print("Processing intersection size " + aSub.size() + "...\r");

            double sc = getMedianIntersectionTime(HashSetSeparateChaining::new, aSub, lSub);
            double lp = getMedianIntersectionTime(HashSetLinearProbing::new, aSub, lSub);
            double qp = getMedianIntersectionTime(HashSetQuadraticProbing::new, aSub, lSub);

            System.out.println(aSub.size() + "," + lSub.size() + "," +
                    String.format("%.4f", sc) + "," + String.format("%.4f", lp) + "," + String.format("%.4f", qp));
        }
        System.err.println("Intersection scaling complete.                ");
    }

    private static double getMedianIntersectionTime(Supplier<ISet<String>> factory, List<String> aData, List<String> lData) {
        List<Double> times = new ArrayList<>();
        for (int i = 0; i < MEDIAN_TRIALS; i++) {
            ISet<String> setA = factory.get();
            ISet<String> setB = factory.get();
            for (String s : aData) setA.add(s);
            for (String s : lData) setB.add(s);

            long start = System.nanoTime();
            setA.intersection(setB);
            long end = System.nanoTime();
            times.add((end - start) / 1e6);
        }
        Collections.sort(times);
        return times.get(MEDIAN_TRIALS / 2);
    }

    // ==================================================================
    // CASE 3: INSERTION SCALING (ALREADY IMPLEMENTED, JUST RENAMED)
    // ==================================================================

    private static void runInsertionScaling(List<String> data) {
        System.out.println("SampleSize,SC_Insert(ms),LP_Insert(ms),QP_Insert(ms)");
        for (double pct : SUBSET_PERCENTAGES) {
            int size = (int) (data.size() * pct);
            List<String> subset = data.subList(0, size);
            System.err.print("Processing insertion size " + size + "...\r");

            double sc = getMedianInsertionTime(HashSetSeparateChaining::new, subset);
            double lp = getMedianInsertionTime(HashSetLinearProbing::new, subset);
            double qp = getMedianInsertionTime(HashSetQuadraticProbing::new, subset);

            System.out.println(size + "," + String.format("%.4f", sc) + "," +
                    String.format("%.4f", lp) + "," + String.format("%.4f", qp));
        }
        System.err.println("Insertion scaling complete.                   ");
    }

    private static double getMedianInsertionTime(Supplier<ISet<String>> factory, List<String> data) {
        List<Double> times = new ArrayList<>();
        for (int i = 0; i < MEDIAN_TRIALS; i++) {
            ISet<String> set = factory.get();
            long start = System.nanoTime();
            for (String item : data) set.add(item);
            long end = System.nanoTime();
            times.add((end - start) / 1e6);
        }
        Collections.sort(times);
        return times.get(MEDIAN_TRIALS / 2);
    }

    // ==================================================================
    // UTILITIES
    // ==================================================================

    private static void warmUp(Supplier<ISet<String>> factory, List<String> a, List<String> l) {
        // Basic run through to trigger JIT compilation for both paths
        ISet<String> s1 = factory.get();
        ISet<String> s2 = factory.get();
        for(int i=0; i<1000 && i<a.size(); i++) s1.add(a.get(i));
        for(int i=0; i<1000 && i<l.size(); i++) s2.add(l.get(i));
        s1.intersection(s2);
    }

    private static List<String> loadUserIDs(String path, int colIdx) {
        List<String> ids = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine();
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
        if (idx + 1 < sorted.size()) {
             return sorted.get(idx) + (sorted.get(idx + 1) - sorted.get(idx)) * (pos - idx);
        }
        return sorted.get(idx);
    }
}
