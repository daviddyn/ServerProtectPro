package com.davidsoft.serverprotect.apps;

import com.davidsoft.serverprotect.Utils;
import com.davidsoft.serverprotect.http.*;
import com.davidsoft.serverprotect.libs.HttpPath;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;

public class ForwardWebApplication extends BaseWebApplication {

    private final String targetDomain;
    private final int targetPort;
    private final boolean targetSSL;
    private final boolean forwardIp;

    private Socket socket;

    public ForwardWebApplication(String targetDomain, int targetPort, boolean targetSSL, boolean forwardIp) {
        this.targetDomain = targetDomain;
        this.targetPort = targetPort;
        this.targetSSL = targetSSL;
        this.forwardIp = forwardIp;
    }

    private boolean tryConnect() {
        if (socket != null) {
            return true;
        }
        try {
            if (targetSSL) {
                socket = SSLSocketFactory.getDefault().createSocket(targetDomain, targetPort);
            }
            else {
                socket = new Socket(targetDomain, targetPort);
            }
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onCreate() {
        tryConnect();
    }

    @Override
    public void onDestroy() {
        if (socket != null) {
            Utils.closeWithoutException(socket, true);
        }
    }

    @Override
    protected HttpResponseSender onClientRequest(HttpRequestReceiver requestReceiver, String ip, HttpPath requestRelativePath) {
        if (!tryConnect()) {
            return new HttpResponseSender(new HttpResponseInfo(500), null);
        }
        HttpRequestInfo forwardRequestInfo = new HttpRequestInfo(requestReceiver.getRequestInfo());
        //1. 重定向Path
        forwardRequestInfo.path = requestRelativePath;
        //2. 重定向host域
        if (targetSSL) {
            if (targetPort == 443) {
                forwardRequestInfo.headers.setFieldValue("Host", targetDomain);
            }
            else {
                forwardRequestInfo.headers.setFieldValue("Host", targetDomain + ":" + targetPort);
            }
        }
        else {
            if (targetPort == 80) {
                forwardRequestInfo.headers.setFieldValue("Host", targetDomain);
            }
            else {
                forwardRequestInfo.headers.setFieldValue("Host", targetDomain + ":" + targetPort);
            }
        }
        //3. 添加Forward字段
        if (forwardIp) {
            forwardRequestInfo.headers.setFieldValue("X-Forwarded-For", ip);
        }
        //4. 发给目标服务器
        requestReceiver.analyseContent();
        //HttpRequestSender forwardRequestSender = new HttpRequestSender(forwardRequestInfo, new )
        return null;
    }

    @Override
    protected HttpResponseSender onGetFavicon(String ip) {
        //要请求废物图标
        //return new HttpResponseSender(new HttpResponseInfo(404), null);
        return null;
    }
}
