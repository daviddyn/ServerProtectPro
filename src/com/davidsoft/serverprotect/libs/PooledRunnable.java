package com.davidsoft.serverprotect.libs;

/**
 * 被池管理的Runnable。
 */
public interface PooledRunnable extends Runnable {

    /**
     * 在此函数中定义当剩余资源不足时需要执行的内容。一旦此函数开始执行，此对象的onUrge()和onForceStop()将永不会触发。
     */
    void runWithoutService();

    /**
     * 在此函数中定义正常情况下需要执行的内容。在此函数返回之前，可能会在另一个线程中触发一次onUrge()或一次onForceStop()两者之一。
     */
    @Override
    void run();

    void onUrge();

    void onForceStop();
}
