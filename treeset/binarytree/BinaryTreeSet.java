import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Stack;

public class BinaryTreeSet {

    // Node definition for the BST
    private static class Node {
        String value;
        Node left;
        Node right;

        Node(String value) {
            this.value = value;
        }
    }

    private Node root;
    private int size = 0;

    // ---- Utility comparison method ----
    private int compare(String a, String b) {
        return a.compareTo(b);
    }

    // ---- Public API ----
    public boolean add(String value) {
        if (value == null)
            throw new NullPointerException("Null values not allowed");

        if (root == null) {
            root = new Node(value);
            size++;
            return true;
        }

        Node current = root;
        while (true) {
            int cmp = compare(value, current.value);
            if (cmp == 0) {
                return false; // Duplicate — do not insert
            } else if (cmp < 0) {
                if (current.left == null) {
                    current.left = new Node(value);
                    size++;
                    return true;
                }
                current = current.left;
            } else {
                if (current.right == null) {
                    current.right = new Node(value);
                    size++;
                    return true;
                }
                current = current.right;
            }
        }
    }

    public boolean contains(String value) {
        Node current = root;
        while (current != null) {
            int cmp = compare(value, current.value);
            if (cmp == 0)
                return true;
            current = (cmp < 0) ? current.left : current.right;
        }
        return false;
    }

    public boolean remove(String value) {
        if (root == null)
            return false;

        Node parent = null;
        Node current = root;

        // Find the node to delete
        while (current != null) {
            int cmp = compare(value, current.value);
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

    // ---- Optional: in-order traversal for debugging ----
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

    public boolean equalsSet(BinaryTreeSet otherSet) {
        if (this.size() != otherSet.size()) return false;
        Stack<Node> stack1 = new Stack();
        Stack<Node> stack2 = new Stack();
        
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
}

    