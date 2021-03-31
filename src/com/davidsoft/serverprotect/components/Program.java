package com.davidsoft.serverprotect.components;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public final class Program {

    private static final String LOG_CATEGORY = "主程序";

    private static final int MESSAGE_SHUTDOWN = 0;
    private static final int MESSAGE_LOG_MAIN = 1;
    private static final int MESSAGE_LOG_REQUEST = 2;
    private static final int MESSAGE_APPLY_SETTINGS = 3;
    private static final int MESSAGE_FLUSH_CONFIGS = 4;
    private static final int MESSAGE_FLUSH_BLACKLIST = 5;
    private static final int MESSAGE_ADD_BLACKLIST = 6;
    private static final int MESSAGE_ADD_BLACKLIST_SYNC = 7;
    private static final int MESSAGE_REMOVE_BLACKLIST = 8;
    private static final int MESSAGE_CLEAN_BLACKLIST = 9;

    private static final class MessageItem {
        private int what;
        private int intArg1;
        private int intArg2;
        private long resultArg;
        private Object objArg1;
        private Object objArg2;

        private Semaphore semaphore;
    }

    private static final LinkedBlockingQueue<MessageItem> messageQueue = new LinkedBlockingQueue<>();

    public static void shutDown() {
        MessageItem messageItem = new MessageItem();
        messageItem.what = MESSAGE_SHUTDOWN;
        messageQueue.offer(messageItem);
    }

    public static void logMain(int logType, String category, String content) {
        MessageItem messageItem = new MessageItem();
        messageItem.what = MESSAGE_LOG_MAIN;
        messageItem.intArg1 = logType;
        messageItem.objArg1 = category;
        messageItem.objArg2 = content;
        messageQueue.offer(messageItem);
    }

    public static void logRequest(int logType, String category, String content) {
        MessageItem messageItem = new MessageItem();
        messageItem.what = MESSAGE_LOG_REQUEST;
        messageItem.intArg1 = logType;
        messageItem.objArg1 = category;
        messageItem.objArg2 = content;
        messageQueue.offer(messageItem);
    }

    public static void applyNewSettings(Settings.RuntimeSettings runtimeSettings) {
        MessageItem messageItem = new MessageItem();
        messageItem.what = MESSAGE_APPLY_SETTINGS;
        messageItem.objArg1 = runtimeSettings;
        messageQueue.offer(messageItem);
    }

    public static void flushConfigs() {
        MessageItem messageItem = new MessageItem();
        messageItem.what = MESSAGE_FLUSH_CONFIGS;
        messageQueue.offer(messageItem);
    }

    public static void flushBlacklist() {
        MessageItem messageItem = new MessageItem();
        messageItem.what = MESSAGE_FLUSH_BLACKLIST;
        messageQueue.offer(messageItem);
    }

    public static void addBlackList(String ipWithRegex, long expires) {
        MessageItem messageItem = new MessageItem();
        messageItem.what = MESSAGE_ADD_BLACKLIST;
        messageItem.objArg1 = ipWithRegex;
        messageItem.intArg1 = (int) ((expires >> 32) & 0x00000000FFFFFFFFL);
        messageItem.intArg2 = (int) (expires & 0x00000000FFFFFFFFL);
        messageQueue.offer(messageItem);
    }

    public static long addBlackListSync(String ipWithRegex, long expires) {
        MessageItem messageItem = new MessageItem();
        messageItem.what = MESSAGE_ADD_BLACKLIST_SYNC;
        messageItem.semaphore = new Semaphore(0);
        messageItem.objArg1 = ipWithRegex;
        messageItem.intArg1 = (int) ((expires >> 32) & 0x00000000FFFFFFFFL);
        messageItem.intArg2 = (int) (expires & 0x00000000FFFFFFFFL);
        messageQueue.offer(messageItem);
        try {
            messageItem.semaphore.acquire();
        } catch (InterruptedException e) {
            return 0;
        }
        return messageItem.resultArg;
    }

    public static void removeBlackList(String ipWithRegex) {
        MessageItem messageItem = new MessageItem();
        messageItem.what = MESSAGE_REMOVE_BLACKLIST;
        messageItem.objArg1 = ipWithRegex;
        messageQueue.offer(messageItem);
    }

    public static void cleanBlackList() {
        MessageItem messageItem = new MessageItem();
        messageItem.what = MESSAGE_CLEAN_BLACKLIST;
        messageQueue.offer(messageItem);
    }

    public static void run() {

        //该启动的启动

        if (!CommandReceiver.startUp()) {
            Log.logMain(Log.LOG_ERROR, LOG_CATEGORY, "程序无法启动，因为无法初始化命令控制器。");
            return;
        }
        if (!Settings.initSettings()) {
            Log.logMain(Log.LOG_ERROR, LOG_CATEGORY, "程序无法启动，因为无法加载设置。");
            CommandReceiver.shutDown();
            return;
        }
        BlackListManager.initBlackList();
        if (!TraceManager.startUp()) {
            Log.logMain(Log.LOG_ERROR, LOG_CATEGORY, "程序无法启动，因为无法初始化访问路径维护模块。");
            CommandReceiver.shutDown();
            return;
        }
        FrequencyManager.initManager();
        ConnectionPool.startUp();
        if (!HttpServerManager.startUp()) {
            Log.logMain(Log.LOG_ERROR, LOG_CATEGORY, "程序无法启动，因为无法初始化Http服务器。");
            ConnectionPool.shutDown();
            TraceManager.shutDown();
            CommandReceiver.shutDown();
            return;
        }
        Log.startUp();

        //进入消息循环

        MessageItem messageItem;
        while (true) {
            try {
                messageItem = messageQueue.take();
            } catch (InterruptedException ignored) {
                //注意：主线程不能interrupt!
                return;
            }
            switch (messageItem.what) {
                case MESSAGE_SHUTDOWN:
                    break;
                case MESSAGE_LOG_MAIN:
                    Log.logMain(messageItem.intArg1, (String) messageItem.objArg1, (String) messageItem.objArg2);
                    continue;
                case MESSAGE_LOG_REQUEST:
                    Log.logRequest(messageItem.intArg1, (String) messageItem.objArg1, (String) messageItem.objArg2);
                    continue;
                case MESSAGE_APPLY_SETTINGS:
                    Settings.applyNewRuntimeSettings((Settings.RuntimeSettings) messageItem.objArg1);
                    FrequencyManager.notifySettingsChanged();
                    HttpServerManager.remap();
                    ConnectionPool.urgeAll();
                    continue;
                case MESSAGE_FLUSH_CONFIGS:
                    Settings.RuntimeSettings newSettings;
                    try {
                        newSettings = Settings.parseRuntimeSettingsFromSettingNodes(Settings.parseSettingNodesFromConfigJsons(Settings.loadConfigJsons()));
                    } catch (IOException | Settings.ApplyException e) {
                        Log.logMain(Log.LOG_ERROR, Settings.LOG_CATEGORY, e.getMessage());
                        continue;
                    }
                    Settings.applyNewRuntimeSettings(newSettings);
                    FrequencyManager.notifySettingsChanged();
                    HttpServerManager.remap();
                    ConnectionPool.urgeAll();
                    continue;
                case MESSAGE_FLUSH_BLACKLIST:
                    BlackListManager.reloadBlackList();
                    continue;
                case MESSAGE_ADD_BLACKLIST:
                    BlackListManager.addBlackList((String)messageItem.objArg1, (((long)messageItem.intArg1) << 32) | messageItem.intArg2);
                    continue;
                case MESSAGE_ADD_BLACKLIST_SYNC:
                    messageItem.resultArg = BlackListManager.addBlackList((String)messageItem.objArg1, (((long)messageItem.intArg1) << 32) | messageItem.intArg2);
                    messageItem.semaphore.release();
                    continue;
                case MESSAGE_REMOVE_BLACKLIST:
                    BlackListManager.removeBlackList((String)messageItem.objArg1);
                    continue;
                case MESSAGE_CLEAN_BLACKLIST:
                    BlackListManager.cleanBlackList();
                    continue;
            }
            break;
        }

        //收拾烂摊子

        Log.shutDown();
        HttpServerManager.shutDown();
        ConnectionPool.shutDown();
        TraceManager.shutDown();
        CommandReceiver.shutDown();
    }
}
