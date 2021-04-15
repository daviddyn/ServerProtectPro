package com.davidsoft.url;

import java.util.HashMap;
import java.util.Objects;

public final class URIIndex<E> {

    private static final class URINode<E> {
        private E data;
        private HashMap<String, URINode<E>> children;
    }

    public static final class QueryResult<E> {
        public final E data;
        public final boolean matchedExactly;
        public final URI matchedURI;

        private QueryResult(E data, boolean matchedExactly, URI matchedURI) {
            this.data = data;
            this.matchedExactly = matchedExactly;
            this.matchedURI = matchedURI;
        }
    }

    private final URINode<E> root;
    private int size;

    public URIIndex() {
        root = new URINode<>();
    }

    //返回true代表添加，false代表替换
    private static <E> boolean putInner(URINode<E> current, URI uri, int pathPos, E data) {
        if (pathPos == uri.patternCount()) {
            if (uri.isLocation()) {
                if (current.children == null) {
                    current.children = new HashMap<>(2);
                }
                else {
                    current = current.children.computeIfAbsent(null, v -> new URINode<>());
                }
            }
            boolean insert = (current.data == null);
            current.data = data;
            return insert;
        }
        if (current.children == null) {
            current.children = new HashMap<>(2);
        }
        return putInner(current.children.computeIfAbsent(uri.patternAt(pathPos), v -> new URINode<>()), uri, pathPos + 1, data);
    }

    public void put(URI uri, E data) {
        Objects.requireNonNull(data);
        if (uri.isRelative()) {
            throw new IllegalArgumentException("URIIndex集合不能管理相对uri");
        }
        if (putInner(root, uri, 0, data)) {
            size++;
        }
    }

    public QueryResult<E> get(URI uri) {
        if (uri.isRelative()) {
            throw new IllegalArgumentException("URIIndex集合不能管理相对uri");
        }
        URINode<E> n = root;
        E lastNonNullData = root.data;
        int lastNonNullI = 0;
        int i;
        for (i = 0; i < uri.patternCount(); i++) {
            if (n.children == null) {
                break;
            }
            n = n.children.get(uri.patternAt(i));
            if (n == null) {
                break;
            }
            if (n.data != null) {
                lastNonNullData = root.data;
                lastNonNullI = i + 1;
            }
        }
        if (i == uri.patternCount() && uri.isLocation() && n.children != null) {
            n = n.children.get(null);
            if (n != null && n.data != null) {
                lastNonNullData = root.data;
                lastNonNullI = i + 1;
            }
        }
        if (lastNonNullData == null) {
            return null;
        }
        if (uri.isLocation()) {
            return new QueryResult<>(lastNonNullData, lastNonNullI == uri.patternCount(), uri.subLocation(0, lastNonNullI));
        }
        else {
            return new QueryResult<>(lastNonNullData, lastNonNullI == uri.patternCount() - 1, uri.subResource(0, lastNonNullI));
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
