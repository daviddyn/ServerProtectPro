package com.davidsoft.utils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * 层级关系中一条层级路径的抽象表示。
 */
public class Path {

    public static final Path EMPTY_PATH = new Path(new String[0]);

    private final String[] patterns;

    private Path(String[] patterns) {
        this.patterns = patterns;
    }

    public boolean isRoot() {
        return patterns.length == 0;
    }

    public int patternCount() {
        return patterns.length;
    }

    public String patternAt(int position) {
        return patterns[position];
    }

    public String lastPattern() {
        return patterns[patterns.length - 1];
    }

    public Path subPath(int startInclude, int endExclude) {
        if (startInclude == 0 && endExclude == patterns.length) {
            return this;
        }
        if (startInclude == endExclude) {
            return EMPTY_PATH;
        }
        return new Path(Arrays.copyOfRange(patterns, startInclude, endExclude));
    }

    public Path getParent() {
        return getParent(1);
    }

    public Path getParent(int count) {
        if (count == 0) {
            return this;
        }
        if (patterns.length == 0) {
            throw new IllegalStateException("此Path已是顶层");
        }
        return subPath(0, patterns.length - count);
    }

    public Path getInto(Path another) {
        return getInto(another.patterns, 0, another.patterns.length);
    }

    public Path getInto(Path another, int startInclude, int endExclude) {
        return getInto(another.patterns, startInclude, endExclude - startInclude);
    }

    public Path getInto(String pattern) {
        String[] newPatterns = Arrays.copyOf(patterns, patterns.length + 1);
        newPatterns[patterns.length] = pattern;
        return new Path(newPatterns);
    }

    public Path getInto(String... patterns) {
        return getInto(patterns, 0, patterns.length);
    }

    public Path getInto(String[] patterns, int offset, int length) {
        if (length == 0) {
            return this;
        }
        String[] newPatterns = Arrays.copyOf(this.patterns, patterns.length + length);
        System.arraycopy(patterns, offset, newPatterns, patterns.length, length);
        return new Path(newPatterns);
    }

    public Path getIntoReverse(String[] patterns) {
        return getIntoReverse(patterns, 0, patterns.length);
    }

    public Path getIntoReverse(String[] patterns, int offset, int length) {
        if (length == 0) {
            return this;
        }
        String[] newPatterns = Arrays.copyOf(this.patterns, patterns.length + length);
        for (int i = 0; i < length; i++) {
            newPatterns[newPatterns.length - i - 1] = patterns[i + offset];
        }
        return new Path(newPatterns);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path httpPath = (Path) o;
        return Arrays.equals(patterns, httpPath.patterns);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(patterns);
    }

    public String toString(String pathSeparator, StringEscapeEncoder encoder) {
        if (patterns.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String pattern : patterns) {
            if (encoder == null) {
                builder.append(pattern).append(pathSeparator);
            }
            else {
                builder.append(encoder.encode(pattern)).append(pathSeparator);
            }
        }
        builder.delete(builder.length() - pathSeparator.length(), builder.length());
        return builder.toString();
    }

    @Override
    public String toString() {
        return toString(">", null);
    }

    public static Path parse(String source, String pathSeparator, StringEscapeDecoder decoder) throws ParseException {
        if (source.isEmpty()) {
            return EMPTY_PATH;
        }
        Builder builder = new Builder();
        int findStart = 0;
        int findEnd;
        do {
            findEnd = source.indexOf(pathSeparator, findStart);
            if (findEnd == -1) {
                findEnd = source.length();
            }
            else {
                if (findEnd == findStart) {
                    throw new ParseException("不允许空长的pattern", findStart);
                }
            }
            String pattern = source.substring(findStart, findEnd);
            builder.goInto(decoder == null ? pattern : decoder.decode(pattern));
            findStart = findEnd + pathSeparator.length();
        } while (findEnd < source.length());
        return builder.build();
    }

    public static Path parseReverse(String source, String pathSeparator, StringEscapeDecoder decoder) throws ParseException {
        Path p = parse(source, pathSeparator, decoder);
        int i, j;
        for (i = 0, j = p.patterns.length - 1; i < j; i++, j--) {
            String t = p.patterns[i];
            p.patterns[i] = p.patterns[j];
            p.patterns[j] = t;
        }
        return p;
    }

    public static Path valueOf(String... patterns) {
        return valueOf(patterns, 0, patterns.length);
    }

    public static Path valueOf(String[] patterns, int offset, int length) {
        if (length == 0) {
            return EMPTY_PATH;
        }
        return new Path(Arrays.copyOfRange(patterns, offset, patterns.length - offset));
    }

