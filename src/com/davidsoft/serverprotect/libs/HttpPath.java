package com.davidsoft.serverprotect.libs;

import com.davidsoft.http.UrlCodec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public final class HttpPath {

    private final String[] patterns;

    public HttpPath() {
        this(new String[] {""});
    }

    private HttpPath(String[] patterns) {
        this.patterns = patterns;
    }

    public String[] getPatterns() {
        return patterns;
    }

    public HttpPath subPath(int startInclude, int endExclude) {
        if (startInclude == 0 && endExclude == patterns.length) {
            return this;
        }
        if (startInclude == endExclude) {
            return new HttpPath();
        }
        return new HttpPath(Arrays.copyOfRange(patterns, startInclude, endExclude));
    }

    public HttpPath subPath(int startInclude) {
        return subPath(startInclude, patterns.length);
    }

    public HttpPath getParent() {
        if (patterns.length == 1) {
            if (patterns[0].isEmpty()) {
                throw new IllegalStateException("此Path已是顶层");
            }
            return new HttpPath(new String[] {""});
        }
        return subPath(0, patterns.length - 1);
    }

    public HttpPath getInto(HttpPath another) {
        String[] newPatterns;
        if (patterns[patterns.length - 1].isEmpty()) {
            newPatterns = Arrays.copyOf(patterns, patterns.length + another.patterns.length - 1);
            System.arraycopy(another.patterns, 0, newPatterns, patterns.length - 1, another.patterns.length);
        }
        else {
            newPatterns = Arrays.copyOf(patterns, patterns.length + another.patterns.length);
            System.arraycopy(another.patterns, 0, newPatterns, patterns.length, another.patterns.length);
        }
        return new HttpPath(newPatterns);
    }

    public HttpPath getRelative(HttpPath another) {
        String[] newPatterns = Arrays.copyOf(patterns, patterns.length + another.patterns.length - 1);
        System.arraycopy(another.patterns, 0, newPatterns, patterns.length - 1, another.patterns.length);
        return new HttpPath(newPatterns);
    }

    public String lastPattern() {
        return patterns[patterns.length - 1];
    }

    public boolean isRoot() {
        return patterns.length == 1 && patterns[0].isEmpty();
    }

    public int patternCount() {
        return patterns.length;
    }

    public String patternAt(int position) {
        return patterns[position];
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String pattern : patterns) {
            builder.append("/").append(UrlCodec.urlEncode(pattern.getBytes(StandardCharsets.UTF_8)));
        }
        return builder.toString();
    }

    public static HttpPath parse(String path) {
        int findStart = path.startsWith("/") ? 1 : 0;
        int findEnd;
        ArrayList<String> buildingPatterns = new ArrayList<>();
        do {
            findEnd = path.indexOf('/', findStart);
            if (findEnd == -1) {
                findEnd = path.length();
            }
            else {
                if (findEnd == findStart) {
                    throw new IllegalArgumentException("不允许空长的pattern");
                }
            }
            buildingPatterns.add(UrlCodec.urlDecodeString(path.substring(findStart, findEnd), StandardCharsets.UTF_8));
            findStart = findEnd + 1;
        } while (findEnd < path.length());
        String[] patterns = new String[buildingPatterns.size()];
        buildingPatterns.toArray(patterns);
        return new HttpPath(patterns);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpPath httpPath = (HttpPath) o;
        return Arrays.equals(patterns, httpPath.patterns);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(patterns);
    }
}
