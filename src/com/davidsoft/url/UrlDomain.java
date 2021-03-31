package com.davidsoft.url;

import java.util.Objects;

/**
 * 表示主机。主机中的地址是以"."为分隔符的Path。
 */
public final class UrlDomain {

    public static final String domainSeparator = ".";
    public static final String portSeparator = ":";
    public static final int PORT_UNDEFINED = -1;

    public static UrlDomain parse(String domain, String pathSeparator) {
        //解析前缀
        int start = 0;
        if (pathSeparator != null && domain.startsWith(pathSeparator + pathSeparator)) {
            start = pathSeparator.length() << 1;
        }
        //解析端口号
        int port;
        int findPos = domain.lastIndexOf(portSeparator);
        if (findPos < start) {
            port = PORT_UNDEFINED;
        }
        else {
            try {
                port = Integer.parseInt(domain.substring(findPos + portSeparator.length()));
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("主机格式不合法", e);
            }
            domain = domain.substring(start, findPos);
        }
        return new UrlDomain(Path.parse(domain.substring(start), domainSeparator), port);
    }

    public static UrlDomain parse(String domain) {
        return parse(domain, null);
    }

    private final Path address;
    private final int port;

    public UrlDomain(Path address, int port) {
        this.address = address;
        this.port = port;
    }

    public boolean hasParent() {
        return address.hasParent();
    }

    public Path getAddress() {
        return address;
    }

    public UrlDomain getParent() {
        return new UrlDomain(address.getParent(), port);
    }

    public UrlDomain subPath(int startInclude, int endExclude) {
        return new UrlDomain(address.subPath(startInclude, endExclude), port);
    }

    public UrlDomain getInto(String path) {
        return new UrlDomain(this.address.getInto(path), port);
    }

    public UrlDomain getInto(Path path) {
        return new UrlDomain(this.address.getInto(path), port);
    }

    public UrlDomain getAnotherPath(String path) {
        return new UrlDomain(this.address.getAnotherPath(path), port);
    }

    public UrlDomain getAnotherPath(Path path) {
        return new UrlDomain(this.address.getAnotherPath(path), port);
    }

    public String patternAt(int position) {
        return address.patternAt(position);
    }

    public int patternCount() {
        return address.patternCount();
    }

    public boolean startsWith(String prefix) {
        return address.startsWith(prefix);
    }

    public boolean startsWith(UrlDomain prefix) {
        return address.startsWith(prefix.getAddress());
    }

    public boolean endsWith(String suffix) {
        return address.endsWith(suffix);
    }

    public boolean endsWith(UrlDomain suffix) {
        return address.endsWith(suffix.getAddress());
    }

    public int getPort() {
        return port;
    }

    public UrlDomain setPort(int port) {
        return new UrlDomain(address, port);
    }

    public String toString(String pathSeparator) {
        if (pathSeparator == null) {
            pathSeparator = "";
        }
        else {
            pathSeparator += pathSeparator;
        }
        if (port == PORT_UNDEFINED) {
            return pathSeparator + address.toString(domainSeparator);
        }
        else {
            return pathSeparator + address.toString(domainSeparator) + portSeparator + port;
        }
    }

    @Override
    public String toString() {
        return toString(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlDomain urlDomain = (UrlDomain) o;
        return port == urlDomain.port &&
                address.equals(urlDomain.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }
}
