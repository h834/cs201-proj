import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A HashSet implementation that uses Open Addressing with Linear Probing.
 * When a collision occurs, we probe for the *next* available slot in the array.
 * (index + 1, index + 2, index + 3, ...)
 *
 * This is your "Interesting Experiment #1".
 */
public class HashSetLinearProbing<T> implements ISet<T> {

    private static final int DEFAULT_CAPACITY = 16;
    // For Open Addressing, we need a lower load factor to leave empty spaces.
    // If this is too high (e.g., 0.8), performance grinds to a halt.
    private static final double MAX_LOAD_FACTOR = 0.5;

    // A special "sentinel" object to mark slots where an item was deleted.
    // This is crucial so we don't "break the chain" when searching.
    private static final Object DELETED = new Object();

    private Object[] table; // Use Object[] to store T or DELETED
    private int size; // Number of unique elements

    public HashSetLinearProbing() {
        table = new Object[DEFAULT_CAPACITY];
        size = 0;
    }

    /**
     * Hash function. Same as before.
     */
    private int hash(T item) {
        return (item.hashCode() & 0x7fffffff) % table.length;
    }

    @Override
    public void add(T item) {
        if (item == null) return;

        // 1. Resize if table is too full
        if ((double) size / table.length > MAX_LOAD_FACTOR) {
            resize();
        }

        int baseHash = hash(item);
        int index = baseHash;

        // 2. Start probing loop
        for (int i = 0; i < table.length; i++) {
            index = (baseHash + i) % table.length; // Linear probe: i = 0, 1, 2, 3...

            if (table[index] == null || table[index] == DELETED) {
                // Found an empty slot! Place item here.
                table[index] = item;
                size++;
                return;
            }

            if (table[index].equals(item)) {
                // Item is already in the set.
                return;
            }
        }
        // If we get here, the table is full (this shouldn't happen if resize is correct)
    }

    @Override
    public boolean contains(T item) {
        if (item == null) return false;

        int baseHash = hash(item);
        int index = baseHash;

        // 1. Start probing loop
        for (int i = 0; i < table.length; i++) {
            index = (baseHash + i) % table.length; // Linear probe

            if (table[index] == null) {
                // Found a truly empty slot, so the item can't be here.
                return false;
            }

            if (table[index].equals(item)) {
                // Found it!
                return true;
            }

            // If table[index] == DELETED, we must keep searching!
        }

        // 2. We've probed the entire table and didn't find it.
        return false;
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

        ISet<T> resultSet = new HashSetLinearProbing<>();
        
        for (T item : smallerSet) {
            if (largerSet.contains(item)) {
                resultSet.add(item);
            }
        }
        return resultSet;
    }

    @Override
    public Iterator<T> iterator() {
        return new LinearProbingIterator();
    }

    private class LinearProbingIterator implements Iterator<T> {
        private int currentIndex;
        private T nextItem;

        public LinearProbingIterator() {
            currentIndex = 0;
            nextItem = null;
            advanceToNextItem();
        }

        @Override
        public boolean hasNext() {
            return nextItem != null;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T currentItem = nextItem;
            advanceToNextItem();
            return currentItem;
        }

        @SuppressWarnings("unchecked")
        private void advanceToNextItem() {
            nextItem = null;
            while (currentIndex < table.length) {
                Object item = table[currentIndex++];
                if (item != null && item != DELETED) {
                    nextItem = (T) item;
                    return;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Object[] oldTable = table;
        table = new Object[oldTable.length * 2];
        size = 0;

        for (Object oldItem : oldTable) {
            if (oldItem != null && oldItem != DELETED) {
                // Re-hash and add the item to the new table
                add((T) oldItem);
            }
        }
    }

    // Note: A 'remove' method for Open Addressing is tricky.
    // You can't just set table[index] = null.
    // You MUST set table[index] = DELETED.
    // This is a great point for your "Analysis" section.
}