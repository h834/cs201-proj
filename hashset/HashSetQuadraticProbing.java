import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A HashSet implementation that uses Open Addressing with Quadratic Probing.
 * When a collision occurs, we probe in quadratic steps.
 * (index + 1*1, index + 2*2, index + 3*3, ...)
 *
 * This is your "Interesting Experiment #2" (or #3).
 * It's designed to solve the "primary clustering" problem of Linear Probing.
 */
public class HashSetQuadraticProbing<T> implements ISet<T> {

    private static final int DEFAULT_CAPACITY = 17; // Start with a prime
    private static final double MAX_LOAD_FACTOR = 0.5;
    private static final Object DELETED = new Object();

    private Object[] table;
    private int size;

    public HashSetQuadraticProbing() {
        table = new Object[DEFAULT_CAPACITY];
        size = 0;
    }

    private int hash(T item) {
        return (item.hashCode() & 0x7fffffff) % table.length;
    }

    @Override
    public void add(T item) {
        if (item == null) return;

        if ((double) size / table.length > MAX_LOAD_FACTOR) {
            resize();
        }

        int baseHash = hash(item);
        int index = baseHash;

        // Start probing loop
        // Use long for i*i to prevent integer overflow
        for (long i = 0; ; i++) { // Loop indefinitely until we find a spot
            index = (int) ((baseHash + i * i) % table.length);

            if (table[index] == null || table[index] == DELETED) {
                table[index] = item;
                size++;
                return;
            }

            if (table[index].equals(item)) {
                return;
            }
            
            // NOTE: Because we ensure the table size is prime and the load
            // factor is < 0.5, this loop is guaranteed to find an
            // empty/deleted slot and will not loop forever.
        }
    }

    @Override
    public boolean contains(T item) {
        if (item == null) return false;

        int baseHash = hash(item);
        int index = baseHash;

        for (long i = 0; ; i++) { // Loop indefinitely
            index = (int) ((baseHash + i * i) % table.length);

            if (table[index] == null) {
                // Found a true null, so the item cannot be in the set.
                return false;
            }

            if (table[index].equals(item)) {
                return true;
            }
            
            // If table[index] == DELETED, we must keep searching.
            
            // As a safety check (though resizing should prevent this),
            // if we've looped more than the table size, something is wrong.
            // But realistically, we should hit a 'null' slot first.
            if (i > table.length) {
                 return false;
            }
        }
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

        ISet<T> resultSet = new HashSetQuadraticProbing<>();
        
        for (T item : smallerSet) {
            if (largerSet.contains(item)) {
                resultSet.add(item);
            }
        }
        return resultSet;
    }

    @Override
    public Iterator<T> iterator() {
        return new QuadraticProbingIterator();
    }

    private class QuadraticProbingIterator implements Iterator<T> {
        private int currentIndex;
        private T nextItem;

        public QuadraticProbingIterator() {
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

    // --- MODIFIED RESIZE METHOD ---
    @SuppressWarnings("unchecked")
    private void resize() {
        Object[] oldTable = table;
        
        // Find the next prime number roughly double the old size.
        int newCapacity = getNextPrime(oldTable.length * 2);
        
        table = new Object[newCapacity];
        size = 0;

        for (Object oldItem : oldTable) {
            if (oldItem != null && oldItem != DELETED) {
                add((T) oldItem);
            }
        }
    }

    // --- HELPER METHODS FOR PRIME RESIZING ---

    /**
     * Finds the next prime number >= n.
     */
    private int getNextPrime(int n) {
        if (n <= 2) return 2;
        if (n % 2 == 0) n++; // Start with an odd number
        while (!isPrime(n)) {
            n += 2;
        }
        return n;
    }

    /**
     * Simple primality test.
     */
    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        // Check divisors up to sqrt(n)
        for (int i = 5; i * i <= n; i = i + 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }
}