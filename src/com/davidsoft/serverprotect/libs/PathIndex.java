package com.davidsoft.serverprotect.libs;

import java.util.HashMap;
import java.util.Objects;

public final class PathIndex<E> {

    private static final class PathNode<E> {
        private E data;
        private HashMap<String, PathNode<E>> children;
    }

    public static final class QueryResult<E> {
        public final E data;
        public final boolean matchedExactly;
        public final HttpPath matchedPath;

        private QueryResult(E data, boolean matchedExactly, HttpPath matchedPath) {
            this.data = data;
            this.matchedExactly = matchedExactly;
            this.matchedPath = matchedPath;
        }
    }

    private final PathNode<E> root;
    private int size;

    public PathIndex() {
        root = new PathNode<>();
    }

    //返回true代表添加，false代表替换
    private static <E> boolean putInner(PathNode<E> current, HttpPath path, int pathPos, E data) {
        if (pathPos == path.patternCount()) {
            boolean insert = current == null;
            current.data = data;
            return insert;
        }
        PathNode<E> child = null;
        if (current.children == null) {
            current.children = new HashMap<>(2);
        }
        else {
            child = current.children.get(path.patternAt(pathPos));
        }
        if (child == null) {
            child = new PathNode<>();
            current.children.put(path.patternAt(pathPos), child);
        }
        return putInner(child, path, pathPos + 1, data);
    }

    public void put(HttpPath path, E data) {
        Objects.requireNonNull(data);
        if (path.isRoot()) {
            if (root.data == null) {
                size++;
            }
            root.data = data;
        }
        else {
            if (putInner(root, path, 0, data)) {
                size++;
            }
        }
    }

    /*
    private static <E> QueryResult<E> getInner(PathNode<E> current, HttpPath path, int pathPos) {
        if (pathPos == path.patternCount()) {
            if (current.data == null) {
                return null;
            }
            return new QueryResult<>(current.data, true, path);
        }
        if (current.children != null) {
            PathNode<E> child = current.children.get(path.patternAt(pathPos));
            if (child != null) {
                QueryResult<E> result = getInner(child, path, pathPos + 1);
                if (result != null) {
                    return result;
                }
            }
        }
        if (current.data == null) {
            return null;
        }
        return new QueryResult<>(current.data, false, path.subPath(0, pathPos));
    }
    */

    public QueryResult<E> get(HttpPath path) {
        if (path.isRoot()) {
            if (root.data == null) {
                return null;
            }
            return new QueryResult<>(root.data, true, path);
        }
        else {
            //从头找
            PathNode<E> n = root;
            E lastNonNullData = root.data;
            int lastNonNullI = 0;
            for (int i = 0; i < path.patternCount(); i++) {
                if (n.children == null) {
                    break;
                }
                n = n.children.get(path.patternAt(i));
                if (n == null) {
                    break;
                }
                if (n.data != null) {
                    lastNonNullData = root.data;
                    lastNonNullI = i + 1;
                }
            }
            if (lastNonNullData == null) {
                return null;
            }
            return new QueryResult<>(lastNonNullData, lastNonNullI == path.patternCount() - 1, path.subPath(0, lastNonNullI));
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
