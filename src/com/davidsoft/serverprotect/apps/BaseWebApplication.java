package com.davidsoft.serverprotect.apps;

import com.davidsoft.collections.ReadOnlyMap;
import com.davidsoft.net.*;
import com.davidsoft.net.http.Utils;
import com.davidsoft.net.http.*;
import com.davidsoft.url.URI;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Map;

public class BaseWebApplication implements WebApplication {

    private String name;
    private File applicationRootFile;
    private URI workingRootURI;
    private RegexIpIndex<Void> ipWhiteList;
    private DomainIndex<Object> allowDomains;
    private ReadOnlyMap<Integer, URI> routers;

    public static final URI FAVICON_URI = URI.valueOfResource(false, "favicon.ico");

    protected final void initialize(String name, File applicationRootFile, URI workingRootURI, RegexIpIndex<Void> ipWhiteList, DomainIndex<Object> allowDomains, ReadOnlyMap<Integer, URI> routers) {
        this.name = name;
        this.applicationRootFile = applicationRootFile;
        this.workingRootURI = workingRootURI;
        this.ipWhiteList = ipWhiteList;
        this.allowDomains = allowDomains;
        this.routers = routers;
    }

    public final File getApplicationRootFile() {
        return applicationRootFile;
    }

    public final URI getWorkingRootURI() {
        return workingRootURI;
    }

    @Override
    public void onCreate() {}

    @Override
    public void onUrge() {

    }

    @Override
    public void onForceStop() {

    }

    @Override
    public void onDestroy() {}

    @Override
    public final HttpResponseSender onClientRequest(HttpRequestInfo requestInfo, HttpContentReceiver requestContent, int clientIp, int serverPort, boolean ssl) {
        //1. ip白名单控制
        if (!ipWhiteList.isEmpty() && !ipWhiteList.contains(clientIp)) {
            return null;
        }
        //2. Host过滤
        String hostField = requestInfo.headers.getFieldValue("Host");
        if (hostField == null) {
            return null;
        }
        else {
            Host host;
            try {
                host = Host.parse(hostField);
            } catch (ParseException e) {
                return null;
            }
            //String domain = com.davidsoft.net.http.Utils.getHostFromDomain(hostField);
            int port = host.getPort();
            if (port == Host.PORT_DEFAULT) {
                port = Utils.getDefaultPort(ssl);
            }
            if (port != serverPort || (!allowDomains.isEmpty() && allowDomains.get(host.getDomain()) == null)) {
                return null;
            }
        }
        //3. 解析相对路径
        System.out.println("requestInfo.uri = " + NetURI.toString(requestInfo.uri));
        System.out.println("workingRootURI = " + NetURI.toString(workingRootURI));
        URI requestRelativeURI = requestInfo.uri.subURI(workingRootURI.patternCount(), requestInfo.uri.patternCount());
        //4. 判断是不是访问网站图标
        if (requestRelativeURI.equals(FAVICON_URI)) {
            return onGetFavicon(requestInfo, requestContent, clientIp, requestRelativeURI);
        }
        //5. 执行+路由控制
        HashSet<Integer> routed = new HashSet<>();
        int savedResponseCode = 0;
        String savedResponseDescription = null;
        HttpResponseSender sender;
        while (true) {
            sender = onClientRequest(requestInfo, requestContent, clientIp, requestRelativeURI);
            if (sender == null) {
                return null;
            }
            if (savedResponseCode == 0) {
                savedResponseCode = sender.responseInfo.responseCode;
                savedResponseDescription = sender.responseInfo.responseDescription;
            }
            if (routed.contains(sender.responseInfo.responseCode)) {
                break;
            }
            URI routedURI = routers.get(sender.responseInfo.responseCode);
            if (routedURI == null) {
                break;
            }
            requestRelativeURI = routedURI;
            routed.add(sender.responseInfo.responseCode);
        }
        sender.responseInfo.responseCode = savedResponseCode;
        sender.responseInfo.responseDescription = savedResponseDescription;
        return sender;
    }

    protected HttpResponseSender onClientRequest(HttpRequestInfo requestInfo, HttpContentReceiver requestContent, int clientIp, URI requestRelativeURI) {
        StringBuilder webPageBuilder = new StringBuilder();
        webPageBuilder.append("<!DOCTYPE html><html><head><title>David Soft Server Protect Pro</title><body><h1>欢迎使用 David Soft Server Protect Pro</h1><hr><p>此页面是&nbsp;BaseWebApplication.onClientRequest(HttpRequestReceiver, String, HttpPath)&nbsp;的缺省实现，请重写此方法来实现自己的业务。</p><p>");
        webPageBuilder.append("App名称：").append(Utils.escapeHtml(name)).append("<br>");
        webPageBuilder.append("App工作目录：").append(Utils.escapeHtml(applicationRootFile.getAbsolutePath())).append("<br>");
        webPageBuilder.append("访问白名单：[");
        if (!ipWhiteList.isEmpty()) {
            for (Map.Entry<Long, Void> entry : ipWhiteList.entrySet()) {
                webPageBuilder.append(RegexIP.toString(entry.getKey())).append(",&nbsp;");
            }
            webPageBuilder.delete(webPageBuilder.length() - 7, webPageBuilder.length());
        }
        webPageBuilder.append("]").append("<br>");
        webPageBuilder.append("访问允许使用的域名：[");
        if (!allowDomains.isEmpty()) {
            allowDomains.forEachKey(domain -> webPageBuilder.append(domain).append(",&nbsp;"));
            webPageBuilder.delete(webPageBuilder.length() - 7, webPageBuilder.length());
        }
        webPageBuilder.append("]").append("<br>");
        webPageBuilder.append("错误路由：[");
        if (!routers.isEmpty()) {
            for (Map.Entry<Integer, URI> entry : routers.entrySet()) {
                webPageBuilder.append(entry.getKey()).append("&nbsp;-&gt;&nbsp;").append(Utils.escapeHtml(NetURI.toString(entry.getValue()))).append(",&nbsp;");
            }
            webPageBuilder.delete(webPageBuilder.length() - 7, webPageBuilder.length());
        }
        webPageBuilder.append("]</p><p>");
        webPageBuilder.append("请求ip：").append(IP.toString(clientIp)).append("<br>");
        webPageBuilder.append("App工作根网址：").append(Utils.escapeHtml(NetURI.toString(workingRootURI))).append("<br>");
        webPageBuilder.append("相对请求网址(除去App映射路径后的网址，应以此地址为准进行业务开发。)：").append(Utils.escapeHtml(NetURI.toString(requestRelativeURI))).append("</p><p>");
        webPageBuilder.append(Utils.escapeHtml(requestInfo.toString()));
        webPageBuilder.append("</p>");
        return new HttpResponseSender(
                new HttpResponseInfo(200),
                new HttpContentStringProvider(webPageBuilder.toString(), StandardCharsets.UTF_8, "text/html", StandardCharsets.UTF_8)
        );
    }

    protected HttpResponseSender onGetFavicon(HttpRequestInfo requestInfo, HttpContentReceiver requestContent, int clientIp, URI requestRelativeURI) {
        return new HttpResponseSender(new HttpResponseInfo(404), null);
    }

    @Override
    public final boolean isProtectEnabled() {
        return ipWhiteList.isEmpty();
    }

    @Override
    public final String getName() {
        return name;
    }
}