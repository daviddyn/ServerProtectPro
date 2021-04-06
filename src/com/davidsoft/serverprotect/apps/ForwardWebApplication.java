package com.davidsoft.serverprotect.apps;

import com.davidsoft.serverprotect.http.HttpRequestReceiver;
import com.davidsoft.serverprotect.http.HttpResponseInfo;
import com.davidsoft.serverprotect.http.HttpResponseSender;
import com.davidsoft.serverprotect.libs.HttpPath;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;

public class ForwardWebApplication extends BaseWebApplication {

    private final String targetDomain;
    private final int targetPort;
    private final boolean targetSSL;
    private final boolean forwardIp;

    private Socket targetSocket;

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
        //1. 尝试连接目标服务器，连不上则向浏览器返回502

        if (targetSocket == null) {
            try {
                if (targetSSL) {
                    targetSocket = SSLSocketFactory.getDefault().createSocket(targetDomain, targetPort);
                } else {
                    targetSocket = new Socket(targetDomain, targetPort);
                }
            }
            catch (IOException e) {
                return new HttpResponseSender(new HttpResponseInfo(502), null);
            }
        }

        //2. 将收到的请求转换为要发给目标服务器的格式

        //更改Domain

        //requestReceiver.getRequestInfo().
        //requestReceiver.getContentCharset()
        return null;
    }
}