import java.util.*;

public class Main {
    public static void main(String[] args) {

        // ==== Step 1: Choose CSV and type ====
        // For lounges
        String filename = "../archive/lounge.csv";
        String type = "lounge";

        // For airlines, uncomment these lines instead:
        // String filename = "../archive/airline.csv";
        // String type = "airline";

        // ==== Step 2: Load data ====
        long start = System.currentTimeMillis();
        Map<String, DynamicArraySet<String>> data = ReviewLoader.loadReviews(filename, type);
        long loadTime = System.currentTimeMillis() - start;
        System.out.println("Loaded data in ms: " + loadTime);
        System.out.println("Total " + type + "s: " + data.size());

        // ==== Step 3: Print all names for reference ====
        // System.out.println("\nAll " + type + " names in dataset:");
        // for (String name : data.keySet()) {
        //     System.out.println(name);
        // }

        // ==== Step 4: Pick two names manually ====
        // Replace these with exact names from the printed list
        String a1 = "Singapore Airlines Business Class Lounge - Singapore Changi Airport";
        String a2 = "Silverkris Lounge - Sydney Airport";

        DynamicArraySet<String> set1 = data.get(a1);
        DynamicArraySet<String> set2 = data.get(a2);

        if (set1 == null || set2 == null) {
            System.out.println("\nOne of the " + type + "s not found in dataset");
            return;
        }

        // ==== Step 5: Print unique user counts ====
        System.out.println("\n" + a1 + " unique users: " + set1.size());
        System.out.println(a2 + " unique users: " + set2.size());

        // ==== Step 6: Compute intersection ====
        long tStart = System.currentTimeMillis();
        DynamicArraySet<String> inter = set1.intersect(set2);
        long tEnd = System.currentTimeMillis();

        System.out.println("\nCommon users: " + inter.size());
        System.out.println("Intersection time (ms): " + (tEnd - tStart));
    }
}
