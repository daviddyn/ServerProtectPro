package com.davidsoft.serverprotect.components;

import com.davidsoft.net.RegexIP;
import com.davidsoft.serverprotect.Utils;
import com.davidsoft.net.RegexIpIndex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class BlackListManager {

    private static final String LOG_CATEGORY = "黑名单管理器";

    private static final File FILE_BLOCKS = new File("configs" + File.separator + "blocks");

    private static final ReentrantReadWriteLock memLock = new ReentrantReadWriteLock(true);
    private static final RegexIpIndex<long[]> blackList = new RegexIpIndex<>();
    public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static void initBlackList() {
        loadBlackList();
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "ip黑名单加载成功。");
    }

    static void reloadBlackList() {
        loadBlackList();
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已应用新的ip黑名单，将在下一个连接时生效。");
    }

    private static void loadBlackList() {
        String src = Utils.getFileString(FILE_BLOCKS, null);
        if (src == null) {
            Log.logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法打开 " + FILE_BLOCKS.getAbsolutePath() + "，黑名单未被加载。");
            return;
        }
        Scanner scanner = new Scanner(src);
        memLock.writeLock().lock();
        blackList.clear();
        int listCount = 0;
        int invalidCount = 0;
        long now = System.currentTimeMillis();
        while (scanner.hasNext()) {
            src = scanner.nextLine().trim();
            listCount++;
            if (src.startsWith("-")) {
                try {
                    blackList.removeExactly(RegexIP.parse(src.substring(1).trim()));
                } catch (ParseException e) {
                    Log.logMain(Log.LOG_WARNING, LOG_CATEGORY, FILE_BLOCKS.getAbsolutePath() + " 中发现无效的ip格式，已忽略。");
                }
                invalidCount++;
            }
            else {
                if (src.startsWith("+")) {
                    src = src.substring(1).trim();
                }
                int findPos = src.indexOf("\t");
                if (findPos == -1 || findPos == 0 || findPos == src.length() - 1) {
                    Log.logMain(Log.LOG_WARNING, LOG_CATEGORY, FILE_BLOCKS.getAbsolutePath() + " 中发现无效行，已忽略。");
                    invalidCount++;
                    continue;
                }
                long parsedIp;
                try {
                    parsedIp = RegexIP.parse(src.substring(0, findPos));
                } catch (ParseException e) {
                    Log.logMain(Log.LOG_WARNING, LOG_CATEGORY, FILE_BLOCKS.getAbsolutePath() + " 中发现无效的ip格式，已忽略。");
                    invalidCount++;
                    continue;
                }
                long parsedTime;
                try {
                    parsedTime = simpleDateFormat.parse(src.substring(findPos + 1).trim()).getTime();
                } catch (ParseException e) {
                    Log.logMain(Log.LOG_WARNING, LOG_CATEGORY, FILE_BLOCKS.getAbsolutePath() + " 中发现无效的时间格式，已忽略。");
                    invalidCount++;
                    continue;
                }
                if (parsedTime <= now) {
                    invalidCount++;
                    continue;
                }
                long[] expires = blackList.getExactly(parsedIp);
                if (expires == null) {
                    blackList.put(parsedIp, new long[]{parsedTime});
                }
                else if (expires[0] < parsedTime) {
                    expires[0] = parsedTime;
                    invalidCount++;
                }
            }
        }
        if (listCount > 10 && invalidCount >= (listCount >> 1)) {
            Log.logMain(Log.LOG_WARNING, LOG_CATEGORY, "黑名单中，无效/重复/过期的项目已超过半数，已启动清理。");
            cleanBlackListInner();
        }
        memLock.writeLock().unlock();
    }

    private static void cleanBlackListInner() {
        PrintStream out;
        try {
            out = new PrintStream(FILE_BLOCKS);
        } catch (FileNotFoundException e) {
            Log.logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法写入 " + FILE_BLOCKS.getAbsolutePath() + "，无法保存新的黑名单！");
            return;
        }
        for (Map.Entry<Long, long[]> entry : blackList.entrySet()) {
            out.print(RegexIP.toString(entry.getKey()));
            out.print("\t");
            out.println(simpleDateFormat.format(entry.getValue()[0]));
        }
        out.close();
    }

    static void cleanBlackList() {
        memLock.readLock().lock();
        cleanBlackListInner();
        memLock.readLock().unlock();
    }

    //返回：0-更新, -1-新建, 其他-未改变
    static long addBlackList(long regexIp, long expires) {
        try {
            memLock.writeLock().lockInterruptibly();
        } catch (InterruptedException e) {
            return 0;
        }
        long result;
        long[] queriedExpires = blackList.getExactly(regexIp);
        if (queriedExpires == null) {
            blackList.put(regexIp, new long[]{expires});
            result = -1;
        }
        else if (expires > queriedExpires[0]) {
            queriedExpires[0] = expires;
            result = 0;
        }
        else {
            result = queriedExpires[0];
        }
        if (result <= 0) {
            Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已禁止 " + RegexIP.toString(regexIp) + " 的访问，且将在 " + simpleDateFormat.format(expires) + " 解除。从下一个连接起开始生效。");
            PrintStream out;
            try {
                out = new PrintStream(new FileOutputStream(FILE_BLOCKS, true));
            } catch (FileNotFoundException e) {
                Log.logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法写入 " + FILE_BLOCKS.getAbsolutePath() + "，无法保存新的黑名单！");
                memLock.writeLock().unlock();
                return result;
            }
            out.print(RegexIP.toString(regexIp));
            out.print("\t");
            out.println(simpleDateFormat.format(expires));
            out.close();
        }
        memLock.writeLock().unlock();
        return result;
    }

    static void removeBlackList(long regexIp) {
        try {
            memLock.writeLock().lockInterruptibly();
        } catch (InterruptedException e) {
            return;
        }
        if (!blackList.removeExactly(regexIp)) {
            return;
        }
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已允许 " + RegexIP.toString(regexIp) + " 的访问。从下一个连接起开始生效。");
        PrintStream out;
        try {
            out = new PrintStream(new FileOutputStream(FILE_BLOCKS, true));
        } catch (FileNotFoundException e) {
            Log.logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法写入 " + FILE_BLOCKS.getAbsolutePath() + "，无法保存新的黑名单！");
            memLock.writeLock().unlock();
            return;
        }
        out.print("-");
        out.println(RegexIP.toString(regexIp));
        out.close();
        memLock.writeLock().unlock();
    }

    //此函数会在其他线程中调用
    public static boolean inBlackList(int ip) {
        memLock.readLock().lock();
        long[] queriedExpires = blackList.get(ip);
        boolean ret = (queriedExpires != null && queriedExpires[0] > System.currentTimeMillis());
        memLock.readLock().unlock();
        return ret;
    }

    //此函数会在其他线程中调用
    public static void freeze() {
        memLock.readLock().lock();
    }

    //此函数会在其他线程中调用
    public static void unfreeze() {
        memLock.readLock().unlock();
    }

    //此函数会在其他线程中调用
    public static Set<Map.Entry<Long, long[]>> entries() {
        return blackList.entrySet();
    }

    public static int size() {
        return blackList.size();
    }
}
