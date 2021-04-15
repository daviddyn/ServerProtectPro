package com.davidsoft.net;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RegexIpIndex<E> {

    private final LinkedHashMap<Long, E> index;

    public RegexIpIndex() {
        index = new LinkedHashMap<>();
    }

    public void put(long regexIp, E value) {
        index.put(regexIp, value);
    }

    public E get(int ip) {
        for (int i = 0; i < 16; i++) {
            E e = getExactly((((long)i) << 32) | ip);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    public boolean contains(int ip) {
        for (int i = 0; i < 16; i++) {
            if (containsExactly((((long)i) << 32) | ip)) {
                return true;
            }
        }
        return false;
    }

    public E getExactly(long regexIp) {
        return index.get(regexIp);
    }

    public boolean containsExactly(long regexIp) {
        return index.containsKey(regexIp);
    }

    public void clear() {
        index.clear();
    }

    public boolean isEmpty() {
        return index.isEmpty();
    }

    public int size() {
        return index.size();
    }

    public Set<Map.Entry<Long, E>> entrySet() {
        return index.entrySet();
    }

    public boolean removeExactly(long regexIp) {
        return index.remove(regexIp) != null;
    }
}
