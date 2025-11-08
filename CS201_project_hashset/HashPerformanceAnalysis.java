import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * Main class to run a performance and statistical analysis on different
 * ISet (HashSet) implementations.
 *
 * This class performs two main analyses:
 * 1. Statistical Analysis: Runs the full dataset comparison multiple times
 * to calculate mean, variance, quantiles, and confidence intervals.
 * 2. Subset Analysis: Runs the comparison on increasing subsets of the data
 * to generate data for plotting the rate of time increase.
 *
 * Assumes the following files are in the same directory:
 * - ISet.java
 * - HashSetSeparateChaining.java
 * - HashSetLinearProbing.java
 * - HashSetQuadraticProbing.java
 * - airline.csv
 * - lounge.csv
 */
public class HashPerformanceAnalysis {

    // --- CONFIGURATION ---
    // Paths to the data files. Update these if your files are in a different location.
    private static final String AIRLINE_FILE = "airline.csv";
    private static final String LOUNGE_FILE = "lounge.csv";

    // Assumes User ID is in the first column (index 0) for both files.
    // Change these values if your User ID is in a different column.
    private static final int AIRLINE_USER_ID_COLUMN = 0;
    private static final int LOUNGE_USER_ID_COLUMN = 0;

    // Number of trials for the statistical analysis (Case 1).
    // 30 is a good starting point for the Central Limit Theorem.
    private static final int STATISTICAL_TRIALS = 30;

    // Percentage steps for the subset analysis (Case 2).
    // {0.1, 0.2, ..., 1.0}
    private static final double[] SUBSET_PERCENTAGES = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
    // --- END CONFIGURATION ---


    /**
     * Main entry point for the analysis.
     */
    public static void main(String[] args) {
        // Load the full datasets into memory once.
        System.out.println("Loading data from " + AIRLINE_FILE + " and " + LOUNGE_FILE + "...");
        List<String> airlineData = loadUserIDs(AIRLINE_FILE, AIRLINE_USER_ID_COLUMN);
        List<String> loungeData = loadUserIDs(LOUNGE_FILE, LOUNGE_USER_ID_COLUMN);
        System.out.printf("Loaded %d airline records and %d lounge records.\n", airlineData.size(), loungeData.size());
        System.out.println("---");

        if (airlineData.isEmpty() || loungeData.isEmpty()) {
            System.err.println("Error: One or both data files failed to load or are empty. Aborting.");
            return;
        }

        // --- JIT WARM-UP ---
        // Run the process once for each implementation to allow the Java JIT
        // compiler to optimize the code before we start timing.
        System.out.println("Performing JIT warm-up runs...");
        runSingleProcess(HashSetSeparateChaining::new, airlineData, loungeData);
        runSingleProcess(HashSetLinearProbing::new, airlineData, loungeData);
        runSingleProcess(HashSetQuadraticProbing::new, airlineData, loungeData);
        System.out.println("Warm-up complete.");
        System.out.println("==========================================================");


        // --- CASE 1 & 4: STATISTICAL ANALYSIS ---
        System.out.println("Running Statistical Analysis (Case 1)...");
        runStatisticalAnalysis(airlineData, loungeData);
        System.out.println("==========================================================");


        // --- CASE 2: SUBSET ANALYSIS ---
        System.out.println("Running Subset/Superset Analysis (Case 2)...");
        runSubsetAnalysis(airlineData, loungeData);
        System.out.println("==========================================================");
    }

