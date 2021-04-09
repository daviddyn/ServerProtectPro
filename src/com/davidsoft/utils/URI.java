package com.davidsoft.utils;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Objects;

public class URI {

    private final Path location;
    private final boolean relative;
    private final String resourceName;

    public URI(Path location, boolean relative, String resourceName) {
        if (relative && resourceName == null && location.isRoot()) {
            throw new IllegalArgumentException();
        }
        this.location = location;
        this.relative = relative;
        this.resourceName = resourceName;
    }

    public Path getLocation() {
        return location;
    }

    public String getResourceName() {
        return resourceName;
    }

    public boolean isLocation() {
        return resourceName == null;
    }

    public URI asLocation() {
        if (resourceName == null) {
            return this;
        }
        return new URI(location.getInto(resourceName), relative, null);
    }

    public boolean isResource() {
        return resourceName != null;
    }

    public URI asResource() {
        if (resourceName != null) {
            return this;
        }
        return new URI(location.getParent(), relative, location.lastPattern());
    }

    public boolean isRelative() {
        return relative;
    }

    public boolean locationIsRoot() {
        return location.isRoot();
    }

    public int patternCount() {
        return resourceName == null ? location.patternCount() : location.patternCount() + 1;
    }

    public String patternAt(int position) {
        if (resourceName != null && position == location.patternCount()) {
            return resourceName;
        }
        return location.patternAt(position);
    }

    public URI subLocation(int startInclude, int endExclude) {
        if (resourceName == null) {
            if (startInclude == 0 && endExclude == location.patternCount()) {
                return this;
            }
        }
        else {
            int locationPatternCount = location.patternCount();
            if (endExclude == locationPatternCount + 1) {
                return new URI(location.subPath(startInclude, locationPatternCount).getInto(resourceName), relative, null);
            }
            if (startInclude == 0 && endExclude == locationPatternCount) {
                return new URI(location, relative, null);
            }
        }
        return new URI(location.subPath(startInclude, endExclude), relative, null);
    }

    public URI subResource(int startInclude, int endExclude) {
        if (resourceName != null && startInclude == 0 && endExclude == location.patternCount() + 1) {
            return this;
        }
        return new URI(location.subPath(startInclude, endExclude - 1), relative, location.patternAt(endExclude - 1));
    }

    public URI getParentLocation() {
        return getParentLocation(1);
    }

    public URI getParentLocation(int count) {
        return subLocation(0, patternCount() - count);
    }

    public URI getParentResource() {
        return getParentResource(1);
    }

    public URI getParentResource(int count) {
        return subResource(0, patternCount() - count);
    }

    public URI getIntoLocation(Path another) {
        return getIntoLocation(another, 0, another.patternCount());
    }

    public URI getIntoLocation(Path another, int startInclude, int endExclude) {
        if (resourceName != null) {
            throw new IllegalStateException("当前URI指向一个资源，因此不可以执行“进入路径”操作。");
        }
        if (startInclude == endExclude) {
            return this;
        }
        return new URI(location.getInto(another, startInclude, endExclude), relative, null);
    }

    public URI getIntoLocation(String location) {
        if (resourceName != null) {
            throw new IllegalStateException("当前URI指向一个资源，因此不可以执行“进入路径”操作。");
        }
        return new URI(this.location.getInto(location), relative, null);
    }

    public URI getIntoLocation(String... patterns) {
        return getIntoLocation(patterns, 0, patterns.length);
    }

    public URI getIntoLocation(String[] patterns, int offset, int length) {
        if (resourceName != null) {
            throw new IllegalStateException("当前URI指向一个资源，因此不可以执行“进入路径”操作。");
        }
        if (length == 0) {
            return this;
        }
        return new URI(location.getInto(patterns, offset, length), relative, null);
    }

    public URI getIntoResource(Path another) {
        return getIntoResource(another, 0, another.patternCount());
    }

    public URI getIntoResource(Path another, int startInclude, int endExclude) {
        if (resourceName != null) {
            throw new IllegalStateException("当前URI指向一个资源，因此不可以执行“进入路径”操作。");
        }
        if (startInclude == endExclude) {
            return this;
        }
        return new URI(location.getInto(another, startInclude, endExclude - 1), relative, location.patternAt(endExclude - 1));
    }

    public URI getIntoResource(String resource) {
        if (resourceName != null) {
            throw new IllegalStateException("当前URI指向一个资源，因此不可以执行“进入路径”操作。");
        }
        return new URI(location, relative, resource);
    }

    public URI getIntoResource(String... patterns) {
        return getIntoResource(patterns, 0, patterns.length);
    }

    public URI getIntoResource(String[] patterns, int offset, int length) {
        if (resourceName != null) {
            throw new IllegalStateException("当前URI指向一个资源，因此不可以执行“进入路径”操作。");
        }
        if (length == 0) {
            return this;
        }
        return new URI(location.getInto(patterns, offset, length - 1), relative, patterns[offset + length - 1]);
    }

