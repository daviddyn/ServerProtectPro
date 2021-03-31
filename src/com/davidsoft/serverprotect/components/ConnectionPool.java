package com.davidsoft.serverprotect.components;

import com.davidsoft.serverprotect.libs.PooledRunnable;
import com.davidsoft.serverprotect.libs.ThreadPool;

public final class ConnectionPool {

    private static final String LOG_CATEGORY = "连接池";

    private static ThreadPool threadPool;

    public static void startUp() {
        threadPool = new ThreadPool(
                Settings.getStaticSettings().maxConnections,
                Settings.getStaticSettings().maxServices
        );
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "连接池初始化成功！");
    }

    public static void shutDown() {
        threadPool.shutDown();
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "连接池已全部释放！");
    }

    public static void active(PooledRunnable runnable) throws InterruptedException {
        threadPool.active(runnable);
    }

    public static void urgeAll() {
        threadPool.urgeAll();
    }
}
