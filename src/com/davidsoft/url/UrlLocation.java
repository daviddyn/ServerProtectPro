package com.davidsoft.url;

import java.util.Objects;

/**
 * 表示一个资源所在的位置
 */
public final class UrlLocation {

    private final String protocol;
    private final UrlDomain domain;
    private final boolean startsWithSeparator;
    private final Path path;

    //识别协议，包括后缀
    //识别成功，返回协议串；不包含协议，返回null；出错，抛出异常
    private static String parseProtocol(String src, int[] position, String protocolSuffix) {
        int findPos = src.indexOf(protocolSuffix, position[0]);
        if (findPos == -1) {
            //找不到“:”，认定为不包含协议
            return null;
        }
        //如果protocolSuffix与UrlDomain中的端口号分隔符相同，则还要判断
        if (UrlDomain.portSeparator.equals(protocolSuffix)) {
            int dotPos = src.indexOf(UrlDomain.domainSeparator, position[0]);
            if (dotPos >= 0 && dotPos < findPos) {
                return null;
            }
        }
        if (findPos == position[0]) {
            //“:”之前无内容(“:”出现在串首)，认定为协议缺失
            throw new IllegalArgumentException("缺少协议：" + src);
        }
        String protocol = src.substring(position[0], findPos);
        position[0] = findPos + protocolSuffix.length();
        return protocol;
    }

    //识别主机，从“//”开始到“/"结束
    //识别成功，返回UrlDomain对象；不包含主机，返回null；出错，抛出异常
    private static UrlDomain parseDomain(String src, int[] position, String pathSeparator) {
        String prefix = pathSeparator + pathSeparator;
        if (src.startsWith(prefix, position[0])) {
            //以“//”开头，则“/”将之前的内容认定为主机
            position[0] += prefix.length();
        }
        else {
            if (src.startsWith(pathSeparator, position[0])) {
                //不以”//“开头，但以“/”开头，则认定为不包含主机
                return null;
            }
            //不以”//“开头，不以“/”开头，则“/”将之前的内容认定为主机
        }
        int findPos = src.indexOf(pathSeparator, position[0]);
        if (findPos == -1) {
            //此后不含”/“，则剩余内容都是主机
            findPos = src.length();
        }
        if (position[0] == findPos) {
            //”//“后再无内容，认定为主机缺失
            throw new IllegalArgumentException("缺少主机：" + src);
        }

        UrlDomain domain = UrlDomain.parse(src.substring(position[0], findPos), pathSeparator);
        position[0] = findPos;
        return domain;
    }

    //识别路径，从“/”开始
    //识别成功，返回Path对象；不包含路径，返回null；出错，抛出异常
    private static Path parsePath(String src, int[] position, String pathSeparator) {
        if (position[0] == src.length()) {
            return null;
        }
        String pathString = src.substring(position[0]);
        Path path = Path.parse(pathString, pathSeparator);
        position[0] = src.length();
        return path;
    }

    public static UrlLocation parse(String src, String protocolSuffix, String pathSeparator) {
        int[] position = new int[1];
        String protocol = parseProtocol(src, position, protocolSuffix);
        UrlDomain domain = parseDomain(src, position, pathSeparator);
        boolean startsWithSeparator = src.startsWith(pathSeparator, position[0]);
        Path path = parsePath(src, position, pathSeparator);
        return new UrlLocation(protocol, domain, startsWithSeparator, path);
    }

    public UrlLocation(String protocol, UrlDomain domain, boolean startsWithSeparator, Path path) {
        this.protocol = protocol;
        this.domain = domain;
        this.startsWithSeparator = startsWithSeparator;
        this.path = path;
    }

    public String getProtocol() {
        return protocol;
    }

    public UrlDomain getDomain() {
        return domain;
    }

    public Path getPath() {
        return path;
    }

    public boolean hasParent() {
        return path.hasParent();
    }

    public UrlLocation getParent() {
        return new UrlLocation(protocol, domain, startsWithSeparator, path.getParent());
    }

    public UrlLocation subLocation(int startInclude, int endExclude) {
        return new UrlLocation(protocol, domain, startsWithSeparator, path.subPath(startInclude, endExclude));
    }

    public UrlLocation getInto(String path) {
        return new UrlLocation(protocol, domain, startsWithSeparator, this.path.getInto(path));
    }

    public UrlLocation getInto(Path another) {
        return new UrlLocation(protocol, domain, startsWithSeparator, path.getInto(another));
    }

    public UrlLocation getAnotherLocation(String... path) {
        return new UrlLocation(protocol, domain, startsWithSeparator, this.path.getAnotherPath(path));
    }

    public UrlLocation getAnotherLocation(Path another) {
        return new UrlLocation(protocol, domain, startsWithSeparator, path.getAnotherPath(another));
    }

    public String pathPatternAt(int position) {
        return path.patternAt(position);
    }

    public int pathPatternCount() {
        return path.patternCount();
    }

    public boolean startsWith(String prefix) {
        return path.startsWith(prefix);
    }

    public boolean startsWith(Path prefix) {
        return path.startsWith(prefix);
    }

    public boolean endsWith(String suffix) {
        return path.endsWith(suffix);
    }

    public boolean endsWith(Path suffix) {
        return path.endsWith(suffix);
    }

    public String toString(String protocolSuffix, String pathSeparator) {
        if (protocol == null) {
            if (domain == null) {
                if (startsWithSeparator) {
                    return pathSeparator + path.toString(pathSeparator) + pathSeparator;
                }
                else {
                    return path.toString(pathSeparator) + pathSeparator;
                }
            }
            else {
                if (startsWithSeparator) {
                    return domain.toString(pathSeparator) + pathSeparator + path.toString(pathSeparator) + pathSeparator;
                }
                else {
                    return domain.toString(pathSeparator) + path.toString(pathSeparator) + pathSeparator;
                }
            }
        }
        else {
            if (domain == null) {
                if (startsWithSeparator) {
                    return protocol + protocolSuffix + pathSeparator + path.toString(pathSeparator) + pathSeparator;
                }
                else {
                    return protocol + protocolSuffix + path.toString(pathSeparator) + pathSeparator;
                }
            }
            else {
                if (startsWithSeparator) {
                    return protocol + protocolSuffix + pathSeparator + domain.toString(pathSeparator) + path.toString(pathSeparator) + pathSeparator;
                }
                else {
                    return protocol + protocolSuffix + domain.toString(pathSeparator) + path.toString(pathSeparator) + pathSeparator;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "UrlLocation{" +
                "protocol='" + protocol + '\'' +
                ", domain=" + domain +
                ", startsWithSeparator=" + startsWithSeparator +
                ", path=" + path +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlLocation that = (UrlLocation) o;
        return startsWithSeparator == that.startsWithSeparator &&
                Objects.equals(protocol, that.protocol) &&
                Objects.equals(domain, that.domain) &&
                path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, domain, startsWithSeparator, path);
    }
}
