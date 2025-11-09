import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class PerformanceTesterHash {
    
    // Simple Record class for single-value records
    static class Record {
        private String value;
        
        public Record(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    /**
     * Load data from a single-column file (one value per line)
     * @param filepath Path to the file
     * @return List of Records
     */
    public static List<Record> loadYourData(String filepath) {
        List<Record> data = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            
            while ((line = br.readLine()) != null) {
                // Trim whitespace and skip empty lines
                line = line.trim();
                if (!line.isEmpty()) {
                    data.add(new Record(line));
                }
            }
            
            System.out.println("Loaded " + data.size() + " records from " + filepath);
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
        
        return data;
    }
    
    /**
     * without replacement
     */
    public static List<Record> drawSample(List<Record> dataset, int n, Random rng) {
        if (n > dataset.size()) {
            throw new IllegalArgumentException("Sample size cannot exceed dataset size");
        }
        
        List<Record> pool = new ArrayList<>(dataset);
        Collections.shuffle(pool, rng);
        return pool.subList(0, n);
    }

    
    /**
     * Compute summary statistics
     */
    public static String computeStats(List<Long> timings) {
        if (timings.isEmpty()) return "No data";
        
        // Convert to milliseconds for readability
        List<Double> timingsMs = new ArrayList<>();
        for (Long t : timings) {
            timingsMs.add(t / 1_000_000.0);
        }
        
        Collections.sort(timingsMs);
        
        double sum = 0;
        for (double t : timingsMs) {
            sum += t;
        }
        double mean = sum / timingsMs.size();
        
        double variance = 0;
        for (double t : timingsMs) {
            variance += Math.pow(t - mean, 2);
        }
        double stdDev = Math.sqrt(variance / (timingsMs.size() - 1));
        
        int n = timingsMs.size();
        double median;
        if (n % 2 == 0) {
            median = (timingsMs.get(n/2 - 1) + timingsMs.get(n/2)) / 2.0;
        } else {
            median = timingsMs.get(n/2);
        }
        double min = timingsMs.get(0); 
        double max = timingsMs.get(n - 1);
        double q1 = quantile(timingsMs, 0.25);
        double q3 = quantile(timingsMs, 0.75);
        return String.format("Mean: %.2f ms, StdDev: %.2f ms, Median: %.2f ms, Min: %.2f ms, Max: %.2f ms, lower quartile: %.2f ms, upper quartile: %.2f ms",
                           mean,stdDev, median, min, max,q1,q3);
    }

    private static double quantile(List<Double> sortedData, double p) {
            int size = sortedData.size();
            double pos = p * (size - 1);
            int lo = (int) Math.floor(pos);
            int hi = (int) Math.ceil(pos);
            double frac = pos - lo;
            if (hi >= size) {
                hi = size - 1;
            }
            return sortedData.get(lo) + frac * (sortedData.get(hi) - sortedData.get(lo));
    }
    
    public static void exportTimings(Map<Integer, List<Long>> timingsPerSampleSize, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("SampleSize,TimeMs\n"); // header
            for (Map.Entry<Integer, List<Long>> entry : timingsPerSampleSize.entrySet()) {
                int n = entry.getKey();
                for (Long t : entry.getValue()) {
                    writer.write(n + "," + (t/ 1_000_000.0) + "\n"); // convert ns -> ms
                }
            }
            System.out.println("Raw timings exported to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws NoSuchAlgorithmException{
        // Load your dataset (single column, one value per line)
        String csvPath = "airline_users.csv"; // Change to your file path
        List<Record> fullDataset = loadYourData(csvPath);
        
        if (fullDataset.isEmpty()) {
            System.err.println("No data loaded. Exiting.");
            return;
        }
        
        // Setup experiment
        Random rng = new Random(42); 
        int[] sampleSizes = {50,70,90,110,130,150,170,190,210,230,250,270,290,310,330,350,370,390,410,430,450,470,490,510,530,550,570,590,610,630,650,670,690,710,730,750,770,790,810,830,850,870,890,910,930,950,970,990,1010,1030,1050,1070,1090,1110,1130,1150,1170,1190,1210,1230,1250,1270,1290,1310,1330,1350,1370,1390,1410,1430,1450,1470,1490,1510,1530,1550,1570,1590,1610,1630,1650,1670,1690,1710,1730,1750,1770,1790,1810,1830,1850,1870,1890,1910,1930,1950,1970,1990};
        int k = 41; // Number of runs per sample size

        Map<Integer, List<Long>> insertionTimingsMap = new LinkedHashMap<>();
        Map<Integer, List<Long>> comparisonTimingsMap = new LinkedHashMap<>();
        
        System.out.println("\n=== Performance Testing ===");
        System.out.println("Total dataset size: " + fullDataset.size());
        System.out.println("Runs per sample size: " + k);
        System.out.println();
        
        // Run experiments
        for (int n : sampleSizes) {
            if (n > fullDataset.size()) {
                System.out.println("Skipping n=" + n + " (exceeds dataset size)");
                continue;
            }
            
            List<Long> insertion_timings = new ArrayList<>();
            List<Long> comparison_timings = new ArrayList<>();
            
            for (int i = 0; i < k; i++) {
                List<Record> sample1 = drawSample(fullDataset, n/2, rng);
                List<Record> sample2 = drawSample(fullDataset, n/2, rng);

                //insertion timing

                //set 1////////////////////////////////////////////
                HashSetSeparateChaining hashSet1 = new HashSetSeparateChaining();
                long start1 = System.nanoTime();
                for (Record r : sample1) {
                    hashSet1.add(r.getValue());
                }
                long end1 = System.nanoTime();
                insertion_timings.add(end1 - start1);
                //////////////////////////////////////////////////
                
                //set 2////////////////////////////////////////////
                HashSetSeparateChaining hashSet2 = new HashSetSeparateChaining();
                long start2 = System.nanoTime();
                for (Record r : sample2) {
                    hashSet2.add(r.getValue());
                }
                long end2 = System.nanoTime();
                insertion_timings.add(end2 - start2);
                //////////////////////////////////////////////////
                
                //comparison timing
                /////////////////////////////////////////////////
                long startCmp = System.nanoTime();
                hashSet1.intersection(hashSet2);
                long endCmp = System.nanoTime();

                comparison_timings.add(endCmp - startCmp);
        
            }
            insertionTimingsMap.put(n, new ArrayList<>(insertion_timings));
            comparisonTimingsMap.put(n, new ArrayList<>(comparison_timings));

            System.out.println("n=" + n + ": " + computeStats(insertion_timings));
            System.out.println("n=" + n + " (comparison): " + computeStats(comparison_timings));
            System.out.println();
        }
        exportTimings(insertionTimingsMap, "hashset/insertion_timingsSP.csv");
        exportTimings(comparisonTimingsMap, "hashset/comparison_timingsSP.csv");
    }
}