package com.davidsoft.url;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 表示具有层级意义的序列。
 */
public final class Path {

    /**
     * 自己实现它的原因不仅在于不需要转义，还在于保留首尾空串。
     *
     * @return 当src为null时，返回null；当src为空串时，返回0长度数组。
     */
    private static String[] nonRegexSplit(String src, String split) {
        if (src == null) {
            return null;
        }
        if (src.length() == 0) {
            return new String[0];
        }
        int findPos = 0;
        ArrayList<String> builder = new ArrayList<>();
        while (true) {
            int endPos = src.indexOf(split, findPos);
            if (endPos == -1) {
                builder.add(src.substring(findPos));
                break;
            }
            else {
                builder.add(src.substring(findPos, endPos));
                findPos = endPos + split.length();
            }
        }
        String[] ret = new String[builder.size()];
        builder.toArray(ret);
        return ret;
    }

    /**
     * 解析路径
     *
     * @param path 首尾均可出现{@code separator}。
     * @param separator 分隔符
     */
    public static Path parse(String path, String separator) {
        if (path.length() == 0) {
            return new Path();
        }
        int start = 0, end = path.length();
        if (path.startsWith(separator)) {
            start = separator.length();
        }
        if (path.length() > separator.length() && path.endsWith(separator)) {
            end = path.length() - separator.length();
        }
        String[] split = nonRegexSplit(path.substring(start, end), separator);
        for (String s : split) {
            if (s.length() == 0) {
                throw new IllegalArgumentException("不允许空长的pattern");
            }
        }
        return new Path(split, true);
    }

    private final String[] patterns;

    private Path(String[] patterns, @SuppressWarnings("unused") boolean directReferer) {
        this.patterns = patterns;
    }

    public Path(String... patterns) {
        this(patterns, -1);
    }

    public Path(String[] patterns, int length) {
        this(patterns, 0, length);
    }

    public Path(String[] patterns, int offset, int length) {
        if (patterns == null || length == 0) {
            this.patterns = null;
        }
        else {
            this.patterns = Arrays.copyOfRange(patterns, offset, length == -1 ? patterns.length : offset + length);
        }
    }

    /**
     * 判断是否存在上层路径（判断当前路径是否为顶层路径）
     */
    public boolean hasParent() {
        return patterns != null;
    }

    public boolean isRoot() {
        return patterns == null;
    }

    /**
     * 获得上层路径
     */
    public Path getParent() {
        if (patterns == null) {
            throw new IllegalStateException("此Path已是顶层");
        }
        return subPath(0, patterns.length - 1);
    }

    /**
     * 指定下标范围获得子路径（冷门功能）
     */
    public Path subPath(int startInclude, int endExclude) {
        if (startInclude == 0 && endExclude == patternCount()) {
            return this;
        }
        if (startInclude < endExclude) {
            return new Path(patterns, startInclude, endExclude);
        }
        return new Path();
    }

    /**
     * 追加<strong>一层</strong>路径
     *
     * @param path 路径名称
     */
    public Path getInto(String path) {
        if (path.length() == 0) {
            throw new IllegalArgumentException("不允许空长的pattern");
        }
        String[] newPatterns;
        if (patterns == null) {
            newPatterns = new String[] {path};
        }
        else {
            newPatterns = Arrays.copyOf(patterns, patterns.length + 1);
            newPatterns[patterns.length] = path;
        }
        return new Path(newPatterns, true);
    }

    /**
     * 通过Path追加里层路径，可以直接进入多层。
     */
    public Path getInto(Path another) {
        if (patterns == null) {
            return another;
        }
        String[] appending = another.patterns;
        if (appending == null) {
            return this;
        }
        String[] newPatterns = Arrays.copyOf(patterns, patterns.length + appending.length);
        System.arraycopy(appending, 0, newPatterns, patterns.length, appending.length);
        return new Path(newPatterns, true);
    }

    /**
     * 功能上等同于调用{@code getParent()}后调用{@code getInto(path)}，但此方法更节省开销
     */
    public Path getAnotherPath(String... path) {
        if (!hasParent()) {
            throw new IllegalStateException("此Path已是顶层");
        }
        String[] newPatterns = new String[patterns.length + path.length - 1];
        System.arraycopy(patterns, 0, newPatterns, 0, patterns.length - 1);
        System.arraycopy(path, 0, newPatterns, patterns.length - 1, path.length);
        return new Path(newPatterns, true);
    }

    /**
     * 功能上等同于调用{@code getParent()}后调用{@code getInto(another)}，但此方法更节省开销
     */
    public Path getAnotherPath(Path another) {
        if (!hasParent()) {
            throw new IllegalStateException("此Path已是顶层");
        }
        String[] appending = another.patterns;
        if (appending == null) {
            return getParent();
        }
        String[] newPatterns = new String[patterns.length + appending.length - 1];
        System.arraycopy(patterns, 0, newPatterns, 0, patterns.length - 1);
        System.arraycopy(appending, 0, newPatterns, patterns.length - 1, appending.length);
        return new Path(newPatterns, true);
    }

    /**
     * 获得路径中的某一pattern
     */
    public String patternAt(int position) {
        if (patterns == null) {
            throw new ArrayIndexOutOfBoundsException(position);
        }
        return patterns[position];
    }

    public String lastPattern() {
        if (patterns == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return patterns[patterns.length - 1];
    }

    /**
     * 获得路径中有几个pattern
     */
    public int patternCount() {
        if (patterns == null) {
            return 0;
        }
        return patterns.length;
    }

    public boolean startsWith(String prefix) {
        if (patterns == null) {
            return false;
        }
        return patterns[0].equals(prefix);
    }

    public boolean startsWith(Path prefix) {
        String[] another = prefix.patterns;
        if (patterns == null) {
            return another == null;
        }
        else {
            if (another == null) {
                return false;
            }
            if (patterns == another) {
                return true;
            }
            if (patterns.length < another.length) {
                return false;
            }
            return Arrays.equals(patterns, 0, another.length, another, 0, another.length);
        }
    }

    public boolean endsWith(String suffix) {
        if (patterns == null) {
            return false;
        }
        return patterns[patterns.length - 1].equals(suffix);
    }

    public boolean endsWith(Path suffix) {
        String[] another = suffix.patterns;
        if (patterns == null) {
            return another == null;
        }
        else {
            if (another == null) {
                return false;
            }
            if (patterns == another) {
                return true;
            }
            if (patterns.length < another.length) {
                return false;
            }
            return Arrays.equals(patterns, patterns.length - another.length, patterns.length, another, 0, another.length);
        }
    }

    public String toString(String separator) {
        if (patterns == null) {
            return "";
        }
        return String.join(separator, patterns);
    }

    public String toReversedString(String separator) {
        if (patterns == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int i = patterns.length - 1;
        while (true) {
            builder.append(patterns[i]);
            if (i == 0) {
                break;
            }
            i--;
            builder.append(separator);
        }
        return String.valueOf(builder);
    }

    public String[] getPatterns() {
        if (patterns == null) {
            return new String[0];
        }
        return Arrays.copyOf(patterns, patterns.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path path = (Path) o;
        return Arrays.equals(patterns, path.patterns);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(patterns);
    }

    @Override
    public String toString() {
        return toString("/");
    }
}
