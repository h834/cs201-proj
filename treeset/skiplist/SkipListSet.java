import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        String hash;    // SHA-256 hash of value
        String value;        // Original value
        Node[] forward;

        Node(String value, int level) {
            this.value = value;
            this.forward = new Node[level];
            this.hash = value != null ? sha256Hex(value.toString()) : null;
        }
    }

    /** Hashing method: SHA-256 as hex string */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
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
    private int compare(String hash1, String hash2) {
        return hash1.compareTo(hash2);
    }

    /** Add an element to the skip list */
    public boolean add(String value) {
        String hash = sha256Hex(value.toString());
        Node[] update = new Node[MAX_LEVEL];
        Node current = head;

        // Find insert position
        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && compare(current.forward[i].hash, hash) < 0) {
                current = current.forward[i];
            }
            update[i] = current;
        }
        current = current.forward[0];

        // Already exists
        if (current != null && compare(current.hash, hash) == 0) {
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
        String hash = sha256Hex(value.toString());
        Node current = head;

        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && compare(current.forward[i].hash, hash) < 0) {
                current = current.forward[i];
            }
        }
        current = current.forward[0];

        return current != null && compare(current.hash, hash) == 0;
    }

    public boolean equalsSet(SkipListSet other) {
        if (other == null) return false;
        if (this.size != other.size) return false;

        Node a = this.head.forward[0];
        Node b = other.head.forward[0];

        while (a != null && b != null) {
            if (!a.hash.equals(b.hash)) return false;
            a = a.forward[0];
            b = b.forward[0];
        }

        // both should reach null simultaneously
        return a == null && b == null;
    }

    /** Remove a value from the skip list */
    public boolean remove(String value) {
        String hash = sha256Hex(value.toString());
        Node[] update = new Node[MAX_LEVEL];
        Node current = head;

        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && compare(current.forward[i].hash, hash) < 0) {
                current = current.forward[i];
            }
            update[i] = current;
        }
        current = current.forward[0];

        if (current == null || compare(current.hash, hash) != 0) {
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
