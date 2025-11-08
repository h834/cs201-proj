public interface ISet<T> extends Iterable<T>{
    void add(T item);
    boolean contains(T item);
    int size();
    ISet<T> intersection(ISet<T> other);
}