    public URI getInto(URI uri) {
        if (resourceName != null) {
            throw new IllegalStateException("当前URI指向一个资源，因此不可以执行“进入路径”操作。");
        }
        if (!uri.isRelative()) {
            throw new IllegalStateException("目标URI不是相对URI，因此不可以从当前URI进入到目标URI。");
        }
        if (uri.location.isRoot()) {
            if (uri.resourceName == null) {
                return this;
            }
            return new URI(location, relative, null);
        }
        return new URI(location.getInto(uri.location), relative, uri.resourceName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof URI)) return false;
        URI uri = (URI) o;
        return relative == uri.relative &&
                location.equals(uri.location) &&
                Objects.equals(resourceName, uri.resourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, relative, resourceName);
    }

    public String toString(String pathSeparator, StringEscapeEncoder encoder) {
        if (relative) {
            if (location.patternCount() == 0) {
                return encoder == null ? resourceName : encoder.encode(resourceName);
            }
            else {
                if (resourceName == null) {
                    return location.toString(pathSeparator, encoder) + pathSeparator;
                }
                else {
                    return location.toString(pathSeparator, encoder) + pathSeparator + (encoder == null ? resourceName : encoder.encode(resourceName));
                }
            }
        }
        else {
            if (location.patternCount() == 0) {
                if (resourceName == null) {
                    return pathSeparator;
                }
                else {
                    return pathSeparator + (encoder == null ? resourceName : encoder.encode(resourceName));
                }
            }
            else {
                if (resourceName == null) {
                    return pathSeparator + location.toString(pathSeparator, encoder) + pathSeparator;
                }
                else {
                    return pathSeparator + location.toString(pathSeparator, encoder) + pathSeparator + (encoder == null ? resourceName : encoder.encode(resourceName));
                }
            }
        }
    }

    public String toLocationString(String pathSeparator, StringEscapeEncoder encoder) {
        if (relative) {
            return location.toString(pathSeparator, encoder) + pathSeparator;
        }
        else {
            if (location.patternCount() == 0) {
                return pathSeparator;
            }
            else {
                return pathSeparator + location.toString(pathSeparator, encoder) + pathSeparator;
            }
        }
    }

    @Override
    public String toString() {
        if (relative) {
            if (location.patternCount() == 0) {
                return "(Relative):" + resourceName;
            }
            else {
                if (resourceName == null) {
                    return "(Relative)>" + location.toString(">", null);
                }
                else {
                    return "(Relative)>" + location.toString(">", null) + ":" + resourceName;
                }
            }
        }
        else {
            if (location.patternCount() == 0) {
                if (resourceName == null) {
                    return "(Root)";
                }
                else {
                    return "(Root):" + resourceName;
                }
            }
            else {
                if (resourceName == null) {
                    return "(Root)>" + location.toString(">", null);
                }
                else {
                    return "(Root)>" + location.toString(">", null) + ":" + resourceName;
                }
            }
        }
    }

    public static URI parse(String source, String pathSeparator, StringEscapeDecoder decoder) throws ParseException {
        if (source.isEmpty()) {
            throw new ParseException("尝试将空串解析为URI", 0);
        }
        if (source.equals(pathSeparator)) {
            return new URI(Path.EMPTY_PATH, false, null);
        }
        int resourceStart = source.lastIndexOf(pathSeparator);
        if (resourceStart == -1) {
            return new URI(Path.EMPTY_PATH, true, decoder == null ? source : decoder.decode(source));
        }
        if (resourceStart == 0) {
            return new URI(Path.EMPTY_PATH, false, decoder == null ? source.substring(pathSeparator.length()) : decoder.decode(source.substring(pathSeparator.length())));
        }
        int locationStart = source.startsWith(pathSeparator) ? pathSeparator.length() : 0;
        if (resourceStart == locationStart) {
            throw new ParseException("不允许空长的pattern", resourceStart);
        }
        if (resourceStart == source.length() - pathSeparator.length()) {
            return new URI(Path.parse(source.substring(locationStart, resourceStart), pathSeparator, decoder), locationStart == 0, null);
        }
        return new URI(Path.parse(source.substring(locationStart, resourceStart), pathSeparator, decoder), locationStart == 0, decoder == null ? source.substring(resourceStart + pathSeparator.length()) : decoder.decode(source.substring(resourceStart + pathSeparator.length())));
    }

    public static URI valueOfLocation(boolean relative, String... patterns) {
        return valueOfLocation(relative, patterns, 0, patterns.length);
    }

    public static URI valueOfLocation(boolean relative, String[] patterns, int offset, int length) {
        if (length == 0) {
            if (relative) {
                throw new IllegalArgumentException();
            }
            return new URI(Path.EMPTY_PATH, false, null);
        }
        return new URI(Path.valueOf(patterns, offset, length), relative, null);
    }

    public static URI valueOfResource(boolean relative, String... patterns) {
        return valueOfResource(relative, patterns, 0, patterns.length);
    }

    public static URI valueOfResource(boolean relative, String[] patterns, int offset, int length) {
        if (length == 0) {
            throw new IllegalArgumentException("patterns的长度至少为1，以便指定资源名称。");
        }
        return new URI(Path.valueOf(patterns, offset, length - 1), relative, patterns[offset + length - 1]);
    }
}
