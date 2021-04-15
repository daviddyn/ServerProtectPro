package com.davidsoft.net;

import com.davidsoft.net.http.HttpURIUtils;
import com.davidsoft.url.Path;
import com.davidsoft.url.URI;

import java.text.ParseException;
import java.util.Objects;

public class URL {

    private final Origin origin;
    private final URI uri;

    public URL(Origin origin, URI uri) {
        if (uri.isRelative() && origin != null) {
            throw new IllegalArgumentException("当uri是相对路径时，origin必须为null");
        }
        this.origin = origin;
        this.uri = uri;
    }

    public Origin getOrigin() {
        return origin;
    }

    public URI getUri() {
        return uri;
    }

    public boolean isRelative() {
        return origin.isRelative() || uri.isRelative();
    }

    public boolean isProtocolRelative() {
        return origin == null || origin.isRelative();
    }

    public boolean isExactlyProtocolRelative() {
        return origin != null && origin.isRelative();
    }

    public boolean isHostRelative() {
        return origin == null;
    }

    public boolean isExactlyHostRelative() {
        return origin == null && !uri.isRelative();
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

    public URL asLocation() {
        return new URL(origin, uri.asLocation());
    }

    public boolean isResource() {
        return uri.isResource();
    }

    public URL asResource() {
        return new URL(origin, uri.asResource());
    }

    public URL getParentLocation() {
        return new URL(origin, uri.getParentLocation());
    }

    public URL getParentLocation(int count) {
        return new URL(origin, uri.getParentLocation(count));
    }

    public URL getParentResource() {
        return new URL(origin, uri.getParentResource());
    }

    public URL getParentResource(int count) {
        return new URL(origin, uri.getParentResource(count));
    }

    public URL getIntoLocation(Path another) {
        return new URL(origin, uri.getIntoLocation(another));
    }

    public URL getIntoLocation(Path another, int startInclude, int endExclude) {
        return new URL(origin, uri.getIntoLocation(another, startInclude, endExclude));
    }

    public URL getIntoLocation(String location) {
        return new URL(origin, uri.getIntoLocation(location));
    }

    public URL getIntoLocation(String... patterns) {
        return new URL(origin, uri.getIntoLocation(patterns));
    }

    public URL getIntoLocation(String[] patterns, int offset, int length) {
        return new URL(origin, uri.getIntoLocation(patterns, offset, length));
    }

    public URL getIntoResource(Path another) {
        return new URL(origin, uri.getIntoResource(another));
    }

    public URL getIntoResource(Path another, int startInclude, int endExclude) {
        return new URL(origin, uri.getIntoResource(another, startInclude, endExclude));
    }

    public URL getIntoResource(String resource) {
        return new URL(origin, uri.getIntoResource(resource));
    }

    public URL getIntoResource(String... patterns) {
        return new URL(origin, uri.getIntoResource(patterns));
    }

    public URL getIntoResource(String[] patterns, int offset, int length) {
        return new URL(origin, uri.getIntoResource(patterns, offset, length));
    }

    public URL getInto(URI uri) {
        return new URL(origin, this.uri.getInto(uri));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        URL url = (URL) o;
        return Objects.equals(origin, url.origin) &&
                uri.equals(url.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, uri);
    }

    public String toLocationString() {
        if (origin == null) {
            return HttpURIUtils.toLocationString(uri);
        }
        else {
            return origin.toString() + HttpURIUtils.toLocationString(uri);
        }
    }

    @Override
    public String toString() {
        if (origin == null) {
            return HttpURIUtils.toString(uri);
        }
        else {
            return origin.toString() + HttpURIUtils.toString(uri);
        }
    }

    public static URL parse(String source) throws ParseException {
        int findPos = source.indexOf(Utils.hostPrefix);
        if (findPos == -1) {
            //没有主机前缀，则将所有内容解析为URI
            return new URL(null, HttpURIUtils.parse(source));
        }
        //有主机前缀，则从主机前缀之后找到URI的开始
        int uriStart = source.indexOf(Utils.pathSeparator, findPos + Utils.hostPrefix.length());
        return new URL(
                Origin.parse(source.substring(0, uriStart)),
                HttpURIUtils.parse(source.substring(uriStart))
        );
    }

    public static URL getAbsoluteURL(URL baseUrl, URL relativeUrl) {
        if (relativeUrl.isURIRelative()) {
            return new URL(baseUrl.origin, baseUrl.uri.getInto(relativeUrl.uri));
        }
        if (relativeUrl.origin == null) {
            return new URL(baseUrl.origin, relativeUrl.uri);
        }
        if (relativeUrl.origin.isRelative()) {
            return new URL(relativeUrl.origin.setProtocol(baseUrl.origin.getProtocol()), relativeUrl.uri);
        }
        return relativeUrl;
    }
}
