package com.davidsoft.http;

import com.davidsoft.utils.Path;
import com.davidsoft.utils.StringEscapeDecoder;
import com.davidsoft.utils.StringEscapeEncoder;
import com.davidsoft.utils.URI;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;

public class HttpURI {

    public static final String pathSeparator = "/";

    private static final StringEscapeEncoder httpUrlEncoder = src -> UrlCodec.urlEncode(src.getBytes(StandardCharsets.UTF_8));

    private static final StringEscapeDecoder httpUrlDecoder = src -> UrlCodec.urlDecodeString(src, StandardCharsets.UTF_8);

    private final URI uri;

    public HttpURI(Path location, boolean relative, String resourceName) {
        uri = new URI(location, relative, resourceName);
    }

    public HttpURI(URI uri) {
        this.uri = uri;
    }

    public Path getLocation() {
        return uri.getLocation();
    }

    public String getResourceName() {
        return uri.getResourceName();
    }

    public boolean isLocation() {
        return uri.isLocation();
    }

    public HttpURI asLocation() {
        return new HttpURI(uri.asLocation());
    }

    public boolean isResource() {
        return uri.isResource();
    }

    public HttpURI asResource() {
        return new HttpURI(uri.asResource());
    }

    public boolean isRelative() {
        return uri.isRelative();
    }

    public boolean locationIsRoot() {
        return uri.locationIsRoot();
    }

    public int patternCount() {
        return uri.patternCount();
    }

    public String patternAt(int position) {
        return uri.patternAt(position);
    }

    public HttpURI subLocation(int startInclude, int endExclude) {
        return new HttpURI(uri.subLocation(startInclude, endExclude));
    }

    public HttpURI subResource(int startInclude, int endExclude) {
        return new HttpURI(uri.subResource(startInclude, endExclude));
    }

    public HttpURI getParentLocation() {
        return new HttpURI(uri.getParentLocation());
    }

    public HttpURI getParentLocation(int count) {
        return new HttpURI(uri.getParentLocation(count));
    }

    public HttpURI getParentResource() {
        return new HttpURI(uri.getParentResource());
    }

    public HttpURI getParentResource(int count) {
        return new HttpURI(uri.getParentResource(count));
    }

    public HttpURI getIntoLocation(Path another) {
        return new HttpURI(uri.getIntoLocation(another));
    }

    public HttpURI getIntoLocation(Path another, int startInclude, int endExclude) {
        return new HttpURI(uri.getIntoLocation(another, startInclude, endExclude));
    }

    public HttpURI getIntoLocation(String location) {
        return new HttpURI(uri.getIntoLocation(location));
    }

    public HttpURI getIntoLocation(String... patterns) {
        return new HttpURI(uri.getIntoLocation(patterns));
    }

    public HttpURI getIntoLocation(String[] patterns, int offset, int length) {
        return new HttpURI(uri.getIntoLocation(patterns, offset, length));
    }

    public HttpURI getIntoResource(Path another) {
        return new HttpURI(uri.getIntoResource(another));
    }

    public HttpURI getIntoResource(Path another, int startInclude, int endExclude) {
        return new HttpURI(uri.getIntoResource(another, startInclude, endExclude));
    }

    public HttpURI getIntoResource(String resource) {
        return new HttpURI(uri.getIntoResource(resource));
    }

    public HttpURI getIntoResource(String... patterns) {
        return new HttpURI(uri.getIntoResource(patterns));
    }

    public HttpURI getIntoResource(String[] patterns, int offset, int length) {
        return new HttpURI(uri.getIntoResource(patterns, offset, length));
    }

    public HttpURI getInto(URI uri) {
        return new HttpURI(this.uri.getInto(uri));
    }

    public HttpURI getInto(HttpURI httpURI) {
        return new HttpURI(uri.getInto(httpURI.uri));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpURI httpURI = (HttpURI) o;
        return uri.equals(httpURI.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
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
