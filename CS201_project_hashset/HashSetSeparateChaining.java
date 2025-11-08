import java.util.LinkedList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A HashSet implementation that uses Separate Chaining to handle collisions.
 * Each bucket in the array holds a LinkedList of all items that hash to that bucket.
 *
 * This is your "Baseline" implementation.
 */
public class HashSetSeparateChaining<T> implements ISet<T> {

    private static final int DEFAULT_CAPACITY = 16;
    // Load factor: (number of items) / (array size)
    // We resize when the load factor exceeds this. 0.75 is a standard value.
    private static final double MAX_LOAD_FACTOR = 0.75;

    private LinkedList<T>[] table;
    private int size; // Number of unique elements in the set

    @SuppressWarnings("unchecked")
    public HashSetSeparateChaining() {
        // Create an array of LinkedLists.
        // We have to use this "unchecked" cast, a common quirk in Java with generic arrays.
        table = (LinkedList<T>[]) new LinkedList[DEFAULT_CAPACITY];
        size = 0;
    }

    /**
     * The hash function. It maps an item to an index in our array.
     * 1. Get the item's built-in hashCode.
     * 2. Use `& 0x7fffffff` to remove the sign bit, ensuring the number is non-negative.
     * 3. Use `% table.length` to map it to a valid index.
     */
    private int hash(T item) {
        return (item.hashCode() & 0x7fffffff) % table.length;
    }

    @Override
    public void add(T item) {
        if (item == null) return; // We don't store nulls

        // 1. Check if we need to resize *before* adding.
        // This keeps the load factor low and performance high.
        if ((double) size / table.length > MAX_LOAD_FACTOR) {
            resize();
        }

        // 2. Find the correct bucket
        int index = hash(item);
        if (table[index] == null) {
            // No list at this bucket yet. Create one.
            table[index] = new LinkedList<>();
        }

        // 3. Add the item to the list (if it's not already there)
        LinkedList<T> bucket = table[index];
        if (!bucket.contains(item)) {
            bucket.add(item);
            size++;
        }
    }

    @Override
    public boolean contains(T item) {
        if (item == null) return false;
        int index = hash(item);
        LinkedList<T> bucket = table[index];

        // If the bucket is null or the list doesn't contain the item, it's not here.
        return bucket != null && bucket.contains(item);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public ISet<T> intersection(ISet<T> other) {
        // For performance, iterate over the smaller set.
        ISet<T> smallerSet = (this.size() < other.size()) ? this : other;
        ISet<T> largerSet = (this.size() < other.size()) ? other : this;

        ISet<T> resultSet = new HashSetSeparateChaining<>();
        
        for (T item : smallerSet) {
            if (largerSet.contains(item)) {
                resultSet.add(item);
            }
        }
        return resultSet;
    }

    @Override
    public Iterator<T> iterator() {
        return new SeparateChainingIterator();
    }

    private class SeparateChainingIterator implements Iterator<T> {
        private int bucketIndex;
        private Iterator<T> listIterator;

        public SeparateChainingIterator() {
            bucketIndex = 0;
            listIterator = null;
            advanceToNextItem();
        }

        @Override
        public boolean hasNext() {
            return listIterator != null && listIterator.hasNext();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T item = listIterator.next();
            if (!listIterator.hasNext()) {
                advanceToNextItem();
            }
            return item;
        }

        private void advanceToNextItem() {
            // Move to the next list in the current bucket if available
            if (listIterator != null && listIterator.hasNext()) {
                return;
            }

            // Find the next non-empty bucket
            while (bucketIndex < table.length) {
                if (table[bucketIndex] != null && !table[bucketIndex].isEmpty()) {
                    listIterator = table[bucketIndex].iterator();
                    bucketIndex++; // Move to next bucket for the *next* call
                    return;
                }
                bucketIndex++;
            }
            
            // No more items
            listIterator = null;
        }
    }

    /**
     * The resize (or "rehash") operation.
     * This is the most complex part, but it's essential for performance.
     * When the table gets too full, we create a new, larger table
     * and re-insert ALL existing items.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        // 1. Store the old table
        LinkedList<T>[] oldTable = table;

        // 2. Create a new table, double the size
        table = (LinkedList<T>[]) new LinkedList[oldTable.length * 2];
        size = 0; // Reset size, as `add` will increment it

        // 3. Re-hash and re-insert all items from the old table
        for (LinkedList<T> bucket : oldTable) {
            if (bucket != null) {
                for (T item : bucket) {
                    // `add` will hash the item to the correct new index
                    add(item);
                }
            }
        }
    }
}