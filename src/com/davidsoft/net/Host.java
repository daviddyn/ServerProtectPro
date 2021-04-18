package com.davidsoft.net;

import java.text.ParseException;
import java.util.Objects;

public class Host {

    public static final int PORT_DEFAULT = -1;

    private final Domain domain;
    private final int port;

    public Host(Domain domain, int port) {
        if (domain == null) {
            throw new IllegalArgumentException("主机不能为null");
        }
        this.domain = domain;
        this.port = port;
    }

    public Domain getDomain() {
        return domain;
    }

    public int getPort() {
        return getPort(PORT_DEFAULT);
    }

    public int getPort(int defaultPort) {
        if (port == PORT_DEFAULT) {
            return defaultPort;
        }
        return port;
    }

    public Host setPort(int port) {
        if (port == this.port) {
            return this;
        }
        return new Host(domain, port);
    }

    public Host setDomain(Domain domain) {
        if (domain == this.domain) {
            return this;
        }
        return new Host(domain, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Host host = (Host) o;
        return port == host.port &&
                domain.equals(host.domain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domain, port);
    }

    public String toString(boolean hasPrefix, int defaultPort) {
        if (hasPrefix) {
            if (port == defaultPort) {
                return Utils.hostPrefix + domain;
            }
            return Utils.hostPrefix + domain + Utils.portSeparator + port;
        }
        else {
            if (port == defaultPort) {
                return domain.toString();
            }
            return domain.toString() + Utils.portSeparator + port;
        }
    }

    @Override
    public String toString() {
        return toString(true, PORT_DEFAULT);
    }

    public static Host parse(String source) throws ParseException {
        return parse(source, PORT_DEFAULT);
    }

    public static Host parse(String source, int defaultPort) throws ParseException {
        int portPos = source.lastIndexOf(Utils.portSeparator);
        if (portPos == -1) {
            portPos = source.length();
        }
        int start = source.startsWith(Utils.hostPrefix) ? Utils.hostPrefix.length() : 0;
        Domain domain = Domain.parse(source.substring(start, portPos));
        int port;
        if (portPos == source.length()) {
            port = defaultPort;
        }
        else {
            try {
                port = Integer.parseInt(source.substring(portPos + Utils.portSeparator.length()));
            } catch (NumberFormatException e) {
                throw new ParseException("端口号格式不正确。", portPos + Utils.portSeparator.length());
            }
        }
        return new Host(domain, port);
    }
}
