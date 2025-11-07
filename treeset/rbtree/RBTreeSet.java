import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RBTreeSet<E> {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    // Node with color
    private static class Node<String> {
        String key;
        String value;
        Node<String> left;
        Node<String> right;
        Node<String> parent;
        boolean color;
        
        Node(String key, String value, boolean color) {
            this.key = key;
            this.value = value;
            this.color = color;
        }
    }

    private Node<String> root;
    private int size = 0;

    // Constructor
    public RBTreeSet() {
    }

    // Hash utility
    private String sha256Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // Utility comparison method
    private int compare(String a, String b) {
        return a.compareTo(b);
    }

    // Helper methods for rotations
    private void rotateLeft(Node<String> x) {
        Node<String> y = x.right;
        x.right = y.left;
        if (y.left != null)
            y.left.parent = x;
        y.parent = x.parent;
        if (x.parent == null)
            root = y;
        else if (x == x.parent.left)
            x.parent.left = y;
        else
            x.parent.right = y;
        y.left = x;
        x.parent = y;
    }

    private void rotateRight(Node<String> y) {
        Node<String> x = y.left;
        y.left = x.right;
        if (x.right != null)
            x.right.parent = y;
        x.parent = y.parent;
        if (y.parent == null)
            root = x;
        else if (y == y.parent.left)
            y.parent.left = x;
        else
            y.parent.right = x;
        x.right = y;
        y.parent = x;
    }

    // Insertion
    public boolean add(String value) throws NoSuchAlgorithmException {
        if (value == null)
            throw new NullPointerException("Null values not allowed");
        
        String key = sha256Hex(value);
        
        // Standard BST insertion
        Node<String> newNode = new Node<>(key, value, RED);
        if (root == null) {
            root = newNode;
            root.color = BLACK; // Root is always black
            size++;
            return true;
        }

        Node<String> current = root;
        Node<String> parent = null;
        
        while (current != null) {
            parent = current;
            int cmp = compare(key, current.key);
            if (cmp == 0) {
                return false; // Duplicate
            } else if (cmp < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }

        newNode.parent = parent;
        int cmp = compare(key, parent.key);
        if (cmp < 0)
            parent.left = newNode;
        else
            parent.right = newNode;

        size++;
        fixInsert(newNode);
        return true;
    }

    // Fix red-black tree properties after insertion
    private void fixInsert(Node<String> z) {
        while (z.parent != null && z.parent.color == RED) {
            if (z.parent == z.parent.parent.left) {
                Node<String> y = z.parent.parent.right; // Uncle
                if (y != null && y.color == RED) {
                    // Case 1: Uncle is red
                    z.parent.color = BLACK;
                    y.color = BLACK;
                    z.parent.parent.color = RED;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.right) {
                        // Case 2: z is right child
                        z = z.parent;
                        rotateLeft(z);
                    }
                    // Case 3: z is left child
                    z.parent.color = BLACK;
                    z.parent.parent.color = RED;
                    rotateRight(z.parent.parent);
                }
            } else {
                Node<String> y = z.parent.parent.left; // Uncle
                if (y != null && y.color == RED) {
                    // Case 1: Uncle is red
                    z.parent.color = BLACK;
                    y.color = BLACK;
                    z.parent.parent.color = RED;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.left) {
                        // Case 2: z is left child
                        z = z.parent;
                        rotateRight(z);
                    }
                    // Case 3: z is right child
                    z.parent.color = BLACK;
                    z.parent.parent.color = RED;
                    rotateLeft(z.parent.parent);
                }
            }
        }
        root.color = BLACK;
    }

    public boolean contains(String value) throws NoSuchAlgorithmException {
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

    // Deletion
    public boolean remove(String value) throws NoSuchAlgorithmException {
        if (root == null)
            return false;

        String key = sha256Hex(value);
        Node<String> z = root;

        // Find the node to delete
        while (z != null) {
            int cmp = compare(key, z.key);
            if (cmp == 0)
                break;
            z = (cmp < 0) ? z.left : z.right;
        }

        if (z == null)
            return false; // Not found

        deleteNode(z);
        size--;
        return true;
    }

    private void deleteNode(Node<String> z) {
        Node<String> y = z;
        Node<String> x;
        boolean yOriginalColor = y.color;

        if (z.left == null) {
            x = z.right;
            transplant(z, z.right);
        } else if (z.right == null) {
            x = z.left;
            transplant(z, z.left);
        } else {
            // Node has two children - find successor
            y = minimum(z.right);
            yOriginalColor = y.color;
            x = y.right;
            
            if (y.parent == z) {
                if (x != null) x.parent = y;
            } else {
                transplant(y, y.right);
                y.right = z.right;
                y.right.parent = y;
            }
            
            transplant(z, y);
            y.left = z.left;
            y.left.parent = y;
            y.color = z.color;
        }

        if (yOriginalColor == BLACK && x != null)
            fixDelete(x);
    }

    private Node<String> minimum(Node<String> node) {
        while (node.left != null)
            node = node.left;
        return node;
    }

    private void transplant(Node<String> u, Node<String> v) {
        if (u.parent == null)
            root = v;
        else if (u == u.parent.left)
            u.parent.left = v;
        else
            u.parent.right = v;
        
        if (v != null)
            v.parent = u.parent;
    }

    // Fix red-black tree properties after deletion
    private void fixDelete(Node<String> x) {
        while (x != root && x.color == BLACK) {
            if (x == x.parent.left) {
                Node<String> w = x.parent.right; // Sibling
                if (w.color == RED) {
                    // Case 1: Sibling is red
                    w.color = BLACK;
                    x.parent.color = RED;
                    rotateLeft(x.parent);
                    w = x.parent.right;
                }
                if ((w.left == null || w.left.color == BLACK) &&
                    (w.right == null || w.right.color == BLACK)) {
                    // Case 2: Sibling's children are black
                    w.color = RED;
                    x = x.parent;
                } else {
                    if (w.right == null || w.right.color == BLACK) {
                        // Case 3: Sibling's right child is black
                        if (w.left != null) w.left.color = BLACK;
                        w.color = RED;
                        rotateRight(w);
                        w = x.parent.right;
                    }
                    // Case 4: Sibling's right child is red
                    w.color = x.parent.color;
                    x.parent.color = BLACK;
                    if (w.right != null) w.right.color = BLACK;
                    rotateLeft(x.parent);
                    x = root;
                }
            } else {
                Node<String> w = x.parent.left; // Sibling
                if (w.color == RED) {
                    // Case 1: Sibling is red
                    w.color = BLACK;
                    x.parent.color = RED;
                    rotateRight(x.parent);
                    w = x.parent.left;
                }
                if ((w.right == null || w.right.color == BLACK) &&
                    (w.left == null || w.left.color == BLACK)) {
                    // Case 2: Sibling's children are black
                    w.color = RED;
                    x = x.parent;
                } else {
                    if (w.left == null || w.left.color == BLACK) {
                        // Case 3: Sibling's left child is black
                        if (w.right != null) w.right.color = BLACK;
                        w.color = RED;
                        rotateLeft(w);
                        w = x.parent.left;
                    }
                    // Case 4: Sibling's left child is red
                    w.color = x.parent.color;
                    x.parent.color = BLACK;
                    if (w.left != null) w.left.color = BLACK;
                    rotateRight(x.parent);
                    x = root;
                }
            }
        }
        x.color = BLACK;
    }

    public int size() {
        return size;
    }

    // In-order traversal for debugging
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

    // Test
    public static void main(String[] args) throws NoSuchAlgorithmException {
        RBTreeSet<String> set = new RBTreeSet<>();

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