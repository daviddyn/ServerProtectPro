package com.davidsoft.serverprotect.libs;

import com.davidsoft.serverprotect.Utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class IpIndex<E> {

    private final LinkedHashMap<Integer, E> index;

    public IpIndex() {
        index = new LinkedHashMap<>();
    }

    public void put(String ip, E value) {
        index.put(Utils.encodeIpWithRegex(ip), value);
    }

    public E query(String ip) {
        int q = Utils.encodeIp(ip);
        E ret;
        for (int i = 0; i < 16; i++) {
            ret = index.get(q | ((((i & 8) == 0 ? 0 : 255) << 24) | (((i & 4) == 0 ? 0 : 255) << 16) | (((i & 2) == 0 ? 0 : 255) << 8) | ((i & 1) == 0 ? 0 : 255)));
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public boolean queryContains(String ip) {
        int q = Utils.encodeIp(ip);
        for (int i = 0; i < 16; i++) {
            if (index.containsKey(q | ((((i & 8) == 0 ? 0 : 255) << 24) | (((i & 4) == 0 ? 0 : 255) << 16) | (((i & 2) == 0 ? 0 : 255) << 8) | ((i & 1) == 0 ? 0 : 255)))) {
                return true;
            }
        }
        return false;
    }

    public E get(String ip) {
        return index.get(Utils.encodeIpWithRegex(ip));
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

    public Set<Map.Entry<Integer, E>> entrySet() {
        return index.entrySet();
    }

    public boolean remove(String ip) {
        return index.remove(Utils.encodeIpWithRegex(ip)) != null;
    }
}
