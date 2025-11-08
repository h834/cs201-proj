package com.mycompany.app;

import java.util.HashSet;
import java.util.Set;

public class AccuracyTester {

    public static void main(String[] args) {
        // Define Test Parameters 
        // You can change these numbers to test different scenarios!
        int uniqueToA = 7000;
        int uniqueToB = 5000;
        int commonToBoth = 3000; // This is the "true overlap"

        int trueSizeA = uniqueToA + commonToBoth;
        int trueSizeB = uniqueToB + commonToBoth;

        System.out.println("--- Accuracy Test Setup ---");
        System.out.println("Target Set A size: " + trueSizeA);
        System.out.println("Target Set B size: " + trueSizeB);
        System.out.println("Target (True) Overlap: " + commonToBoth);
        System.out.println("---------------------------");

        //Create Ground Truth Sets (java.util.HashSet) 
        // These sets will hold the actual, exact data.
        Set<String> setA = new HashSet<>();
        Set<String> setB = new HashSet<>();

        // Add items that are common to both sets
        for (int i = 0; i < commonToBoth; i++) {
            String item = "common-item-" + i;
            setA.add(item);
            setB.add(item);
        }

        // Add items that are unique to set A
        for (int i = 0; i < uniqueToA; i++) {
            setA.add("a-only-item-" + i);
        }

        // Add items that are unique to set B
        for (int i = 0; i < uniqueToB; i++) {
            setB.add("b-only-item-" + i);
        }

        //Populate Probabilistic Sets
        // Now, we add the exact same data to our probabilistic sets.
        ProbabilisticSet probSetA = new ProbabilisticSet(); //
        ProbabilisticSet probSetB = new ProbabilisticSet(); //

        for (String item : setA) {
            probSetA.add(item); //
        }

        for (String item : setB) {
            probSetB.add(item); //
        }
        
        // Get the estimated cardinalities for A and B
        double estimatedA = probSetA.getEstimate(); //
        double estimatedB = probSetB.getEstimate(); //

        // Get the estimated overlap
        double estimatedOverlap = probSetA.compareOverlap(probSetB); //

        //Report Results
        System.out.println("\n--- Results ---");
        System.out.printf("Set A (True): %d   | (Est): %.2f%n", setA.size(), estimatedA);
        System.out.printf("Set B (True): %d   | (Est): %.2f%n", setB.size(), estimatedB);
        
        System.out.println("\n--- Overlap Comparison ---");
        System.out.printf("True Overlap:    %d%n", commonToBoth);
        System.out.printf("Estimated Overlap: %.2f%n", estimatedOverlap);

        // Calculate the error
        double absoluteError = estimatedOverlap - commonToBoth;
        double percentError = (absoluteError / commonToBoth) * 100.0;

        System.out.printf("\nAbsolute Error: %.2f%n", absoluteError);
        System.out.printf("Percent Error:  %.2f%%%n", percentError);
    }
}