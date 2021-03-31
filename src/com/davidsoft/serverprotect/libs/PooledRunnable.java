package com.davidsoft.serverprotect.libs;

public interface PooledRunnable extends Runnable {
    void runWithoutService();
    void onUrge();
    void onForceStop();
}
