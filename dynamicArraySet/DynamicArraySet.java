import java.util.Arrays;

public class DynamicArraySet<T> {
    private Object[] data;
    private int size;

    public DynamicArraySet() {
        data = new Object[10];
        size = 0;
    }

    private void resize() {
        data = Arrays.copyOf(data, data.length * 2);
    }

    public void add(T value) {
        if (contains(value)) return; // avoid duplicates

        if (size == data.length) resize();
        data[size++] = value;
    }

    public boolean contains(T value) {
        for (int i = 0; i < size; i++) {
            if (data[i].equals(value)) return true;
        }
        return false;
    }

    public int size() { return size; }

    public DynamicArraySet<T> intersect(DynamicArraySet<T> other) {
        DynamicArraySet<T> result = new DynamicArraySet<>();
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked")
            T val = (T) data[i];
            if (other.contains(val)) {
                result.add(val);
            }
        }
        return result;
    }
}
