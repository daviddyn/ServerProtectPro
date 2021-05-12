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

    public void put(URI uri, E data) {
        if (uri.isRelative()) {
            throw new IllegalArgumentException("URIIndex集合不能管理相对uri");
        }
        URINode<E> n = root;
        for (int i = 0; i < uri.patternCount(); i++) {
            if (n.children == null) {
                n.children = new HashMap<>(2);
            }
            n = n.children.computeIfAbsent(uri.patternAt(i), (t) -> new URINode<>());
        }
        if (uri.isLocation()) {
            if (n.children == null) {
                n.children = new HashMap<>(2);
            }
            n = n.children.computeIfAbsent(null, (t) -> new URINode<>());
        }
        if (n.data == null) {
            size++;
        }
        n.data = data;
    }

    public QueryResult<E> get(URI uri) {
        if (uri.isRelative()) {
            throw new IllegalArgumentException("URIIndex集合不能管理相对uri");
        }
        if (root.children == null || root.children.isEmpty()) {
            return null;
        }
        URINode<E> n = root;
        URINode<E> lastMatchedNode = root.children.containsKey(null) ? root : null;
        int lastNonNullI = 0;
        int i;
        for (i = 0; i < uri.patternCount(); i++) {
            if (n.children == null) {
                break;
            }
            URINode<E> child = n.children.get(uri.patternAt(i));
            if (child == null) {
                break;
            }
            n = child;
            if (n.children != null && n.children.containsKey(null)) {
                if (i == uri.patternCount() - 1) {
                    if (n.data!= null) {
                        lastMatchedNode = n;
                        lastNonNullI = i + 1;
                    }
                }
                else {
                    lastMatchedNode = n;
                    lastNonNullI = i + 1;
                }
            }
        }
        if (i == uri.patternCount()) {
            if (uri.isLocation()) {
                if (n.children != null) {
                    n = n.children.get(null);
                    if (n != null) {
                        return new QueryResult<>(n.data, true, uri);
                    }
                }
            }
            else {
                if (n.data != null) {
                    return new QueryResult<>(n.data, true, uri);
                }
            }
        }
        if (lastMatchedNode == null) {
            return null;
        }
        else {
            return new QueryResult<>(lastMatchedNode.children.get(null).data, false, uri.subLocation(0, lastNonNullI));
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
