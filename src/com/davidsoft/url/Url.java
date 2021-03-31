package com.davidsoft.url;

/**
 * 表示一个资源，由资源所在的位置(UrlLocation)和资源名称组成.
 */
public class Url {

    private final UrlLocation location;
    private final String name;

    public Url(UrlLocation location, String name) {
        this.location = location;
        this.name = name;
    }

    public static Url parse(String src, String protocolSuffix, String pathSeparator) {
        if (src.endsWith(pathSeparator)) {
            throw new IllegalArgumentException("Url不能以\"" + pathSeparator + "\"结尾：" + src);
        }
        UrlLocation location = UrlLocation.parse(src, protocolSuffix, pathSeparator);
        if (location.getPath() == null || location.pathPatternCount() == 0) {
            throw new IllegalArgumentException("缺少路径：" + src);
        }
        String name = location.pathPatternAt(location.pathPatternCount() - 1);
        return new Url(location.getParent(), name);
    }

    public UrlLocation getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String toString(String protocolSuffix, String pathSeparator) {
        return location.toString(protocolSuffix, pathSeparator) + name;
    }
}
