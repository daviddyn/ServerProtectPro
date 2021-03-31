package com.davidsoft.serverprotect.components;

import com.davidsoft.serverprotect.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class CommandReceiver {

    private static final String LOG_CATEGORY = "命令控制";

    private static final class ServerThread extends Thread {

        private final ServerSocket serverSocket;
        private Socket socket;
        private boolean flag;
        private final byte[] receiveBuffer;

        private ServerThread(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
            flag = true;
            receiveBuffer = new byte[32];
        }

        @Override
        public void run() {
            super.run();
            while (flag) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    continue;
                }
                int length;
                try {
                    InputStream in = socket.getInputStream();
                    length = in.read(receiveBuffer);
                    OutputStream out = socket.getOutputStream();
                    out.write("OK".getBytes(StandardCharsets.UTF_8));
                    Utils.closeWithoutException(socket, true);
                }
                catch (IOException e) {
                    socket = null;
                    continue;
                }
                socket = null;
                if (length == -1) {
                    length = 0;
                }
                executeCommand(new String(receiveBuffer, 0, length, StandardCharsets.UTF_8));
            }
        }

        private void shutDown() {
            Utils.closeWithoutException(serverSocket, true);
            if (socket != null) {
                Utils.closeWithoutException(socket, true);
            }
            interrupt();
            flag = false;
        }

        //此函数可能会被其他线程调用
        private void executeCommand(String command) {
            switch (command) {
                case "shutdown":
                    Program.shutDown();
                    break;
                case "flushconfig":
                    Program.flushConfigs();
                    break;
                case "flushblacklist":
                    Program.flushBlacklist();
                    break;
            }
        }
    }

    private static ServerThread serverThread;

    public static boolean startUp() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(4937);
        } catch (IOException e) {
            Log.logMain(Log.LOG_ERROR, LOG_CATEGORY, "命令控制端口(4937)已被被占用，可能是由本程序已经启动引起的。");
            return false;
        }
        serverThread = new ServerThread(serverSocket);
        serverThread.start();
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "命令控制器初始化成功！");
        return true;
    }

    public static void shutDown() {
        if (serverThread == null) {
            return;
        }
        ServerThread thread = serverThread;
        serverThread = null;
        thread.shutDown();
        try {
            thread.join();
        } catch (InterruptedException ignored) {}
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "命令控制器已停止。");
    }
}
