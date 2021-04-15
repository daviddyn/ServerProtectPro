package com.davidsoft.serverprotect.apps;

import com.davidsoft.collections.ReadOnlyMap;
import com.davidsoft.net.*;
import com.davidsoft.net.http.Utils;
import com.davidsoft.net.http.*;
import com.davidsoft.serverprotect.libs.HttpPath;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Map;

public class BaseWebApplication implements WebApplication {

    private String name;
    private File applicationRootFile;
    private HttpPath workingRootPath;
    private RegexIpIndex<Void> ipWhiteList;
    private DomainIndex<Void> allowDomains;
    private ReadOnlyMap<Integer, String> routers;

    private static final HttpPath FAVICON_PATH = HttpPath.parse("/favicon.ico");

    protected final void initialize(String name, File applicationRootFile, HttpPath workingRootPath, RegexIpIndex<Void> ipWhiteList, DomainIndex<Void> allowDomains, ReadOnlyMap<Integer, String> routers) {
        this.name = name;
        this.applicationRootFile = applicationRootFile;
        this.workingRootPath = workingRootPath;
        this.ipWhiteList = ipWhiteList;
        this.allowDomains = allowDomains;
        this.routers = routers;
    }

    public final File getApplicationRootFile() {
        return applicationRootFile;
    }

    public final HttpPath getWorkingRootPath() {
        return workingRootPath;
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
        //3. 判断是不是访问网站图标
        if (requestInfo.path.equals(FAVICON_PATH)) {
            return onGetFavicon(clientIp);
        }
        //4. 解析相对路径
        HttpPath requestRelativePath;
        if (workingRootPath.isRoot()) {
            requestRelativePath = requestInfo.path;
        }
        else {
            requestRelativePath = requestInfo.path.subPath(workingRootPath.patternCount());
        }
        //5. 执行+路由控制
        HashSet<Integer> routed = new HashSet<>();
        int savedResponseCode = 0;
        String savedResponseDescription = null;
        HttpResponseSender sender;
        while (true) {
            sender = onClientRequest(requestInfo, requestContent, clientIp, requestRelativePath);
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
            String pathString = routers.get(sender.responseInfo.responseCode);
            if (pathString == null) {
                break;
            }
            requestRelativePath = HttpPath.parse(pathString);
            routed.add(sender.responseInfo.responseCode);
        }
        sender.responseInfo.responseCode = savedResponseCode;
        sender.responseInfo.responseDescription = savedResponseDescription;
        return sender;
    }

    protected HttpResponseSender onClientRequest(HttpRequestInfo requestInfo, HttpContentReceiver requestContent, int clientIp, HttpPath requestRelativePath) {
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
            for (Map.Entry<Integer, String> entry : routers.entrySet()) {
                webPageBuilder.append(entry.getKey()).append("&nbsp;-&gt;&nbsp;").append(Utils.escapeHtml(entry.getValue())).append(",&nbsp;");
            }
            webPageBuilder.delete(webPageBuilder.length() - 7, webPageBuilder.length());
        }
        webPageBuilder.append("]</p><p>");
        webPageBuilder.append("请求ip：").append(IP.toString(clientIp)).append("<br>");
        webPageBuilder.append("App工作根网址：").append(Utils.escapeHtml(workingRootPath.toString())).append("<br>");
        webPageBuilder.append("相对请求网址：").append(Utils.escapeHtml(requestRelativePath.toString())).append("</p><p>");
        webPageBuilder.append(Utils.escapeHtml(requestInfo.toString()));
        webPageBuilder.append("</p>");
        return new HttpResponseSender(
                new HttpResponseInfo(200),
                new HttpContentStringProvider(webPageBuilder.toString(), StandardCharsets.UTF_8, "text/html", StandardCharsets.UTF_8)
        );
    }

    protected HttpResponseSender onGetFavicon(int clientIp) {
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