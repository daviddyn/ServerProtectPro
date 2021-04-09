package com.davidsoft.serverprotect.components;

import com.davidsoft.serverprotect.apps.WebApplicationFactory;
import com.davidsoft.http.*;
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

/**
 * 此类用于处理客户端与服务端的通信。一个此类的实例对应一个浏览器的本服务器的连接。
 */
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

    private final Socket socket;    //连接到客户浏览器的socket
    private final int serverPort;   //当前服务端的端口号
    private final boolean ssl;      //false: http连接; true: https连接
    private final Settings.RuntimeSettings runtimeSettings;     //解析后的服务器配置文件。每个实例维护一个配置文件的引用，因为多个此类的实例将在不同的线程环境中工作，而且配置文件会被实时更改。
    private final RulerNode[] rulers;   //解析后的防火墙规则

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

    /**
     * 当连接池资源已用尽时，向浏览器返回一个503 Service Unavailable(服务器忙)，而后断开连接。
     */
    @Override
    public void runWithoutService() {
        try {
            HttpResponseInfo responseInfo = new HttpResponseInfo(503);
            sendResponse(new HttpResponseSender(responseInfo, null), false, null, socket.getOutputStream());
        } catch (IOException ignored) {}
        com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
    }

    /**
     * 当被催促停止时，将flag置为false，这样在run方法中，处理完本次事务后会断开连接，不再复用连接。
     */
    @Override
    public void onUrge() {
        //处理完本次事务后停止
        flag = false;
    }

    /**
     * 放弃正在执行的事务，立即停止，直接断开连接。
     */
    @Override
    public void onForceStop() {
        //放弃正在执行的事务，立即停止
        flag = false;
        com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
    }

    @Override
    public void run() {
        //获得客户IP、写日志
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
            HttpResponseSender responseSender = null;

            //第一步：等待浏览器发来请求、解析请求

            HttpRequestInfo requestInfo = new HttpRequestInfo();
            int receiveResult;
            try {
                receiveResult = requestInfo.fromHttpStream(in,
                        Settings.getStaticSettings().maxPathLength,
                        Settings.getStaticSettings().maxHeaderSize
                );
            }
            catch (IOException e) {
                //接收请求时发生网络异常则直接断开连接
                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                return;
            }
            switch (receiveResult) {
                case HttpRequestInfo.SUCCESS:
                    //解析成功
                    break;
                case HttpRequestInfo.INVALID_DATA:
                    //如果浏览器发来的内容不符合http语法，则触发了IllegalData规则。
                    switch (runtimeSettings.protections.precautionForIllegalData.method) {
                        case "disabled":
                            //如果IllegalData规则设置为[已禁用]，则向浏览器发送400 Bad Request。
                            responseSender = new HttpResponseSender(new HttpResponseInfo(400), null);
                            break;
                        case "action":
                            //如果IllegalData规则设置为[返回指定内容]，则返回指定内容。
                            responseSender = doPrecaution(runtimeSettings.protections.precautionForIllegalData, false);
                            if (responseSender == null) {
                                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                                return;
                            }
                            break;
                        case "block":
                            //如果IllegalData规则设置为[封禁ip]，则封禁ip。
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
                    //如果浏览器发来的请求头过大(最大字节数在配置文件中定义)，则向浏览器发送413 Request Entity Too Large。
                    responseSender = new HttpResponseSender(new HttpResponseInfo(413), null);
                    break;
                case HttpRequestInfo.PATH_LENGTH_EXCEED:
                    //如果浏览器请求的URL过长(最大字符数在配置文件中定义)，则向浏览器发送414 Request-URI Too Large。
                    responseSender = new HttpResponseSender(new HttpResponseInfo(414), null);
                    break;
            }
            //至此，如果浏览器发来的请求被成功解析，则responseSender应该为null，否则按responseSender中的描述向浏览器返回数据并断开连接。
            if (responseSender != null) {
                try {
                    sendResponse(responseSender, false, null, out);
                } catch (IOException ignored) {}
                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                return;
            }

            //第二步：从请求头中获取一些必要信息

            //如果http版本不是1.1，则向浏览器发送505 HTTP Version not supported，并断开连接。
            if (!"HTTP/1.1".equals(requestInfo.protocolVersion)) {
                try {
                    sendResponse(new HttpResponseSender(new HttpResponseInfo(505), null), false, null, out);
                } catch (IOException ignored) {}
                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                return;
            }

            //通过X-Requested-With字段的情况判断是否为xhr请求。
            boolean xhr = "XMLHttpRequest".equals(requestInfo.headers.getFieldValue("X-Requested-With"));
            //通过Connection字段的情况判断浏览器是否想复用此连接。
            boolean clientWantToKeepConnection = !"close".equals(requestInfo.headers.getFieldValue("Connection"));
            if (!clientWantToKeepConnection) {
                flag = false;
            }
            //通过Accept-Encoding字段的情况判断浏览器想以何种压缩方式返回内容。responseEncoding为null则代表不压缩，返回原始数据。
            String responseEncoding = Utils.analyseQualityValues(requestInfo.headers.getFieldValue("Accept-Encoding"), HttpContentEncodedStreamFactory.getSupportedContentEncodings());

            //第三步：实例化webapp

            //获得当前服务器端口的APP映射表
            Settings.ApplicationMapping applicationMapping = runtimeSettings.appMappings.get(serverPort);
            //根据浏览器发来的URL，从映射表中找到映射的APP
            Settings.WebApplication webApplicationSettings;
            HttpPath appWorkingRootPath;
            PathIndex.QueryResult<Settings.WebApplication> queryResult = applicationMapping.mappedApps.get(requestInfo.path);
            if (queryResult == null) {
                webApplicationSettings = applicationMapping.defaultApp;
                appWorkingRootPath = new HttpPath();
            }
            else {
                webApplicationSettings = queryResult.data;
                appWorkingRootPath = queryResult.matchedPath;
            }
            //如果未找到任何有效APP，则向浏览器返回404。
            if (webApplicationSettings == null) {
                try {
                    sendResponse(new HttpResponseSender(new HttpResponseInfo(404), null), flag && clientWantToKeepConnection, null, out);
                } catch (IOException e) {
                    com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                    return;
                }
                continue;
            }
            //实例化APP
            if (webApplication == null || !webApplication.getName().equals(webApplicationSettings.name)) {
                if (webApplication != null) {
                    webApplication.onDestroy();
                }
                webApplication = WebApplicationFactory.fromSettings(webApplicationSettings, appWorkingRootPath);
                //如果实例化失败，则向浏览器返回500
                if (webApplication == null) {
                    try {
                        sendResponse(new HttpResponseSender(new HttpResponseInfo(500), null), flag && clientWantToKeepConnection, null, out);
                    } catch (IOException e) {
                        com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                        return;
                    }
                    continue;
                }
                webApplication.onCreate();
            }

            //第四步：如果此APP启用了防火墙，则逐一判断规则

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
                    if (ruler.ruler.judge(clientIp, requestInfo)) {
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

            //第五步：分析请求头是否合理

            HttpContentReceiver contentReceiver = null;
            if ("POST".equals(requestInfo.method)) {
                contentReceiver = new HttpContentReceiver(in, requestInfo.headers);
                switch (contentReceiver.analyseContent()) {
                    case HttpContentReceiver.ANALYSE_SUCCESS:
                        responseSender = null;
                        break;
                    case HttpContentReceiver.ANALYSE_UNSUPPORTED_CONTENT_ENCODING:
                        responseSender = new HttpResponseSender(new HttpResponseInfo(501), null);
                        break;
                    case HttpContentReceiver.ANALYSE_CONTENT_LENGTH_REQUIRED:
                        responseSender = new HttpResponseSender(new HttpResponseInfo(411), null);
                        break;
                    case HttpContentReceiver.ANALYSE_MALFORMED_CONTENT_LENGTH:
                        responseSender = new HttpResponseSender(new HttpResponseInfo(400), null);
                        break;
                    default:
                        responseSender = new HttpResponseSender(new HttpResponseInfo(500), null);
                        break;
                }
                if (responseSender != null) {
                    try {
                        sendResponse(responseSender, flag && clientWantToKeepConnection, responseEncoding, out);
                    }
                    catch (IOException e) {
                        com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                        webApplication.onDestroy();
                        return;
                    }
                }
            }

            //第六步：执行webapp，获得正常情况下应当向浏览器返回的内容。

            //调用WebApp的onClientRequest方法获得返回内容
            responseSender = webApplication.onClientRequest(requestInfo, contentReceiver, clientIp, serverPort, ssl);
            //如果onClientRequest返回null则直接断开连接
            if (responseSender == null) {
                com.davidsoft.serverprotect.Utils.closeWithoutException(socket, true);
                webApplication.onDestroy();
                return;
            }
            //将返回内容发送给浏览器
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
}
