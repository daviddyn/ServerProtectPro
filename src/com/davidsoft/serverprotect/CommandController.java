package com.davidsoft.serverprotect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class CommandController {

    public static void main(String[] args) {
        Socket socket;
        try {
            socket = new Socket("127.0.0.1", 4937);
        } catch (IOException e) {
            System.err.println("主程序尚未运行。");
            return;
        }
        try {
            OutputStream out = socket.getOutputStream();
            out.write(args[0].getBytes(StandardCharsets.UTF_8));
            //out.close();
            InputStream in = socket.getInputStream();
            byte[] receiveBuffer = new byte[16];
            int length = in.read(receiveBuffer);
            //in.close();
            if (length == -1) {
                length = 0;
            }
            if ("OK".equals(new String(receiveBuffer, 0, length, StandardCharsets.UTF_8))) {
                System.out.println("完成。");
            } else {
                System.err.println("与主程序通信异常。");
            }
        } catch (IOException e) {
            System.err.println("无法连接至主程序。");
        }
        Utils.closeWithoutException(socket, true);
    }
}
