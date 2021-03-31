package com.davidsoft.serverprotect.http;

public interface WebApplication {

    void onCreate();
    void onUrge();
    void onForceStop();
    void onDestroy();
    boolean isProtectEnabled();
    String getName();
    HttpResponseSender onClientRequest(HttpRequestReceiver requestReceiver, String clientIp, int serverPort, boolean ssl);
}