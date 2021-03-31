package com.davidsoft.collections;

import java.util.Iterator;

public class ReadOnlyIterator<E> implements Iterator<E> {

    private final Iterator<E> source;

    public ReadOnlyIterator(Iterator<E> source) {
        this.source = source;
    }

    @Override
    public boolean hasNext() {
        return source != null && source.hasNext();
    }

    @Override
    public E next() {
        if (source == null) {
            return null;
        }
        return source.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
