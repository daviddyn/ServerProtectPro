package com.davidsoft.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ReadOnlyMap<K, V> implements Map<K, V> {

    private final Map<K, V> source;

    public ReadOnlyMap(Map<K, V> source) {
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
    public boolean containsKey(Object key) {
        if (source == null) {
            return false;
        }
        return source.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (source == null) {
            return false;
        }
        return source.containsValue(value);
    }
    
    @Override
    public V get(Object key) {
        if (source == null) {
            return null;
        }
        return source.get(key);
    }
    
    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        return new ReadOnlySet<>(source == null ? null : source.keySet());
    }

    @Override
    public Collection<V> values() {
        return new ReadOnlyCollection<>(source == null ? null : source.values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new ReadOnlySet<>(source == null ? null : source.entrySet());
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }
}
