import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BinaryTreeSet<E> {

    //node
    private static class Node<String> {
        String key;
        String value;
        Node<String> left;
        Node<String> right;
        
        Node(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private Node<String> root;
    private int size = 0;

    // Constructor
    public BinaryTreeSet() {
    }
    //hash utility
    private String sha256Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ---- Utility comparison method ----
    private int compare(String a, String b) {
        return a.compareTo(b);
    }

    //insertion
    public boolean add(String value) throws NoSuchAlgorithmException{
        if (value == null)
            throw new NullPointerException("Null values not allowed");
        
        String key = sha256Hex(value);
        if (root == null) {
            root = new Node<>(key,value);
            size++;
            return true;
        }

        Node<String> current = root;
        while (true) {
            int cmp = compare(key, current.key);
            if (cmp == 0) {
                return false; // Duplicate — do not insert
            } else if (cmp < 0) {
                if (current.left == null) {
                    current.left = new Node<>(key,value);
                    size++;
                    return true;
                }
                current = current.left;
            } else {
                if (current.right == null) {
                    current.right = new Node<>(key,value);
                    size++;
                    return true;
                }
                current = current.right;
            }
        }
    }

    public boolean contains(String value) throws NoSuchAlgorithmException{
        String key = sha256Hex(value);
        Node<String> current = root;
        while (current != null) {
            int cmp = compare(key, current.key);
            if (cmp == 0)
                return true;
            current = (cmp < 0) ? current.left : current.right;
        }
        return false;
    }

    public boolean remove(String value) throws NoSuchAlgorithmException{
        if (root == null)
            return false;

        Node<String> parent = null;
        Node<String> current = root;
        String key = sha256Hex(value);

        // Find the node to delete
        while (current != null) {
            int cmp = compare(key, current.key);
            if (cmp == 0)
                break;
            parent = current;
            current = (cmp < 0) ? current.left : current.right;
        }

        if (current == null)
            return false; // Not found

        // Case 1: Node has two children
        if (current.left != null && current.right != null) {
            Node<String> successorParent = current;
            Node<String> successor = current.right;
            while (successor.left != null) {
                successorParent = successor;
                successor = successor.left;
            }
            current.value = successor.value; // Replace value
            current.key = successor.key;
            // Remove successor node
            current = successor;
            parent = successorParent;
        }

        // Case 2: Node has 0 or 1 child
        Node<String> child = (current.left != null) ? current.left : current.right;

        if (parent == null)
            root = child;
        else if (parent.left == current)
            parent.left = child;
        else
            parent.right = child;

        size--;
        return true;
    }

    public int size() {
        return size;
    }

    // ---- Optional: in-order traversal for debugging ----
    public void printInOrder() {
        System.out.print("[");
        inOrder(root);
        System.out.println("]");
    }

    private void inOrder(Node<String> node) {
        if (node == null) return;
        inOrder(node.left);
        System.out.print(node.value + " ");
        inOrder(node.right);
    }

    // ---- Simple test ----
    public static void main(String[] args) throws NoSuchAlgorithmException {
        BinaryTreeSet<String> set = new BinaryTreeSet<>();

        set.add("10");
        set.add("5");
        set.add("15");
        set.add("7");
        set.add("3");

        System.out.println("Set contains 7? " + set.contains("7"));
        System.out.println("Set contains 8? " + set.contains("8"));

        set.printInOrder(); // [3 5 7 10 15]

        set.remove("5");
        set.printInOrder(); // [3 7 10 15]

        System.out.println("Size: " + set.size());
    }
}
