package com.davidsoft.collections;

import java.util.Collection;
import java.util.Iterator;

public class ReadOnlyCollection<E> implements Collection<E> {

    private final Collection<E> source;

    public ReadOnlyCollection(Collection<E> source) {
        this.source = source;
    }

    @Override
    public int size() {
        return source == null ? 0 : source.size();
    }

    @Override
    public boolean isEmpty() {
        return source == null || source.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (source == null) {
            return false;
        }
        return source.contains(o);
    }
    
    @Override
    public Iterator<E> iterator() {
        return new ReadOnlyIterator<>(source == null ? null : source.iterator());
    }
    
    @Override
    public Object[] toArray() {
        if (source == null) {
            return new Object[0];
        }
        return source.toArray();
    }

    @SuppressWarnings({"unchecked", "SuspiciousToArrayCall"})
    @Override
    public <T> T[] toArray(T[] a) {
        if (source == null) {
            return (T[]) new Object[0];
        }
        return source.toArray(a);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (source == null) {
            return false;
        }
        return source.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
