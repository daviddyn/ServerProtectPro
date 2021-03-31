package com.davidsoft.serverprotect.libs;

import com.davidsoft.collections.IdSet;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool {

    private final class PooledThread extends Thread {
        private final int id;
        private final PooledRunnable pooledRunnable;
        private boolean releaseResourceOnStop;

        private PooledThread(int id, PooledRunnable pooledRunnable) {
            this.id = id;
            this.pooledRunnable = pooledRunnable;
            releaseResourceOnStop = true;
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

    private final ReentrantLock lock;
    private final Semaphore semaphore;
    private final IdSet<PooledThread> threads;
    private final int urgeThreshold;

    public ThreadPool(int maxThreads, int maxServices) {
        lock = new ReentrantLock();
        semaphore = new Semaphore(maxThreads);
        threads = new IdSet<>(0, maxServices - 1);
        urgeThreshold = (int) (maxServices * 0.2);
    }

    public void shutDown() {
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
    }

    public void urgeAll() {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            return;
        }
        requireUrge();
        lock.unlock();
    }

    private void requireUrge() {
        for (PooledThread thread : threads) {
            thread.requireUrge();
        }
    }

    private void requireForeStop() {
        for (PooledThread thread : threads) {
            thread.requireForceStop();
        }
    }

    public void active(PooledRunnable runnable) throws InterruptedException {
        lock.lockInterruptibly();
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
        semaphore.acquire();
        thread.start();
    }
}
