package com.davidsoft.serverprotect.components;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public final class FrequencyManager {

    private static final String LOG_CATEGORY = "访问频率模块";

    private static final class RequestNode {
        private final int ip;
        private final long when;

        private RequestNode(int ip, long when) {
            this.ip = ip;
            this.when = when;
        }
    }

    private static final ReentrantLock lock = new ReentrantLock(true);
    private static long frequencyDetectInterval;
    private static int frequencyDetectTimes;
    private static LinkedList<RequestNode> requestSequence;

    public static void initManager() {
        frequencyDetectInterval = Settings.getRuntimeSettings().protections.frequencyDetectIntervalInSecond * 1000;
        frequencyDetectTimes = Settings.getRuntimeSettings().protections.frequencyDetectTimes;
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "访问频率模块初始化成功！");
    }

    public static void notifySettingsChanged() {
        Settings.RuntimeSettings runtimeSettings = Settings.getRuntimeSettings();
        long frequencyDetectInterval = runtimeSettings.protections.frequencyDetectIntervalInSecond * 1000;
        int frequencyDetectTimes = runtimeSettings.protections.frequencyDetectTimes;
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            return;
        }
        FrequencyManager.frequencyDetectInterval = frequencyDetectInterval;
        FrequencyManager.frequencyDetectTimes = frequencyDetectTimes;
        lock.unlock();
    }

    //此函数可能会被其他线程调用
    private static void startManagerIfNotStarted() {
        if (requestSequence == null) {
            requestSequence = new LinkedList<>();
        }
    }

    //此函数可能会被其他线程调用
    public static boolean query(int ip, long requestTime) {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            return false;
        }
        startManagerIfNotStarted();

        Iterator<RequestNode> iterator = requestSequence.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            RequestNode node = iterator.next();
            if (requestTime - node.when >= frequencyDetectInterval) {
                iterator.remove();
            }
            else if (node.ip == ip) {
                count++;
            }
        }
        if (count < frequencyDetectTimes) {
            requestSequence.add(new RequestNode(ip, requestTime));
            lock.unlock();
            return true;
        }
        else {
            lock.unlock();
            return false;
        }
    }
}
