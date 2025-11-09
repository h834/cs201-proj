package com.mycompany.app;
import org.apache.datasketches.hll.HllSketch;
import org.apache.datasketches.hll.TgtHllType;
import org.apache.datasketches.hll.Union;

/**
 * Represents a single probabilistic set using an HLL sketch
 * to count unique items.
 */
public class ProbabilisticSet {

    // Parameter that controls the accuracy of the model
    // This is a good default.
    private static final int DEFAULT_LOG_K = 16;

    // This sketch will store the unique items for this one set.
    private HllSketch sketch;

    /**
     * Creates a new counter with default accuracy.
     */
    public ProbabilisticSet() {
        this.sketch = new HllSketch(DEFAULT_LOG_K);
    }

    /**
     * Creates a new counter with specified accuracy.
     * @param logK The log-base-2 of the number of registers (controls accuracy and size)
     */
    public ProbabilisticSet(int logK) {
        this.sketch = new HllSketch(logK);
    }

    /**
     * Adds a single item (like an author's name or a CSV line) 
     * to the set.
     *
     * @param item The string item to add.
     */
    public void add(String item) {
        if (item != null && !item.isEmpty()) {
            sketch.update(item);
        }
    }

    /**
     * Gets the estimated number of unique items added to this set.
     *
     * @return The estimated cardinality.
     */
    public double getEstimate() {
        return sketch.getEstimate();
    }

    /**
     * Calculates the estimated overlap (intersection) between this
     * set and another ProbabilisticSet set.
     *
     * @param other The other set to compare against.
     * @return The estimated size of the intersection (A ∩ B).
     */
    public double compareOverlap(ProbabilisticSet other) {
        if (other == null) {
            return 0;
        }

        // Get the estimated cardinalities for this (A) and other (B)
        double countA = this.getEstimate();
        double countB = other.getEstimate();

        // Create a Union object to merge the sketches
        Union union = new Union(DEFAULT_LOG_K);
        union.update(this.sketch);
        union.update(other.sketch);
        HllSketch unionSketch = union.getResult(TgtHllType.HLL_4);

        // Get the estimated cardinality of the union (A U B)
        double countUnion = unionSketch.getEstimate();

        // Apply the Inclusion-Exclusion Principle:
        // |A ∩ B| = |A| + |B| - |A U B|
        double intersection = countA + countB - countUnion;

        // Ensure intersection isn't negative due to statistical variance
        if (intersection < 0) {
            return 0;
        }
        
        return intersection;
    }
    
    /**
     * Returns the underlying HllSketch.
     * This is useful for more advanced operations, like in our
     * AirlineReviewAnalyzer.
     */
    protected HllSketch getSketch() {
        return this.sketch;
    }
}