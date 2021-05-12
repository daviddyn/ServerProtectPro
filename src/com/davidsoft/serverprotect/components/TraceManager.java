package com.davidsoft.serverprotect.components;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class TraceManager {

    private static final long TRACE_NODE_EXPIRES = 600000;
    private static final long TRACE_NODE_CLEAN_INTERVAL = 600000;

    private static final class TraceNode {
        private long expires;
        private TraceInfo traceInfo;
    }

    public static final class TraceInfo {
        public String requiredRedirectLocation;
        public final ArrayList<String> history = new ArrayList<>();
    }

    private static final String LOG_CATEGORY = "访问路径维护模块";
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static final HashMap<String, TraceNode> traces = new HashMap<>();
    private static final Timer timer = new Timer();


    //此函数可能会被其他线程调用
    private static boolean doCleanUp() {
        try {
            lock.writeLock().lockInterruptibly();
        } catch (InterruptedException e) {
            return false;
        }
        long now = System.currentTimeMillis();
        int removeCount = 0;
        Iterator<TraceNode> iterator = traces.values().iterator();
        while (iterator.hasNext()) {
            TraceNode node = iterator.next();
            if (now >= node.expires) {
                iterator.remove();
                removeCount++;
            }
        }
        if (removeCount > 0) {
            Program.logMain(Log.LOG_INFO, LOG_CATEGORY, "已清理 " + removeCount + " 个路径历史节点。");
        }
        lock.writeLock().unlock();
        return true;
    }

    public static boolean startUp() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!doCleanUp()) {
                    timer.cancel();
                }
            }
        }, TRACE_NODE_CLEAN_INTERVAL, TRACE_NODE_CLEAN_INTERVAL);
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "访问路径维护模块初始化成功！");
        return true;
    }

    public static void shutDown() {
        timer.cancel();
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "访问路径维护模块已停止。");
    }

    //此函数可能会被其他线程调用
    public static TraceInfo getTraceInfo(String nodeId) {
        try {
            lock.readLock().lockInterruptibly();
        } catch (InterruptedException e) {
            return null;
        }
        TraceNode traceNode = traces.get(nodeId);
        lock.readLock().unlock();
        if (traceNode == null) {
            return null;
        }
        return traceNode.traceInfo;
    }

    //此函数可能会被其他线程调用
    public static void registerRedirect(String nodeId, String requiredRedirectLocation) {
        try {
            lock.writeLock().lockInterruptibly();
        } catch (InterruptedException e) {
            return;
        }
        TraceNode traceNode = traces.get(nodeId);
        if (traceNode == null) {
            traceNode = new TraceNode();
            traceNode.traceInfo = new TraceInfo();
            traces.put(nodeId, traceNode);
        }
        traceNode.expires = System.currentTimeMillis() + TRACE_NODE_EXPIRES;
        traceNode.traceInfo.requiredRedirectLocation = requiredRedirectLocation;
        lock.writeLock().unlock();
    }
}