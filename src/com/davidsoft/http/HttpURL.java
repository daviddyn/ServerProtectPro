package com.davidsoft.http;

import com.davidsoft.utils.Path;
import com.davidsoft.utils.URI;

import java.text.ParseException;
import java.util.Objects;

public class HttpURL {

    public static final String protocolSuffix = ":";

    private final String protocol;
    private final Path host;
    private final HttpURI uri;

    public HttpURL(String protocol, Path host, HttpURI uri) {
        this.protocol = protocol;
        this.host = host;
        this.uri = uri;
    }

    public String getProtocol() {
        return protocol;
    }

    public Path getHost() {
        return host;
    }

    public HttpURI getUri() {
        return uri;
    }

    public boolean isRelative() {
        return uri.isRelative() || host == null || protocol == null;
    }

    public boolean isProtocolRelative() {
        return protocol == null;
    }

    public boolean isExactlyProtocolRelative() {
        return protocol == null && host != null;
    }

    public boolean isHostRelative() {
        return host == null;
    }

    public boolean isExactlyHostRelative() {
        return host == null && !uri.isRelative();
    }

    public boolean isURIRelative() {
        return uri.isRelative();
    }

    public boolean isExactlyURIRelative() {
        return uri.isRelative();
    }

    public boolean isRoot() {
        return !uri.isRelative() && uri.locationIsRoot();
    }

    public boolean isLocation() {
        return uri.isLocation();
    }

    public HttpURL asLocation() {
        return new HttpURL(protocol, host, uri.asLocation());
    }

    public boolean isResource() {
        return uri.isResource();
    }

    public HttpURL asResource() {
        return new HttpURL(protocol, host, uri.asResource());
    }

    public HttpURL subLocation(int startInclude, int endExclude) {
        return new HttpURL(protocol, host, uri.subLocation(startInclude, endExclude));
    }

    public HttpURL subResource(int startInclude, int endExclude) {
        return new HttpURL(protocol, host, uri.subResource(startInclude, endExclude));
    }

    public HttpURL getParentLocation() {
        return new HttpURL(protocol, host, uri.getParentLocation());
    }

    public HttpURL getParentLocation(int count) {
        return new HttpURL(protocol, host, uri.getParentLocation(count));
    }

    public HttpURL getParentResource() {
        return new HttpURL(protocol, host, uri.getParentResource());
    }

    public HttpURL getParentResource(int count) {
        return new HttpURL(protocol, host, uri.getParentResource(count));
    }

    public HttpURL getIntoLocation(Path another) {
        return new HttpURL(protocol, host, uri.getIntoLocation(another));
    }

    public HttpURL getIntoLocation(Path another, int startInclude, int endExclude) {
        return new HttpURL(protocol, host, uri.getIntoLocation(another, startInclude, endExclude));
    }

    public HttpURL getIntoLocation(String location) {
        return new HttpURL(protocol, host, uri.getIntoLocation(location));
    }

    public HttpURL getIntoLocation(String... patterns) {
        return new HttpURL(protocol, host, uri.getIntoLocation(patterns));
    }

    public HttpURL getIntoLocation(String[] patterns, int offset, int length) {
        return new HttpURL(protocol, host, uri.getIntoLocation(patterns, offset, length));
    }

    public HttpURL getIntoResource(Path another) {
        return new HttpURL(protocol, host, uri.getIntoResource(another));
    }

    public HttpURL getIntoResource(Path another, int startInclude, int endExclude) {
        return new HttpURL(protocol, host, uri.getIntoResource(another, startInclude, endExclude));
    }

    public HttpURL getIntoResource(String resource) {
        return new HttpURL(protocol, host, uri.getIntoResource(resource));
    }

    public HttpURL getIntoResource(String... patterns) {
        return new HttpURL(protocol, host, uri.getIntoResource(patterns));
    }

    public HttpURL getIntoResource(String[] patterns, int offset, int length) {
        return new HttpURL(protocol, host, uri.getIntoResource(patterns, offset, length));
    }

    public HttpURL getInto(URI uri) {
        return new HttpURL(protocol, host, this.uri.getInto(uri));
    }

    public HttpURL getInto(HttpURI httpURI) {
        return new HttpURL(protocol, host, uri.getInto(httpURI));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpURL httpURL = (HttpURL) o;
        return Objects.equals(protocol, httpURL.protocol) &&
                Objects.equals(host, httpURL.host) &&
                uri.equals(httpURL.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, host, uri);
    }

    public String toOriginString() {
        if (host == null) {
            return null;
        }
        if (protocol == null) {
            return HttpURI.pathSeparator + HttpURI.pathSeparator + host;
        }
        else {
            return protocol + protocolSuffix + HttpURI.pathSeparator + HttpURI.pathSeparator + host;
        }
    }

    public String toLocationString() {

        return uri.toLocationString(pathSeparator, httpUrlEncoder);
    }

    @Override
    public String toString() {
        return uri.toString(pathSeparator, httpUrlEncoder);
    }

    public static HttpURI parse(String source) throws ParseException {
        return new HttpURI(URI.parse(source, pathSeparator, httpUrlDecoder));
    }

    public static HttpURI valueOfLocation(boolean relative, String... patterns) {
        return new HttpURI(URI.valueOfLocation(relative, patterns));
    }

    public static HttpURI valueOfLocation(boolean relative, String[] patterns, int offset, int length) {
        return new HttpURI(URI.valueOfLocation(relative, patterns, offset, length));
    }

    public static HttpURI valueOfResource(boolean relative, String... patterns) {
        return new HttpURI(URI.valueOfResource(relative, patterns));
    }

    public static HttpURI valueOfResource(boolean relative, String[] patterns, int offset, int length) {
        return new HttpURI(URI.valueOfResource(relative, patterns, offset, length));
    }
}
