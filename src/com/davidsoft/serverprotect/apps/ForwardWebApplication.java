package com.davidsoft.serverprotect.apps;

import com.davidsoft.net.*;
import com.davidsoft.net.http.*;
import com.davidsoft.url.URI;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.ParseException;

public class ForwardWebApplication extends BaseWebApplication {

    private final String targetDomain;
    private final int targetPort;
    private final boolean targetSSL;
    private final boolean forwardIp;

    private Socket targetSocket;
    private OutputStream targetOutputStream;
    private InputStream targetInputStream;

    public ForwardWebApplication(String targetDomain, int targetPort, boolean targetSSL, boolean forwardIp) {
        this.targetDomain = targetDomain;
        this.targetPort = targetPort;
        this.targetSSL = targetSSL;
        this.forwardIp = forwardIp;
    }

    @Override
    public void onUrge() {
        super.onUrge();
    }

    @Override
    public void onForceStop() {
        super.onForceStop();
    }

    @Override
    protected HttpResponseSender onClientRequest(HttpRequestInfo requestInfo, HttpContentReceiver requestContent, int clientIp, URI requestRelativeURI) {
        //1. 将收到的请求转换为要发给目标服务器的格式

        Origin targetOrigin;
        try {
            targetOrigin = new Origin(targetSSL ? "http" : "https", new Host(Domain.parse(targetDomain), targetPort));
        } catch (ParseException ignored) {
            //unreachable
            return null;
        }
		HttpRequestInfo targetRequestInfo = new HttpRequestInfo(requestInfo);
        //更改Host
		targetRequestInfo.headers.setFieldValue("Host", targetOrigin.getHost().toString(false, targetSSL ? 443 : 80));
        //如果有Origin，则更改Origin
        if (targetRequestInfo.headers.containsField("Origin")) {
            targetRequestInfo.headers.setFieldValue("Origin", targetOrigin.toString(targetSSL ? 443 : 80));
        }
		//如果有Referer，则更改Referer的Origin部分
        String value = targetRequestInfo.headers.getFieldValue("Referer");
		if (value != null) {
            URL url;
            try {
                url = URL.parse(value);
            } catch (ParseException ignored) {
                //unreachable
                return null;
            }
            targetRequestInfo.headers.setFieldValue("Referer", url.setOrigin(targetOrigin).toString(targetSSL ? 443 : 80));
        }

        //如果转发ip，则添加X-Forwarded-For字段
        if (forwardIp) {
            targetRequestInfo.headers.setFieldValue("X-Forwarded-For", IP.toString(clientIp));
        }
        //TODO: 处理Cookie
        
        //2. 构造准备发给目标服务器的内容
        HttpRequestSender requestSender = new HttpRequestSender(
                targetRequestInfo,
                requestContent == null ? null : new HttpContentForwardProvider(requestContent)
        );
        
        //3. 尝试连接目标服务器，连不上、发生网络问题则向浏览器返回502

        if (targetSocket == null) {
            try {
                if (targetSSL) {
                    targetSocket = SSLSocketFactory.getDefault().createSocket(targetDomain, targetPort);
                } else {
                    targetSocket = new Socket(targetDomain, targetPort);
                }
                targetOutputStream = targetSocket.getOutputStream();
                targetInputStream = targetSocket.getInputStream();
            }
            catch (IOException e) {
                return new HttpResponseSender(new HttpResponseInfo(502), null);
            }
        }
        
        //4. 向目标服务器发送请求，发生网络问题则向浏览器返回502
        
        try {
            requestSender.send(targetOutputStream);
        }
        catch (IOException e) {
            return new HttpResponseSender(new HttpResponseInfo(502), null);
        }

        //5. 从目标服务器接收Response，发生网络问题、不符合语法则向浏览器返回502

        HttpResponseInfo responseInfo;
        try {
            responseInfo = HttpResponseInfo.fromHttpStream(targetInputStream);
            if (responseInfo == null) {
                return new HttpResponseSender(new HttpResponseInfo(502), null);
            }
        }
        catch (IOException e) {
            return new HttpResponseSender(new HttpResponseInfo(502), null);
        }
        HttpContentReceiver targetContentReceiver = new HttpContentReceiver(targetInputStream, responseInfo.headers);
        if (targetContentReceiver.analyseContent() != HttpContentReceiver.ANALYSE_SUCCESS) {
            return new HttpResponseSender(new HttpResponseInfo(502), null);
        }
        
        //6. 将收到的请求转换为要发浏览器的格式
        
        //TODO: 处理Cookie

        //7. 发送给浏览器

        return new HttpResponseSender(responseInfo, new HttpContentForwardProvider(targetContentReceiver));
    }
}
