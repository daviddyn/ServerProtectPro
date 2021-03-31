package com.davidsoft.serverprotect.components;

import com.davidsoft.serverprotect.apps.WebApplicationFactory;
import com.davidsoft.serverprotect.http.*;
import com.davidsoft.serverprotect.libs.HttpPath;
import com.davidsoft.serverprotect.libs.PathIndex;
import com.davidsoft.serverprotect.libs.PooledRunnable;
import com.davidsoft.serverprotect.rulers.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ClientConnection implements PooledRunnable {

    private static final class RulerNode {
        private final Ruler ruler;
        private final boolean block;
        private final Settings.Precaution precaution;
        private final int responseCode;

        private RulerNode(Ruler ruler, boolean block, Settings.Precaution precaution, int responseCode) {
            this.ruler = ruler;
            this.block = block;
            this.precaution = precaution;
            this.responseCode = responseCode;
        }
    }

    private final Socket socket;
    private final int serverPort;
    private final boolean ssl;
    private final Settings.RuntimeSettings runtimeSettings;
    private final RulerNode[] rulers;

    private boolean flag;

    public ClientConnection(Socket socket, int serverPort, boolean ssl) {
        this.socket = socket;
        this.serverPort = serverPort;
        this.ssl = ssl;
        runtimeSettings = Settings.getRuntimeSettings();

        //从设置中创建ruler
        ArrayList<RulerNode> rulerBuilder = new ArrayList<>(6);
        switch (runtimeSettings.protections.precautionForIllegalFrequency.method) {
            case "block":
                rulerBuilder.add(new RulerNode(new FrequencyRuler(), true, runtimeSettings.protections.precautionForIllegalFrequency, 0));
                break;
            case "action":
                rulerBuilder.add(new RulerNode(new FrequencyRuler(), false, runtimeSettings.protections.precautionForIllegalFrequency, 0));
                break;
        }
        switch (runtimeSettings.protections.precautionForIllegalForward.method) {
            case "block":
                rulerBuilder.add(new RulerNode(new ForwardRuler(), true, runtimeSettings.protections.precautionForIllegalForward, 0));
                break;
            case "action":
                rulerBuilder.add(new RulerNode(new ForwardRuler(), false, runtimeSettings.protections.precautionForIllegalForward, 0));
                break;
        }
        switch (runtimeSettings.protections.precautionForIllegalMethod.method) {
            case "block":
                rulerBuilder.add(new RulerNode(new MethodRuler(), true, runtimeSettings.protections.precautionForIllegalMethod, 0));
                break;
            case "action":
                rulerBuilder.add(new RulerNode(new MethodRuler(), false, runtimeSettings.protections.precautionForIllegalMethod, 0));
                break;
        }
        switch (runtimeSettings.protections.precautionForIllegalAgent.method) {
            case "block":
                rulerBuilder.add(new RulerNode(new AgentRuler(), true, runtimeSettings.protections.precautionForIllegalAgent, 0));
                break;
            case "action":
                rulerBuilder.add(new RulerNode(new AgentRuler(), false, runtimeSettings.protections.precautionForIllegalAgent, 0));
                break;
            default:
                rulerBuilder.add(new RulerNode(new AgentRuler(), false, null, 412));
                break;
        }
        switch (runtimeSettings.protections.precautionForIllegalRedirect.method) {
            case "block":
                rulerBuilder.add(new RulerNode(new RedirectRuler(), true, runtimeSettings.protections.precautionForIllegalRedirect, 0));
                break;
            case "action":
                rulerBuilder.add(new RulerNode(new RedirectRuler(), false, runtimeSettings.protections.precautionForIllegalRedirect, 0));
                break;
            default:
                rulerBuilder.add(new RulerNode(new RedirectRuler(), false, null, 412));
                break;
        }
        switch (runtimeSettings.protections.precautionForIllegalTrace.method) {
            case "block":
                rulerBuilder.add(new RulerNode(new TraceRuler(), true, runtimeSettings.protections.precautionForIllegalTrace, 0));
                break;
            case "action":
                rulerBuilder.add(new RulerNode(new TraceRuler(), false, runtimeSettings.protections.precautionForIllegalTrace, 0));
                break;
            default:
                rulerBuilder.add(new RulerNode(new TraceRuler(), false, null, 412));
                break;
        }
        rulers = new RulerNode[rulerBuilder.size()];
        rulerBuilder.toArray(rulers);

        flag = true;
    }

    @Override
    public void runWithoutService() {
        try {
            HttpResponseInfo responseInfo = new HttpResponseInfo(503);
            sendResponse(new HttpResponseSender(responseInfo, null), false, null, socket.getOutputStream());
        } catch (IOException ignored) {}
        com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
    }

    @Override
    public void onUrge() {
        //处理完本次事务后停止
        flag = false;
    }

    @Override
    public void onForceStop() {
        //放弃正在执行的事务，立即停止
        flag = false;
        com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
    }

    @Override
    public void run() {
        //获得客户IP
        String clientIp = socket.getInetAddress().getHostAddress();
        Program.logRequest(Log.LOG_INFO, socket.getRemoteSocketAddress().toString(), "已连接");
        runInner(clientIp);
        Program.logRequest(Log.LOG_INFO, socket.getRemoteSocketAddress().toString(), "已断开");
    }

    private void runInner(String clientIp) {
        InputStream in;
        OutputStream out;
        WebApplication webApplication = null;

        //获得输入输出流
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
            return;
        }

        //连接复用的处理循环
        do {
            //解析请求
            HttpResponseSender responseSender = null;
            HttpRequestReceiver httpRequestReceiver = new HttpRequestReceiver(in);
            int receiveState;
            try {
                receiveState = httpRequestReceiver.receive(
                        Settings.getStaticSettings().maxPathLength,
                        Settings.getStaticSettings().maxHeaderSize
                );
            }
            catch (IOException e) {
                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                return;
            }
            switch (receiveState) {
                case HttpRequestInfo.SUCCESS:
                    break;
                case HttpRequestInfo.INVALID_DATA:
                    switch (runtimeSettings.protections.precautionForIllegalData.method) {
                        case "disabled":
                            responseSender = new HttpResponseSender(new HttpResponseInfo(400), null);
                            break;
                        case "action":
                            responseSender = doPrecaution(runtimeSettings.protections.precautionForIllegalData, false);
                            if (responseSender == null) {
                                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                                return;
                            }
                            break;
                        case "block":
                            Program.addBlackList(clientIp, System.currentTimeMillis() + runtimeSettings.protections.precautionForIllegalData.blockLengthInMinute * 60000);
                            responseSender = doPrecautionForBlock(false);
                            if (responseSender == null) {
                                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                                return;
                            }
                            break;
                    }
                    break;
                case HttpRequestInfo.HEADER_SIZE_EXCEED:
                    responseSender = new HttpResponseSender(new HttpResponseInfo(413), null);
                    break;
                case HttpRequestInfo.PATH_LENGTH_EXCEED:
                    responseSender = new HttpResponseSender(new HttpResponseInfo(414), null);
                    break;
            }
            if (responseSender != null) {
                try {
                    sendResponse(responseSender, false, null, out);
                } catch (IOException ignored) {}
                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                return;
            }

            //判断HTTP版本
            if (!"HTTP/1.1".equals(httpRequestReceiver.getRequestInfo().protocolVersion)) {
                try {
                    sendResponse(new HttpResponseSender(new HttpResponseInfo(505), null), true, null, out);
                } catch (IOException e) {
                    com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                    return;
                }
                continue;
            }

            //从请求头中获取一些信息
            boolean xhr = "XMLHttpRequest".equals(httpRequestReceiver.getRequestInfo().headers.getFieldValue("X-Requested-With"));
            boolean clientWantToKeepConnection = !"close".equals(httpRequestReceiver.getRequestInfo().headers.getFieldValue("Connection"));
            if (!clientWantToKeepConnection) {
                flag = false;
            }
            String responseEncoding = Utils.analyseQualityValues(httpRequestReceiver.getRequestInfo().headers.getFieldValue("Accept-Encoding"), HttpContentEncodedStreamFactory.getSupportedContentEncodings());
            //String responseEncoding = null;

            //实例化webapp
            Settings.ApplicationMapping applicationMapping = runtimeSettings.appMappings.get(serverPort);
            PathIndex.QueryResult<Settings.WebApplication> queryResult = applicationMapping.mappedApps.get(httpRequestReceiver.getRequestInfo().path);
            Settings.WebApplication webApplicationSettings;
            HttpPath appWorkingRootPath;
            if (queryResult == null) {
                webApplicationSettings = applicationMapping.defaultApp;
                appWorkingRootPath = new HttpPath();
            }
            else {
                webApplicationSettings = queryResult.data;
                appWorkingRootPath = queryResult.matchedPath;
            }
            if (webApplication == null || !webApplication.getName().equals(webApplicationSettings.name)) {
                if (webApplication != null) {
                    webApplication.onDestroy();
                }
                webApplication = WebApplicationFactory.fromSettings(webApplicationSettings, appWorkingRootPath);
                webApplication.onCreate();
            }

            //保护机制
            if (webApplication.isProtectEnabled()) {

                //判断ip封禁
                if (BlackListManager.inBlackList(clientIp)) {
                    responseSender = doPrecautionForBlock(xhr);
                    if (responseSender != null) {
                        try {
                            sendResponse(responseSender, false, responseEncoding, out);
                        } catch (IOException ignored) {}
                    }
                    com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                    webApplication.onDestroy();
                    return;
                }

                //其他规则
                for (RulerNode ruler : rulers) {
                    if (ruler.ruler.judge(clientIp, httpRequestReceiver.getRequestInfo())) {
                        continue;
                    }
                    if (ruler.block) {
                        Program.addBlackList(clientIp, System.currentTimeMillis() + ruler.precaution.blockLengthInMinute * 60000);
                        responseSender = doPrecautionForBlock(xhr);
                        if (responseSender != null) {
                            try {
                                sendResponse(responseSender, false, responseEncoding, out);
                            } catch (IOException ignored) {}
                        }
                        com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                        webApplication.onDestroy();
                        return;
                    }
                    if (ruler.precaution == null) {
                        try {
                            sendResponse(new HttpResponseSender(new HttpResponseInfo(ruler.responseCode), null), flag && clientWantToKeepConnection, responseEncoding, out);
                        } catch (IOException ignored) {
                            com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                            webApplication.onDestroy();
                            return;
                        }
                        break;
                    }
                    else {
                        responseSender = doPrecaution(ruler.precaution, xhr);
                        if (responseSender != null) {
                            try {
                                sendResponse(responseSender, false, responseEncoding, out);
                            } catch (IOException ignored) {}
                        }
                        com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                        webApplication.onDestroy();
                        return;
                    }
                }
            }

            //执行webapp
            responseSender = webApplication.onClientRequest(httpRequestReceiver, clientIp, serverPort, ssl);
            if (responseSender == null) {
                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                webApplication.onDestroy();
                return;
            }
            try {
                sendResponse(responseSender, flag && clientWantToKeepConnection && !"close".equals(responseSender.responseInfo.headers.getFieldValue("Connection")), responseEncoding, out);
            } catch (IOException ignored) {
                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                webApplication.onDestroy();
                return;
            }

        } while (flag);

        //收拾烂摊子
        if (webApplication != null) {
            webApplication.onDestroy();
        }
        com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
    }

    private HttpResponseSender doPrecautionForBlock(boolean xhr) {
        if ("disabled".equals(runtimeSettings.protections.precautionForBlackList.method)) {
            return new HttpResponseSender(new HttpResponseInfo("HTTP/1.1", 233, "You Are Detected"), null);
        }
        return doPrecaution(runtimeSettings.protections.precautionForBlackList, xhr);
    }

    private HttpResponseSender doPrecaution(Settings.Precaution precaution, boolean xhr) {
        Settings.Action action = xhr ? precaution.xhrAction : precaution.action;
        if ("shutdown".equals(action.actionType)) {
            return null;
        }
        Settings.ActionContent content = action.actionContent;
        if ("file".equals(content.type)) {
            HttpContentFileProvider provider;
            try {
                provider = new HttpContentFileProvider(new File(content.content), null);
            } catch (IOException e) {
                e.printStackTrace();
                return new HttpResponseSender(new HttpResponseInfo(500), null);
            }
            return new HttpResponseSender(new HttpResponseInfo(200), provider);
        }
        else {
            return new HttpResponseSender(
                    new HttpResponseInfo(200),
                    new HttpContentStringProvider(content.content, StandardCharsets.UTF_8, content.mime, StandardCharsets.UTF_8)
            );
        }
    }

    private void sendResponse(HttpResponseSender responseSender, boolean keepConnection, String contentEncoding, OutputStream out) throws IOException {
        //添油加醋
        responseSender.responseInfo.headers.setFieldValue("Server", "DSSPPro/2.0");
        if (keepConnection) {
            responseSender.responseInfo.headers.setFieldValue("Connection", "keep-alive");
        }
        else {
            responseSender.responseInfo.headers.setFieldValue("Connection", "close");
        }
        responseSender.send(out, contentEncoding);
    }
}