    public static Path reverseValueOf(String... patterns) {
        return reverseValueOf(patterns, 0, patterns.length);
    }

    public static Path reverseValueOf(String[] patterns, int offset, int length) {
        if (length == 0) {
            return EMPTY_PATH;
        }
        String[] newPatterns = new String[length];
        for (int i = 0; i < patterns.length; i++) {
            newPatterns[i] = patterns[offset + length - i - 1];
        }
        return new Path(newPatterns);
    }


    public static class Builder {

        private final ArrayList<String> patterns;

        private Builder(int initialCapacity) {
            patterns = new ArrayList<>(initialCapacity);
        }

        public Builder() {
            patterns = new ArrayList<>();
        }

        public Builder(Path path) {
            this(path.patterns, 0, path.patterns.length);
        }

        public Builder(Path path, int startInclude, int endExclude) {
            this(path.patterns, startInclude, endExclude - startInclude);
        }

        public Builder(String... patterns) {
            this(patterns, 0, patterns.length);
        }

        public Builder(String[] patterns, int offset, int length) {
            this.patterns = new ArrayList<>(length);
            goInto(patterns, offset, length);
        }

        public boolean isRoot() {
            return patterns.isEmpty();
        }

        public int patternCount() {
            return patterns.size();
        }

        public String patternAt(int position) {
            return patterns.get(position);
        }

        public String lastPattern() {
            return patterns.get(patterns.size() - 1);
        }

        public Builder makeSubPath(int startInclude, int endExclude) {
            if (startInclude == 0 && endExclude == patterns.size()) {
                return this;
            }
            int i;
            if (startInclude == 0) {
                i = endExclude;
            }
            else {
                for (i = 0; i < endExclude - startInclude; i++) {
                    patterns.set(i, patterns.get(i + startInclude));
                }
            }
            for (int j = patterns.size() - 1; j >= i; j--) {
                patterns.remove(j);
            }
            return this;
        }

        public Builder makeParent() {
            return makeParent(1);
        }

        public Builder makeParent(int count) {
            if (count == 0) {
                return this;
            }
            if (patterns.isEmpty()) {
                throw new IllegalStateException("此Path已是顶层");
            }
            return makeSubPath(0, patterns.size() - count);
        }

        public Builder goInto(Path another) {
            return goInto(another.patterns, 0, another.patterns.length);
        }

        public Builder goInto(Path another, int startInclude, int endExclude) {
            return goInto(another.patterns, startInclude, endExclude - startInclude);
        }

        public Builder goInto(String pattern) {
            patterns.add(pattern);
            return this;
        }

        public Builder goInto(String... patterns) {
            return goInto(patterns, 0, patterns.length);
        }

        public Builder goInto(String[] patterns, int offset, int length) {
            if (length == 0) {
                return this;
            }
            this.patterns.ensureCapacity(this.patterns.size() + length);
            for (int i = 0; i < length; i++) {
                this.patterns.add(patterns[offset + i]);
            }
            return this;
        }

        public Builder goIntoReverse(String... patterns) {
            return goIntoReverse(patterns, 0, patterns.length);
        }

        public Builder goIntoReverse(String[] patterns, int offset, int length) {
            if (length == 0) {
                return this;
            }
            for (int i = 0; i < length; i++) {
                this.patterns.add(patterns[offset + length - i - 1]);
            }
            return this;
        }

        public Builder goInto(Builder another) {
            return goInto(another, 0, another.patterns.size());
        }

        public Builder goInto(Builder another, int startInclude, int endExclude) {
            patterns.addAll(another.patterns.subList(startInclude, endExclude));
            return this;
        }

        public Path build() {
            if (patterns.isEmpty()) {
                return EMPTY_PATH;
            }
            String[] newPatterns = new String[patterns.size()];
            patterns.toArray(newPatterns);
            return new Path(newPatterns);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (o instanceof Builder) {
                return patterns.equals(((Builder) o).patterns);
            }
            if (o instanceof Path) {
                Path p = (Path) o;
                if (p.patterns.length != patterns.size()) {
                    return false;
                }
                for (int i = 0; i < p.patterns.length; i++) {
                    if (!p.patterns[i].equals(patterns.get(i))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return patterns.hashCode();
        }

        public String toString(String pathSeparator, StringEscapeEncoder encoder) {
            if (patterns.isEmpty()) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (String pattern : patterns) {
                if (encoder == null) {
                    builder.append(pattern).append(pathSeparator);
                }
                else {
                    builder.append(encoder.encode(pattern)).append(pathSeparator);
                }
            }
            builder.delete(builder.length() - pathSeparator.length(), builder.length());
            return builder.toString();
        }

        @Override
        public String toString() {
            return toString(">", null);
        }
    }
}
