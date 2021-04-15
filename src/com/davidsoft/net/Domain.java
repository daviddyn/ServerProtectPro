package com.davidsoft.net;

import com.davidsoft.url.Path;

import java.text.ParseException;
import java.util.Objects;

public class Domain {

    private final Path path;
    private final boolean relative;

    private Domain(Path path, boolean relative) {
        if (path.patternCount() == 0) {
            throw new IllegalArgumentException("Domain须包含至少一个Pattern");
        }
        this.path = path;
        this.relative = relative;
    }

    public boolean isRelative() {
        return relative;
    }

    public Domain asRelative() {
        if (relative) {
            return this;
        }
        return new Domain(path, true);
    }

    public boolean isAbsolute() {
        return !relative;
    }

    public Domain asAbsolute() {
        if (relative) {
            return new Domain(path, false);
        }
        return this;
    }

    public int patternCount() {
        return path.patternCount();
    }

    public String patternAt(int position) {
        return path.patternAt(position);
    }

    public String lastPattern() {
        return path.lastPattern();
    }

    public Domain subRelativeDomain(int startInclude, int endExclude) {
        if (relative) {
            if (startInclude == 0 && endExclude == path.patternCount()) {
                return this;
            }
            else {
                return new Domain(path.subPath(startInclude, endExclude), true);
            }
        }
        else {
            return new Domain(path.subPath(startInclude, endExclude), true);
        }
    }

    public Domain subAbsoluteDomain(int startInclude, int endExclude) {
        if (relative) {
            return new Domain(path.subPath(startInclude, endExclude), false);
        }
        else {
            if (startInclude == 0 && endExclude == path.patternCount()) {
                return this;
            }
            else {
                return new Domain(path.subPath(startInclude, endExclude), false);
            }
        }
    }

    public Domain getParentRelative() {
        return getParentRelative(1);
    }

    public Domain getParentRelative(int count) {
        if (relative && count == 0) {
            return this;
        }
        return new Domain(path.getParent(count), true);
    }

    public Domain getParentAbsolute() {
        return getParentAbsolute(1);
    }

    public Domain getParentAbsolute(int count) {
        if (!relative && count == 0) {
            return this;
        }
        return new Domain(path.getParent(count), false);
    }

    public Domain getIntoRelative(Domain another) {
        return getIntoRelative(another, 0, another.path.patternCount());
    }

    public Domain getIntoRelative(Domain another, int startInclude, int endExclude) {
        if (relative && startInclude == endExclude) {
            return this;
        }
        return new Domain(path.getInto(another.path), true);
    }

    public Domain getIntoRelative(String pattern) {
        return new Domain(path.getInto(pattern), true);
    }

    public Domain getIntoRelative(String... patterns) {
        return getIntoRelative(patterns, 0, patterns.length);
    }

    public Domain getIntoRelative(String[] patterns, int offset, int length) {
        if (relative && length == 0) {
            return this;
        }
        return new Domain(path.getInto(patterns, offset, length), true);
    }

    public Domain getIntoAbsolute(Domain another) {
        return getIntoAbsolute(another, 0, another.path.patternCount());
    }

    public Domain getIntoAbsolute(Domain another, int startInclude, int endExclude) {
        if (!relative && startInclude == endExclude) {
            return this;
        }
        return new Domain(path.getInto(another.path), false);
    }

    public Domain getIntoAbsolute(String pattern) {
        return new Domain(path.getInto(pattern), false);
    }

    public Domain getIntoAbsolute(String... patterns) {
        return getIntoAbsolute(patterns, 0, patterns.length);
    }

    public Domain getIntoAbsolute(String[] patterns, int offset, int length) {
        if (!relative && length == 0) {
            return this;
        }
        return new Domain(path.getInto(patterns, offset, length), false);
    }

    public boolean isChildDomainOf(Domain parent) {
        return path.startsWith(parent.path);
    }

    public boolean isParentDomainOf(Domain child) {
        return child.path.startsWith(path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Domain domain = (Domain) o;
        return relative == domain.relative &&
                path.equals(domain.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, relative);
    }

    @Override
    public String toString() {
        if (relative) {
            return Utils.domainSeparator + path.toInverseString(Utils.domainSeparator, null);
        }
        return path.toInverseString(Utils.domainSeparator, null);
    }

    public static Domain parse(String source) throws ParseException {
        int start = 0;
        boolean relative;
        relative = source.startsWith(Utils.domainSeparator, start);
        if (relative) {
            start += Utils.domainSeparator.length();
        }
        if (start == source.length()) {
            throw new ParseException("Domain须包含至少一个Pattern", start);
        }
        return new Domain(Path.parseReverse(source.substring(start), Utils.domainSeparator, null), relative);
    }

    public static Domain relativeValueOf(String... patterns) {
        return relativeValueOf(patterns, 0, patterns.length);
    }

    public static Domain relativeValueOf(String[] patterns, int offset, int length) {
        return new Domain(Path.reverseValueOf(patterns, offset, length), true);
    }

    public static Domain absoluteValueOf(String... patterns) {
        return absoluteValueOf(patterns, 0, patterns.length);
    }

    public static Domain absoluteValueOf(String[] patterns, int offset, int length) {
        return new Domain(Path.reverseValueOf(patterns, offset, length), false);
    }
}