package com.davidsoft.net;

import java.text.ParseException;
import java.util.Objects;

public class Origin {

    private final String protocol;
    private final Host host;

    public Origin(String protocol, Host host) {
        if (host == null) {
            throw new IllegalArgumentException("主机不能为null");
        }
        if (protocol != null && protocol.isEmpty()) {
            throw new IllegalArgumentException("协议不能为空");
        }
        this.protocol = protocol;
        this.host = host;
    }

    public boolean isRelative() {
        return protocol == null;
    }

    public String getProtocol() {
        return protocol;
    }

    public Origin setProtocol(String protocol) {
        if (protocol.equals(this.protocol)) {
            return this;
        }
        return new Origin(protocol, host);
    }

    public Host getHost() {
        return host;
    }

    public Origin setHost(Host host) {
        if (host.equals(this.host)) {
            return this;
        }
        return new Origin(protocol, host);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Origin origin = (Origin) o;
        return Objects.equals(protocol, origin.protocol) &&
                host.equals(origin.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, host);
    }

    public String toString(int defaultPort) {
        if (protocol == null) {
            return host.toString(true, defaultPort);
        }
        return protocol + Utils.protocolSuffix + host.toString(true, defaultPort);
    }

    @Override
    public String toString() {
        return toString(Host.PORT_DEFAULT);
    }

    public static Origin parse(String source) throws ParseException {
        int findPos = source.indexOf(Utils.hostPrefix);
        if (findPos == -1) {
            throw new ParseException("缺少主机", source.length());
        }
        if (findPos == 0) {
            return new Origin(null, Host.parse(source));
        }
        int protocolEnd = findPos - Utils.protocolSuffix.length();
        if (!source.startsWith(Utils.protocolSuffix, protocolEnd)) {
            throw new ParseException("主机格式不正确", findPos);
        }
        return new Origin(
                source.substring(0, protocolEnd),
                Host.parse(source.substring(findPos))
        );
    }
}