package com.davidsoft.serverprotect.apps;

import com.davidsoft.net.*;
import com.davidsoft.net.http.*;
import com.davidsoft.serverprotect.Utils;
import com.davidsoft.serverprotect.components.Log;
import com.davidsoft.url.URI;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

        //更改请求地址
        targetRequestInfo.uri = requestRelativeURI;
        //更改Host
		targetRequestInfo.headers.setFieldValue("Host", targetOrigin.getHost().toString(false, targetSSL ? 443 : 80));
        //如果有Origin，则更改Origin
        if (targetRequestInfo.headers.containsField("Origin")) {
            targetRequestInfo.headers.setFieldValue("Origin", targetOrigin.toString(targetSSL ? 443 : 80));
        }
		//如果有Referer，则更改Referer
        String value = targetRequestInfo.headers.getFieldValue("Referer");
		if (value != null) {
            URL refererUrl;
            try {
                refererUrl = URL.parse(value);
            } catch (ParseException ignored) {
                //unreachable
                return null;
            }
            targetRequestInfo.headers.setFieldValue(
                    "Referer",
                    new URL(targetOrigin, refererUrl.getUri().subURI(getWorkingRootURI().patternCount(), refererUrl.getUri().patternCount())).toString(targetSSL ? 443 : 80)
            );
        }
        //如果转发ip，则添加X-Forwarded-For字段
        if (forwardIp) {
            targetRequestInfo.headers.setFieldValue("X-Forwarded-For", IP.toString(clientIp));
        }
        //TODO: 处理Cookie
        
        //2. 构造准备发给目标服务器的内容
        HttpRequestSender requestSender = new HttpRequestForwardSender(targetRequestInfo, requestContent);

        HttpResponseInfo responseInfo;
        HttpContentReceiver targetContentReceiver;
        boolean retrying = false;

        while (true) {

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
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.logRequest(Log.LOG_ERROR, "转发器", "接收：" + requestInfo.toAbstractString() + "；因连不上目标服务器而失败。");
                    return new HttpResponseSender(new HttpResponseInfo(502), null);
                }
            }

            //4. 向目标服务器发送请求，发生网络问题则向浏览器返回502

            try {
                requestSender.send(targetOutputStream);
            } catch (IOException e) {
                if (retrying) {
                    e.printStackTrace();
                    Log.logRequest(Log.LOG_ERROR, "转发器", "接收：" + requestInfo.toAbstractString() + "；因向目标服务器发送数据时发生IO异常而失败。");
                    return new HttpResponseSender(new HttpResponseInfo(502), null);
                }
                else {
                    retrying = true;
                    Utils.closeWithoutException(targetSocket);
                    targetSocket = null;
                    continue;
                }
            }

            //5. 从目标服务器接收Response，发生网络问题、不符合语法则向浏览器返回502

            try {
                responseInfo = HttpResponseInfo.fromHttpStream(targetInputStream);
                if (responseInfo == null) {
                    if (retrying) {
                        Log.logRequest(Log.LOG_ERROR, "转发器", "接收：" + requestInfo.toAbstractString() + "；因目标服务器返回的Header不正确而失败。");
                        return new HttpResponseSender(new HttpResponseInfo(502), null);
                    }
                    else {
                        retrying = true;
                        Utils.closeWithoutException(targetSocket);
                        targetSocket = null;
                        continue;
                    }
                }
            } catch (IOException e) {
                if (retrying) {
                    e.printStackTrace();
                    Log.logRequest(Log.LOG_ERROR, "转发器", "接收：" + requestInfo.toAbstractString() + "；因从目标服务器接收数据时发生IO异常而失败。");
                    return new HttpResponseSender(new HttpResponseInfo(502), null);
                }
                else {
                    retrying = true;
                    Utils.closeWithoutException(targetSocket);
                    targetSocket = null;
                    continue;
                }
            }
            targetContentReceiver = new HttpContentReceiver(targetInputStream, responseInfo.responseCode, responseInfo.headers);
            if (targetContentReceiver.analyseContent() != HttpContentReceiver.ANALYSE_SUCCESS) {
                Log.logRequest(Log.LOG_ERROR, "转发器", "接收：" + requestInfo.toAbstractString() + "；因目标服务器返回的Content不正确而失败。");
                Log.logRequest(Log.LOG_ERROR, "转发器", responseInfo.toString());
                return new HttpResponseSender(new HttpResponseInfo(502), null);
            }

            break;
        }
        
        //6. 将收到的请求转换为要发浏览器的格式
        if (responseInfo.headers.containsField("Access-Control-Allow-Origin")) {
            responseInfo.headers.setFieldValue("Access-Control-Allow-Origin", (targetSSL ? "http://" : "https+//") + requestInfo.headers.getFieldValue("host"));
        }
        
        //TODO: 处理Cookie

        //7. 发送给浏览器
        Log.logRequest(Log.LOG_INFO, "转发器", "接收：" + requestInfo.toAbstractString() + "；返回：" + responseInfo.toAbstractString());

        return new HttpResponseForwardSender(responseInfo, targetContentReceiver);
    }

    @Override
    protected HttpResponseSender onGetFavicon(HttpRequestInfo requestInfo, HttpContentReceiver requestContent, int clientIp, URI requestRelativeURI) {
        return onClientRequest(requestInfo, requestContent, clientIp, requestRelativeURI);
    }

    private static class HttpRequestForwardSender extends HttpRequestSender {

        private final HttpContentReceiver contentReceiver;

        public HttpRequestForwardSender(HttpRequestInfo requestInfo, HttpContentReceiver contentReceiver) {
            super(requestInfo, null);
            this.contentReceiver = contentReceiver;
        }

        @Override
        public void send(OutputStream out) throws IOException {
            requestInfo.toHttpStream(out);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            if (contentReceiver != null && contentReceiver.hasContent()) {
                contentReceiver.getContentRawInputStream().transferTo(out);
                if (contentReceiver.hasSuspendedHeaders()) {
                    contentReceiver.receiveSuspendedHeaders();
                    contentReceiver.getSuspendedHeaders().toRequestStream(out);
                    out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    private static class HttpResponseForwardSender extends HttpResponseSender {

        private final HttpContentReceiver contentReceiver;

        public HttpResponseForwardSender(HttpResponseInfo responseInfo, HttpContentReceiver contentReceiver) {
            super(responseInfo, null);
            this.contentReceiver = contentReceiver;
        }

        @Override
        public void send(OutputStream out, String contentEncoding) throws IOException {
            responseInfo.toHttpStream(out);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            if (contentReceiver != null && contentReceiver.hasContent()) {
                contentReceiver.getContentRawInputStream().transferTo(out);
                if (contentReceiver.hasSuspendedHeaders()) {
                    contentReceiver.receiveSuspendedHeaders();
                    contentReceiver.getSuspendedHeaders().toResponseStream(out);
                    out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }
}
