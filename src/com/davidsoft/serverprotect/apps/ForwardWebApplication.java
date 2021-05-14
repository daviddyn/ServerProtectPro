package com.davidsoft.serverprotect.apps;

import com.davidsoft.net.*;
import com.davidsoft.net.http.*;
import com.davidsoft.serverprotect.Utils;
import com.davidsoft.serverprotect.components.Log;
import com.davidsoft.serverprotect.components.Program;
import com.davidsoft.url.URI;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

public class ForwardWebApplication extends BaseWebApplication {

    private final String targetDomain;
    private final int targetPort;
    private final boolean targetSSL;
    private final boolean forwardIp;

    private Socket targetSocket;
    private OutputStream targetOutputStream;
    private InputStream targetInputStream;
    private ThreadedHttpResponseReceiver threadedHttpResponseReceiver;

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
                    threadedHttpResponseReceiver = new ThreadedHttpResponseReceiver(targetInputStream);
                    threadedHttpResponseReceiver.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    return new HttpResponseSender(new HttpResponseInfo(502), null);
                }
            }
            else {
                threadedHttpResponseReceiver.continueDetect();
            }

            //4. 向目标服务器发送请求，发生网络问题则向浏览器返回502

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (threadedHttpResponseReceiver.isClosed()) {
                Program.logMain(Log.LOG_ERROR, "Debug", "已检测到关闭");
                Utils.closeWithoutException(targetSocket);
                targetSocket = null;
                continue;
            }
            else {
                Program.logMain(Log.LOG_ERROR, "Debug", "未测到关闭！！！");
            }

            try {
                requestSender.send(targetOutputStream);
            } catch (IOException e) {
                Utils.closeWithoutException(targetSocket);
                threadedHttpResponseReceiver.interrupt();
                targetSocket = null;
                if (retrying) {
                    e.printStackTrace();
                    return new HttpResponseSender(new HttpResponseInfo(502), null);
                }
                else {
                    retrying = true;
                    continue;
                }
            }

            //5. 从目标服务器接收Response，发生网络问题、不符合语法则向浏览器返回502

            if (threadedHttpResponseReceiver.isClosed()) {
                return new HttpResponseSender(new HttpResponseInfo(502), null);
            }
            responseInfo = threadedHttpResponseReceiver.getResponseInfo();
            if (responseInfo == null) {
                Utils.closeWithoutException(targetSocket);
                threadedHttpResponseReceiver.interrupt();
                targetSocket = null;
                return new HttpResponseSender(new HttpResponseInfo(502), null);
            }
            targetContentReceiver = new HttpContentReceiver(targetInputStream, responseInfo.responseCode, responseInfo.headers);
            if (targetContentReceiver.analyseContent() != HttpContentReceiver.ANALYSE_SUCCESS) {
                Utils.closeWithoutException(targetSocket);
                threadedHttpResponseReceiver.interrupt();
                targetSocket = null;
                return new HttpResponseSender(new HttpResponseInfo(502), null);
            }

            break;
        }
        
        //6. 将收到的请求转换为要发浏览器的格式

        Origin clientOrigin = new Origin(targetSSL ? "http://" : "https://", getSelfHost());
        if (responseInfo.headers.containsField("Access-Control-Allow-Origin")) {
            responseInfo.headers.setFieldValue("Access-Control-Allow-Origin", clientOrigin.toString());
        }
        String redirectLocation = requestInfo.headers.getFieldValue("Location");
        if (redirectLocation != null) {
            URL redirectUrl = null;
            try {
                redirectUrl = URL.parse(redirectLocation);
            } catch (ParseException ignored) {
                //默认服务器不会返回无效的url
            }
            if (targetOrigin.equals(redirectUrl.getOrigin())) {
                requestInfo.headers.setFieldValue("Location", new URL(clientOrigin, getWorkingRootURI().getInto(redirectUrl.getUri())).toString());
            }
        }
        //TODO: 处理Cookie
        for (Map.Entry<String, String> entry : responseInfo.headers.cookies.entrySet()) {
            String[] patterns = entry.getValue().split("; ");
            boolean needProcess = false;
            for (int i = 0; i < patterns.length; i++) {
                if (patterns[i].startsWith("domain=") && UrlCodec.urlDecodeString(patterns[i].substring(7), StandardCharsets.UTF_8).equals(targetDomain)) {
                    needProcess = true;
                    patterns[i] = "domain=" + targetDomain;
                }
                else if (!getWorkingRootURI().locationIsRoot() && patterns[i].startsWith("path=")) {
                    needProcess = true;
                    patterns[i] = "path=" + NetURI.toString(getWorkingRootURI()) + patterns[i].substring(6);
                }
            }
            if (needProcess) {
                entry.setValue(String.join("; ", patterns));
            }
        }

        //7. 发送给浏览器
        return new HttpResponseForwardSender(responseInfo, targetContentReceiver);
    }

    @Override
    protected HttpResponseSender onGetFavicon(HttpRequestInfo requestInfo, HttpContentReceiver requestContent, int clientIp, URI requestRelativeURI) {
        return onClientRequest(requestInfo, requestContent, clientIp, requestRelativeURI);
    }

    @Override
    public void onDestroy() {
        if (targetSocket != null) {
            Utils.closeWithoutException(targetSocket);
            threadedHttpResponseReceiver.interrupt();
        }
        super.onDestroy();
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

    private static final class ThreadedHttpResponseReceiver extends Thread {
        private final InputStream in;
        private final Semaphore semaphore;
        private final Semaphore waitSemaphore;
        private HttpResponseInfo responseInfo;
        private boolean closed;

        private ThreadedHttpResponseReceiver(InputStream in) {
            this.in = in;
            semaphore = new Semaphore(0);
            waitSemaphore = new Semaphore(0);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    responseInfo = HttpResponseInfo.fromHttpStream(in);
                } catch (IOException e) {
                    closed = true;
                    responseInfo = null;
                    semaphore.release();
                    return;
                }
                semaphore.release();
                try {
                    waitSemaphore.acquire();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        public HttpResponseInfo getResponseInfo() {
            try {
                semaphore.acquire();
            } catch (InterruptedException ignored) {}
            return responseInfo;
        }

        public void continueDetect() {
            waitSemaphore.release();
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