    /**
     * Reads a CSV file and extracts User IDs from a specified column.
     * Assumes the first line is a header and skips it.
     */
    private static List<String> loadUserIDs(String filePath, int columnIndex) {
        List<String> userIDs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header row

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length > columnIndex) {
                    userIDs.add(values[columnIndex].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath + ": " + e.getMessage());
        }
        return userIDs;
    }

    /**
     * The core task: populates two sets and finds their intersection.
     * Returns the time taken in nanoseconds.
     */
    private static long runSingleProcess(Supplier<ISet<String>> setFactory,
                                         List<String> airlineData,
                                         List<String> loungeData) {

        ISet<String> airlineSet = setFactory.get();
        ISet<String> loungeSet = setFactory.get();

        long startTime = System.nanoTime();

        // 1. Populate airline set
        for (String id : airlineData) {
            airlineSet.add(id);
        }

        // 2. Populate lounge set
        for (String id : loungeData) {
            loungeSet.add(id);
        }

        // 3. Find intersection
        ISet<String> overlap = airlineSet.intersection(loungeSet);

        long endTime = System.nanoTime();

        // Optional: Print overlap size to verify correctness
        // System.out.println("Overlap size: " + overlap.size());

        return endTime - startTime;
    }

    /**
     * Runs the analysis N times for each hash method to get statistical data.
     */
    private static void runStatisticalAnalysis(List<String> airlineData, List<String> loungeData) {
        // We will store timings in milliseconds for easier reading.
        List<Double> scTimes = new ArrayList<>();
        List<Double> lpTimes = new ArrayList<>();
        List<Double> qpTimes = new ArrayList<>();

        System.out.printf("Running %d trials for each implementation...\n", STATISTICAL_TRIALS);

        for (int i = 0; i < STATISTICAL_TRIALS; i++) {
            System.out.print("Running trial " + (i + 1) + "/" + STATISTICAL_TRIALS + "...\r");
            scTimes.add(runSingleProcess(HashSetSeparateChaining::new, airlineData, loungeData) / 1_000_000.0);
            lpTimes.add(runSingleProcess(HashSetLinearProbing::new, airlineData, loungeData) / 1_000_000.0);
            qpTimes.add(runSingleProcess(HashSetQuadraticProbing::new, airlineData, loungeData) / 1_000_000.0);
        }
        System.out.println("\nTrials complete. Calculating statistics...");
        System.out.println("---");

        System.out.println("STATISTICAL ANALYSIS RESULTS (All times in milliseconds)");
        System.out.println("---");
        calculateAndPrintStats("Separate Chaining", scTimes);
        System.out.println("---");
        calculateAndPrintStats("Linear Probing", lpTimes);
        System.out.println("---");
        calculateAndPrintStats("Quadratic Probing", qpTimes);
        System.out.println("---");
    }

    /**
     * Helper function to calculate and print all required statistics.
     */
    private static void calculateAndPrintStats(String methodName, List<Double> times) {
        Collections.sort(times); // Sort for quantiles

        // --- Calculations ---
        double sum = 0;
        for (double time : times) {
            sum += time;
        }
        double mean = sum / times.size();

        double sumSqDiff = 0;
        for (double time : times) {
            sumSqDiff += Math.pow(time - mean, 2);
        }
        double variance = sumSqDiff / (times.size() - 1); // Sample variance
        double stdDev = Math.sqrt(variance);

        // 95% CI: mean ± (1.96 * (stdDev / sqrt(n)))
        double ciMargin = 1.96 * (stdDev / Math.sqrt(times.size()));
        double ciLower = mean - ciMargin;
        double ciUpper = mean + ciMargin;

        // Quantiles
        double q1 = getQuantile(times, 0.25);
        double q3 = getQuantile(times, 0.75);

        // --- Printing (FIXED AGAIN) ---
        // Removed all System.out.printf calls to avoid the
        // FormatFlagsConversionMismatchException,
        // replacing them with System.out.println and string concatenation.
        System.out.println("Method: " + methodName);
        System.out.println("  Trials:          " + times.size());
        System.out.println("  Mean (Average):  " + String.format("%.4f", mean) + " ms");
        System.out.println("  Variance:        " + String.format("%.4f", variance) + " ms^2");
        System.out.println("  Std Deviation:   " + String.format("%.4f", stdDev) + " ms");
        System.out.println("  95% CI:          [" + String.format("%.4f", ciLower) + " ms, " + String.format("%.4f", ciUpper) + " ms]");
        System.out.println("  1st Quantile (Q1): " + String.format("%.4f", q1) + " ms");
        System.out.println("  Median (Q2):     " + String.format("%.4f", getQuantile(times, 0.5)) + " ms");
        System.out.println("  3rd Quantile (Q3): " + String.format("%.4f", q3) + " ms");
    }

    /**
     * Calculates a specific quantile (e.g., 0.25 for Q1) from a sorted list.
     * Uses a simple linear interpolation method.
     */
    private static double getQuantile(List<Double> sortedTimes, double quantile) {
        double index = quantile * (sortedTimes.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sortedTimes.get(lower);
        }
        // Linear interpolation
        double fraction = index - lower;
        return sortedTimes.get(lower) + (sortedTimes.get(upper) - sortedTimes.get(lower)) * fraction;
    }


    /**
     * Runs the analysis on increasing subsets of the data.
     */
    private static void runSubsetAnalysis(List<String> airlineData, List<String> loungeData) {
        System.out.println("Generating data for line plots...");
        System.out.println("Note: This may take a moment.");

        // Store results as {Size, Time} pairs
        List<String> scResults = new ArrayList<>();
        List<String> lpResults = new ArrayList<>();
        List<String> qpResults = new ArrayList<>();

        // Add headers for the CSV-like output
        scResults.add("TotalItems,Time(ms)");
        lpResults.add("TotalItems,Time(ms)");
        qpResults.add("TotalItems,Time(ms)");

        for (double percentage : SUBSET_PERCENTAGES) {
            int airlineSubsetSize = (int) (airlineData.size() * percentage);
            int loungeSubsetSize = (int) (loungeData.size() * percentage);
            int totalItems = airlineSubsetSize + loungeSubsetSize;

            System.out.printf("Processing subset: %.0f%% (Total items: %d)\n", percentage * 100, totalItems);

            List<String> airlineSubset = airlineData.subList(0, airlineSubsetSize);
            List<String> loungeSubset = loungeData.subList(0, loungeSubsetSize);

            // Run each implementation once for this subset size.
            // Convert nanoseconds to milliseconds.
            long scTime = runSingleProcess(HashSetSeparateChaining::new, airlineSubset, loungeSubset);
            long lpTime = runSingleProcess(HashSetLinearProbing::new, airlineSubset, loungeSubset);
            long qpTime = runSingleProcess(HashSetQuadraticProbing::new, airlineSubset, loungeSubset);

            scResults.add(String.format("%d,%.4f", totalItems, scTime / 1_000_000.0));
            lpResults.add(String.format("%d,%.4f", totalItems, lpTime / 1_000_000.0));
            qpResults.add(String.format("%d,%.4f", totalItems, qpTime / 1_000_000.0));
        }

        System.out.println("\n--- SUBSET ANALYSIS DATA ---");
        System.out.println("Copy and paste this data into a spreadsheet to plot a line graph.");

        System.out.println("\n--- Separate Chaining ---");
        for (String line : scResults) {
            System.out.println(line);
        }

        System.out.println("\n--- Linear Probing ---");
        for (String line : lpResults) {
            System.out.println(line);
        }

        System.out.println("\n--- Quadratic Probing ---");
        for (String line : qpResults) {
            System.out.println(line);
        }
    }
}