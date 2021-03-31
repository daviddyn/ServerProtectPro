package com.davidsoft.serverprotect.components;

import com.davidsoft.collections.IdSet;
import com.davidsoft.serverprotect.libs.PooledRunnable;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public final class ConnectionPool {

    private static final String LOG_CATEGORY = "连接池";

    private static final class PooledThread extends Thread {
        private final int id;
        private final PooledRunnable pooledRunnable;
        private boolean releaseResourceOnStop;

        private PooledThread(int id, PooledRunnable pooledRunnable) {
            this.id = id;
            this.pooledRunnable = pooledRunnable;
        }

        @Override
        public void run() {
            super.run();
            if (id == -1) {
                pooledRunnable.runWithoutService();
            }
            else {
                pooledRunnable.run();
                if (releaseResourceOnStop) {
                    try {
                        lock.lockInterruptibly();
                    } catch (InterruptedException e) {
                        semaphore.release();
                        return;
                    }
                    threads.remove(id);
                    lock.unlock();
                }
            }
            if (releaseResourceOnStop) {
                semaphore.release();
            }
        }

        private void requireUrge() {
            releaseResourceOnStop = true;
            pooledRunnable.onUrge();
        }

        private void requireForceStop() {
            releaseResourceOnStop = false;
            pooledRunnable.onForceStop();
            interrupt();
        }
    }

    private static final ReentrantLock lock = new ReentrantLock();
    private static Semaphore semaphore;
    private static IdSet<PooledThread> threads;
    private static int urgeThreshold;

    public static void startUp(int maxConnections, int maxServices) {
        semaphore = new Semaphore(maxConnections);
        threads = new IdSet<>(0, maxServices - 1);
        urgeThreshold = (int) (maxServices * 0.2);
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "连接池初始化成功！");
    }

    public static void shutDown() {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            return;
        }
        for (PooledThread thread : threads) {
            thread.requireForceStop();
            try {
                thread.join();
            } catch (InterruptedException e) {}
        }
        lock.unlock();
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "连接池已全部释放！");
    }

    private static void requireUrge() {
        for (PooledThread thread : threads) {
            thread.requireUrge();
        }
    }

    private static void requireForeStop() {
        for (PooledThread thread : threads) {
            thread.requireForceStop();
        }
    }

    public static boolean active(PooledRunnable runnable) {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            return false;
        }
        if (threads.full()) {
            requireForeStop();
        }
        else if (threads.remain() <= urgeThreshold) {
            requireUrge();
        }
        int id = threads.peekId();
        PooledThread thread = new PooledThread(id, runnable);
        if (id != -1) {
            threads.add(thread);
        }
        lock.unlock();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            return false;
        }
        thread.start();
        return true;
    }
}
