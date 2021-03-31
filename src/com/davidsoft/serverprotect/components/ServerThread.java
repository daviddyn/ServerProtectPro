package com.davidsoft.serverprotect.components;

import com.davidsoft.serverprotect.Utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread extends Thread {

    public final int port;
    public final boolean ssl;
    private final ServerSocket serverSocket;

    private boolean flag;

    public ServerThread(int port, boolean ssl, ServerSocket serverSocket) {
        this.port = port;
        this.ssl = ssl;
        this.serverSocket = serverSocket;
        flag = true;
    }

    @Override
    public void run() {
        super.run();
        //循环监听

        while (flag) {
            Socket socket;
            //等待新连接
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                return;
            }
            //创建线程，从连接池申请permit
            try {
                ConnectionPool.active(new ClientConnection(socket, port, ssl));
            } catch (InterruptedException e) {
                flag = false;
                return;
            }
        }
    }

    public void shutDown() {
        if (!flag) {
            return;
        }
        flag = false;
        Utils.closeWithoutException(serverSocket, true);
        interrupt();
    }
}
