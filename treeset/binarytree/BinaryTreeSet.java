import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Stack;

public class BinaryTreeSet {

    //node
    private static class Node {
        String key;
        String value;
        Node left;
        Node right;
        
        Node(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private Node root;
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
            root = new Node(key,value);
            size++;
            return true;
        }

        Node current = root;
        while (true) {
            int cmp = compare(key, current.key);
            if (cmp == 0) {
                return false; // Duplicate — do not insert
            } else if (cmp < 0) {
                if (current.left == null) {
                    current.left = new Node(key,value);
                    size++;
                    return true;
                }
                current = current.left;
            } else {
                if (current.right == null) {
                    current.right = new Node(key,value);
                    size++;
                    return true;
                }
                current = current.right;
            }
        }
    }

    public boolean contains(String value) throws NoSuchAlgorithmException{
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

    public boolean equalsSet(BinaryTreeSet otherSet) {
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

    public boolean remove(String value) throws NoSuchAlgorithmException{
        if (root == null)
            return false;

        Node parent = null;
        Node current = root;
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
            Node successorParent = current;
            Node successor = current.right;
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
        Node child = (current.left != null) ? current.left : current.right;

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
}
