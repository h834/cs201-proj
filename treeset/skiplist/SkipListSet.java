import java.util.Random;

/**
 * SkipListSet that stores arbitrary objects by SHA-256 hashes.
 * Objects do not need to implement Comparable.
 */
public class SkipListSet {

    private static final double P = 0.5;    // Probability for level promotion
    private static final int MAX_LEVEL = 16; // Maximum height of skip list

    private final Node head = new Node(null, MAX_LEVEL);
    private int level = 1;
    private int size = 0;
    private final Random random = new Random();

    // Node class
    private static class Node {
        String value;        
        Node[] forward;

        Node(String value, int level) {
            this.value = value;
            this.forward = new Node[level];
        }
    }


    /** Generate a random level for a new node */
    private int randomLevel() {
        int lvl = 1;
        while (lvl < MAX_LEVEL && random.nextDouble() < P) {
            lvl++;
        }
        return lvl;
    }

    /** Compare hashes lexicographically */
    private int compare(String val1, String val2) {
        return val1.compareTo(val2);
    }

    /** Add an element to the skip list */
    public boolean add(String value) {
        Node[] update = new Node[MAX_LEVEL];
        Node current = head;

        // Find insert position
        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && compare(current.forward[i].value, value) < 0) {
                current = current.forward[i];
            }
            update[i] = current;
        }
        current = current.forward[0];

        // Already exists
        if (current != null && compare(current.value, value) == 0) {
            return false;
        }

        // Insert new node
        int newLevel = randomLevel();
        if (newLevel > level) {
            for (int i = level; i < newLevel; i++) {
                update[i] = head;
            }
            level = newLevel;
        }

        Node newNode = new Node(value, newLevel);
        for (int i = 0; i < newLevel; i++) {
            newNode.forward[i] = update[i].forward[i];
            update[i].forward[i] = newNode;
        }

        size++;
        return true;
    }

    /** Check if the skip list contains a value */
    public boolean contains(String value) {
        Node current = head;

        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && compare(current.forward[i].value, value) < 0) {
                current = current.forward[i];
            }
        }
        current = current.forward[0];

        return current != null && compare(current.value, value) == 0;
    }

    public boolean equalsSet(SkipListSet other) {
        if (other == null) return false;
        if (this.size != other.size) return false;

        Node a = this.head.forward[0];
        Node b = other.head.forward[0];

        while (a != null && b != null) {
            if (!a.value.equals(b.value)) return false;
            a = a.forward[0];
            b = b.forward[0];
        }

        // both should reach null simultaneously
        return a == null && b == null;
    }

    /** Remove a value from the skip list */
    public boolean remove(String value) {
        Node[] update = new Node[MAX_LEVEL];
        Node current = head;

        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && compare(current.forward[i].value, value) < 0) {
                current = current.forward[i];
            }
            update[i] = current;
        }
        current = current.forward[0];

        if (current == null || compare(current.value, value) != 0) {
            return false; // not found
        }

        for (int i = 0; i < level; i++) {
            if (update[i].forward[i] != current) break;
            update[i].forward[i] = current.forward[i];
        }

        while (level > 0 && head.forward[level - 1] == null) {
            level--;
        }

        size--;
        return true;
    }

    /** Get the number of elements */
    public int size() {
        return size;
    }


}
