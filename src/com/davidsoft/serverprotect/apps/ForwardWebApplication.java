package com.davidsoft.serverprotect.apps;

import com.davidsoft.serverprotect.http.HttpRequestReceiver;
import com.davidsoft.serverprotect.http.HttpResponseInfo;
import com.davidsoft.serverprotect.http.HttpResponseSender;
import com.davidsoft.serverprotect.libs.HttpPath;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;
import com.davidsoft.serverprotect.http.*;
import java.io.*;

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
    protected HttpResponseSender onClientRequest(HttpRequestReceiver requestReceiver, String ip, HttpPath requestRelativePath) {
        //1. 将收到的请求转换为要发给目标服务器的格式

        String origin = com.davidsoft.serverprotect.http.Utils.buildOrigin(targetDomain, targetPort, targetSSL);
		HttpRequestInfo targetRequestInfo = new HttpRequestInfo(requestReceiver.getRequestInfo());
        //更改Domain
		targetRequestInfo.headers.setFieldValue("Domain", com.davidsoft.serverprotect.http.Utils.buildDomain(targetDomain, targetPort, targetSSL));
		//如果有Referer，则更改Referer的协议和域名部分
        String value = targetRequestInfo.headers.getFieldValue("Referer");
		if (value != null) {
            int findPos = value.indexOf("//");
            if (findPos >= 0) {
                findPos = value.indexOf("/", findPos + 2);
                if (findPos == -1) {
                    value = origin;
                }
                else {
                    value = origin + value.substring(findPos);
                }
                targetRequestInfo.headers.setFieldValue("Referer", value);
            }
        }
		//如果有Origin，则更改Origin的协议域名部分
        value = targetRequestInfo.headers.getFieldValue("Origin");
        if (value != null) {
            if (value.endsWith("/")) {
                value = origin + "/";
            }
            else {
                value = origin;
            }
            targetRequestInfo.headers.setFieldValue("Origin", value);
        }
        
        //2. 构造准备发给目标服务器的内容
        HttpRequestSender requestSender = new HttpRequestSender(
            targetRequestInfo,
            null
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
            requestSender.send(targetOutputStream, null);
        }
        catch (IOException e) {
            return new HttpResponseSender(new HttpResponseInfo(502), null);
        }
        
        //5. 从目标服务器接收Response，发生网络问题则向浏览器返回502
        
        HttpResponseReceiver responseReceiver = new HttpResponseReceiver(targetInputStream);
        responseReceiver.receive();

        
        return null;
    }
}
