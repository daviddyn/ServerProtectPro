package com.davidsoft.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class DomainIndex<E> {

    private static final class DomainNode<E> {
        private E data;
        private E defaultData;
        private HashMap<String, DomainNode<E>> children;
    }

    public static final class QueryResult<E> {
        public final E data;
        public final boolean matchedExactly;
        public final Domain matchedDomain;

        private QueryResult(E data, boolean matchedExactly, Domain matchedDomain) {
            this.data = data;
            this.matchedExactly = matchedExactly;
            this.matchedDomain = matchedDomain;
        }
    }

    private final DomainNode<E> root;
    private int size;

    public DomainIndex() {
        root = new DomainNode<>();
    }

    //返回true代表添加，false代表替换
    private static <E> boolean putInner(DomainNode<E> current, Domain domain, int pathPos, E data) {
        if (pathPos == domain.patternCount()) {
            boolean insert;
            if (domain.isRelative()) {
                insert = (current.defaultData == null);
                current.defaultData = data;
            }
            else {
                insert = (current.data == null);
                current.data = data;
            }
            return insert;
        }
        if (current.children == null) {
            current.children = new HashMap<>(2);
        }
        return putInner(current.children.computeIfAbsent(domain.patternAt(pathPos), v -> new DomainNode<>()), domain, pathPos + 1, data);
    }

    public void put(Domain domain, E data) {
        Objects.requireNonNull(data);
        if (putInner(root, domain, 0, data)) {
            size++;
        }
    }

    public QueryResult<E> get(Domain domain) {
        if (domain.isRelative()) {
            throw new IllegalArgumentException("你必须使用绝对Domain来检索DomainIndex");
        }
        DomainNode<E> n = root;
        int i;
        for (i = 0; i < domain.patternCount(); i++) {
            if (n.children == null) {
                return null;
            }
            DomainNode<E> child = n.children.get(domain.patternAt(i));
            if (child == null) {
                if (n.defaultData == null) {
                    return null;
                }
                return new QueryResult<>(n.defaultData, false, domain.getParentRelative(domain.patternCount() - i));
            }
            n = child;
        }
        if (n.data == null) {
            return null;
        }
        return new QueryResult<>(n.data, true, domain);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private void forEachKeyInner(DomainNode<E> n, ArrayList<String> path, Consumer<? super Domain> action) {
        if (n.data != null || n.defaultData != null) {
            String[] patterns = new String[path.size()];
            path.toArray(patterns);
            if (n.data != null) {
                action.accept(Domain.absoluteValueOf(patterns));
            }
            if (n.defaultData != null) {
                action.accept(Domain.relativeValueOf(patterns));
            }
        }
        if (n.children != null) {
            for (Map.Entry<String, DomainNode<E>> entry : n.children.entrySet()) {
                path.add(entry.getKey());
                forEachKeyInner(entry.getValue(), path, action);
                path.remove(path.size() - 1);
            }
        }
    }

    public void forEachKey(Consumer<? super Domain> action) {
        forEachKeyInner(root, new ArrayList<>(), action);
    }
}
