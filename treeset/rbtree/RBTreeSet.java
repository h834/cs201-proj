import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Stack;

public class RBTreeSet {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    // Node with color
    private static class Node {
        String key;
        String value;
        Node left;
        Node right;
        Node parent;
        boolean color;
        
        Node(String key, String value, boolean color) {
            this.key = key;
            this.value = value;
            this.color = color;
        }
    }

    private Node root;
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

    public boolean equalsSet(RBTreeSet otherSet) {
        if (this.size() != otherSet.size()) return false;
        Stack<Node> stack1 = new Stack<>();
        Stack<Node> stack2 = new Stack<>();
        
        Node current1 = this.root;
        Node current2 = otherSet.root;
        
        while ((current1 != null || !stack1.isEmpty()) &&
            (current2 != null || !stack2.isEmpty())) {
            
            // Reach the leftmost nodes of both trees
            while (current1 != null) {
                stack1.push(current1);
                current1 = current1.left;
            }
            while (current2 != null) {
                stack2.push(current2);
                current2 = current2.left;
            }
            
            // Pop nodes from both stacks
            current1 = stack1.pop();
            current2 = stack2.pop();
            
            // Compare values
            if (!current1.value.equals(current2.value)) {
                return false;
            }
            
            // Move to the right subtree
            current1 = current1.right;
            current2 = current2.right;
        }
        
        // Both stacks should be empty if trees are equal
        return stack1.isEmpty() && stack2.isEmpty();
    }

    // Helper methods for rotations
    private void rotateLeft(Node x) {
        Node y = x.right;
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

    private void rotateRight(Node y) {
        Node x = y.left;
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
        Node newNode = new Node(key, value, RED);
        if (root == null) {
            root = newNode;
            root.color = BLACK; // Root is always black
            size++;
            return true;
        }

        Node current = root;
        Node parent = null;
        
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
    private void fixInsert(Node z) {
        while (z.parent != null && z.parent.color == RED) {
            if (z.parent == z.parent.parent.left) {
                Node y = z.parent.parent.right; // Uncle
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
                Node y = z.parent.parent.left; // Uncle
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
        Node current = root;
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
        Node z = root;

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

    private void deleteNode(Node z) {
        Node y = z;
        Node x;
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

    private Node minimum(Node node) {
        while (node.left != null)
            node = node.left;
        return node;
    }

    private void transplant(Node u, Node v) {
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
    private void fixDelete(Node x) {
        while (x != root && x.color == BLACK) {
            if (x == x.parent.left) {
                Node w = x.parent.right; // Sibling
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
                Node w = x.parent.left; // Sibling
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

    private void inOrder(Node node) {
        if (node == null) return;
        inOrder(node.left);
        System.out.print(node.value + " ");
        inOrder(node.right);
    }
}