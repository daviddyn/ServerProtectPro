package com.davidsoft.serverprotect.components;

import com.davidsoft.serverprotect.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

public final class Log {

    public static final int LOG_INFO = 0;
    public static final int LOG_ERROR = 1;
    public static final int LOG_WARNING = 2;

    private static final String LOG_CATEGORY = "日志管理器";
    private static final byte[] LINE_SEPARATOR_BYTES = System.lineSeparator().getBytes(StandardCharsets.UTF_8);

    private static final File fileDirectory = new File("logs").getAbsoluteFile();
    private static final Calendar calendar = Calendar.getInstance();
    private static long lastDateTimeStamp;
    private static FileOutputStream requestLog;
    private static FileOutputStream mainLog;
    private static boolean fileLogInitialized = false;

    private static String getLogFileName(String logName, long timeStamp) {
        if (fileDirectory.exists()) {
            if (!fileDirectory.isDirectory()) {
                logMain(LOG_ERROR, LOG_CATEGORY, fileDirectory.getPath() + " 虽存在，但它并不是文件夹。");
                return null;
            }
        }
        else {
            if (!fileDirectory.mkdirs()) {
                logMain(LOG_ERROR, LOG_CATEGORY, "无法创建路径 " + fileDirectory.getPath());
                return null;
            }
        }
        calendar.setTimeInMillis(timeStamp);
        return String.format("%s%s%s_%04d-%02d-%02d.log",
                fileDirectory.getPath(),
                File.separator,
                logName,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DATE)
        );
    }

    private static boolean openFileLog() {
        String requestLogFileName = getLogFileName("requests", lastDateTimeStamp);
        if (requestLogFileName == null) {
            return false;
        }
        String mainLogFileName = getLogFileName("main", lastDateTimeStamp);
        if (mainLogFileName == null) {
            return false;
        }
        try {
            requestLog = new FileOutputStream(requestLogFileName, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logMain(LOG_ERROR, LOG_CATEGORY, "无法打开请求日志。");
            return false;
        }
        try {
            mainLog = new FileOutputStream(mainLogFileName, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Utils.closeWithoutException(requestLog);
            logMain(LOG_ERROR, LOG_CATEGORY, "无法打开总日志。");
            return false;
        }
        fileLogInitialized = true;
        return true;
    }

    private static void closeFileLog() {
        if (!fileLogInitialized) {
            return;
        }
        if (!Utils.closeWithoutException(requestLog)) {
            logMain(LOG_WARNING, LOG_CATEGORY, "无法关闭请求日志，您的日志文件可能无法被正常保存。");
        }
        if (!Utils.closeWithoutException(mainLog)) {
            logMain(LOG_WARNING, LOG_CATEGORY, "无法关闭总日志，您的日志文件可能无法被正常保存。");
        }
        fileLogInitialized = false;
    }

    private static boolean changeFileLog(long now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long currentDateTimeStamp = calendar.getTimeInMillis();
        if (currentDateTimeStamp <= lastDateTimeStamp) {
            return true;
        }
        closeFileLog();
        lastDateTimeStamp = currentDateTimeStamp;
        if (!openFileLog()) {
            logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法将日志写入文件，此后您将只能在控制台中查看日志。");
            return false;
        }
        return true;
    }

    public static void startUp() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        lastDateTimeStamp = calendar.getTimeInMillis();
        if (!openFileLog()) {
            logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法将日志写入文件，此后您将只能在控制台中查看日志。");
        }
        logMain(LOG_INFO, LOG_CATEGORY, "日志管理器初始化成功，现在您可以肆无忌惮地输出日志了！");
    }

    public static void shutDown() {
        if (fileLogInitialized) {
            closeFileLog();
            logMain(LOG_INFO, LOG_CATEGORY, "日志管理器已停止。");
        }
    }

    private static String buildLogItem(int logType, long timeStamp, String category, String content) {
        calendar.setTimeInMillis(timeStamp);
        return String.format(
                "%04d-%02d-%02d %02d:%02d:%02d %s [%s]: %s",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DATE),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
                switch (logType) {
                    case LOG_INFO -> "信息";
                    case LOG_ERROR -> "错误";
                    default -> "警告";
                },
                category,
                content
        );
    }

    public static void logRequest(int logType, String category, String content) {
        long now = System.currentTimeMillis();
        String logItem = buildLogItem(logType, now, category, content);
        if (logType == LOG_INFO) {
            System.out.println(logItem);
        }
        else {
            System.err.println(logItem);
        }
        if (!fileLogInitialized) {
            return;
        }
        if (!changeFileLog(now)) {
            return;
        }
        byte[] logData = logItem.getBytes(StandardCharsets.UTF_8);
        try {
            requestLog.write(logData);
            requestLog.write(LINE_SEPARATOR_BYTES);
            requestLog.flush();
        } catch (IOException e) {
            e.printStackTrace();
            closeFileLog();
            logMain(LOG_ERROR, LOG_CATEGORY, "无法写入请求日志。");
            closeFileLog();
            logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法将日志写入文件，此后您将只能在控制台中查看日志。");
        }
    }

    public static void logMain(int logType, String category, String content) {
        long now = System.currentTimeMillis();
        String logItem = buildLogItem(logType, now, category, content);
        if (logType == LOG_INFO) {
            System.out.println(logItem);
        }
        else {
            System.err.println(logItem);
        }
        if (!fileLogInitialized) {
            return;
        }
        if (!changeFileLog(now)) {
            return;
        }
        byte[] logData = logItem.getBytes(StandardCharsets.UTF_8);
        try {
            mainLog.write(logData);
            mainLog.write(LINE_SEPARATOR_BYTES);
            mainLog.flush();
        } catch (IOException e) {
            e.printStackTrace();
            logMain(LOG_ERROR, LOG_CATEGORY, "无法写入总日志。");
            closeFileLog();
            logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法将日志写入文件，此后您将只能在控制台中查看日志。");
        }
    }
}