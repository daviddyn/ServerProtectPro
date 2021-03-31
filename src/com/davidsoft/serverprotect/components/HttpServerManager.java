package com.davidsoft.serverprotect.components;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class HttpServerManager {

    private static final String LOG_CATEGORY = "Http服务器";

    private static final HashMap<Integer, ServerThread> serverThreads = new HashMap<>();

    private static ServerSocket createServerSocket(int port, boolean ssl) {
        if (ssl) {
            //TODO: 增加SSL ServerSocket的支持
            return null;
        }
        else {
            try {
                return new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static boolean startUp() {
        Settings.RuntimeSettings runtimeSettings = Settings.getRuntimeSettings();

        for (Map.Entry<Integer, Settings.ApplicationMapping> entry : runtimeSettings.appMappings.entrySet()) {
            ServerSocket serverSocket = createServerSocket(entry.getKey(), entry.getValue().ssl);
            if (serverSocket == null) {
                if (entry.getValue().ssl) {
                    Log.logMain(Log.LOG_ERROR, LOG_CATEGORY, "无法在" + entry.getKey() + "端口上建立SSL服务器。");
                }
                else {
                    Log.logMain(Log.LOG_ERROR, LOG_CATEGORY, "无法在" + entry.getKey() + "端口上建立服务器。");
                }
                return false;
            }
            //创建线程，但不启动
            serverThreads.put(entry.getKey(), new ServerThread(entry.getKey(), entry.getValue().ssl, serverSocket));
        }
        for (ServerThread thread : serverThreads.values()) {
            if (thread.ssl) {
                Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已在" + thread.port + "端口上建立SSL服务器。");
            }
            else {
                Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已在" + thread.port + "端口上建立服务器。");
            }
            thread.start();
        }
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "Http服务器启动成功！");
        return true;
    }

    public static void remap() {
        Settings.RuntimeSettings runtimeSettings = Settings.getRuntimeSettings();

        //1. 停止ports中不存在的服务
        Iterator<ServerThread> iterator = serverThreads.values().iterator();
        while (iterator.hasNext()) {
            ServerThread thread = iterator.next();
            if (!runtimeSettings.appMappings.containsKey(thread.port)) {
                thread.shutDown();
                if (thread.ssl) {
                    Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已停止" + thread.port + "端口上的SSL服务器。");
                }
                else {
                    Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已停止" + thread.port + "端口上的服务器。");
                }
                iterator.remove();
            }
        }

        //2. 应用新ports
        for (Map.Entry<Integer, Settings.ApplicationMapping> entry : runtimeSettings.appMappings.entrySet()) {
            ServerThread thread = serverThreads.get(entry.getKey());
            if (thread != null && thread.ssl != entry.getValue().ssl) {
                thread.shutDown();
                if (thread.ssl) {
                    Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已停止" + thread.port + "端口上的服务器。");
                }
                else {
                    Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已停止" + thread.port + "端口上的SSL服务器。");
                }
                thread = null;
            }
            if (thread == null) {
                ServerSocket serverSocket = createServerSocket(entry.getKey(), entry.getValue().ssl);
                if (serverSocket == null) {
                    if (entry.getValue().ssl) {
                        Log.logMain(Log.LOG_ERROR, LOG_CATEGORY, "无法在" + entry.getKey() + "端口上建立SSL服务器。");
                    }
                    else {
                        Log.logMain(Log.LOG_ERROR, LOG_CATEGORY, "无法在" + entry.getKey() + "端口上建立服务器。");
                    }
                }
                else {
                    thread = new ServerThread(entry.getKey(), entry.getValue().ssl, serverSocket);
                    serverThreads.put(entry.getKey(), thread);
                    thread.start();
                    if (thread.ssl) {
                        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已在" + thread.port + "端口上建立SSL服务器。");
                    }
                    else {
                        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已在" + thread.port + "端口上建立服务器。");
                    }
                }
            }
        }
    }

    public static void shutDown() {
        //停止所有服务器
        for (ServerThread thread : serverThreads.values()) {
            thread.shutDown();
            try {
                thread.join();
            } catch (InterruptedException ignored) {}
            if (thread.ssl) {
                Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已停止" + thread.port + "端口上的SSL服务器。");
            }
            else {
                Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已停止" + thread.port + "端口上的服务器。");
            }
        }
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "Http服务器已停止。");
    }
}