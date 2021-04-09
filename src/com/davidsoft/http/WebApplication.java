package com.davidsoft.http;

public interface WebApplication {

    /**
     * 当此webapp被创建时调用。
     */
    void onCreate();

    /**
     * 当此webapp被催促终止时调用。
     * 当服务器当前的连接数达到一定阈值时，此webapp会被催促终止。
     * 被催促终止的webapp，会在它onClientRequest返回后立即被销毁(触发onDestroy)，且相应的连接不再会被复用(客户端浏览器将收到Connection: close)。
     */
    void onUrge();

    /**
     * 当此webapp被强行停止时调用。
     * 当服务器当前的连接数达到一定阈值，或受外部因素干预，使得此webapp不应继续执行时，此webapp将被强行停止。
     * 被强行停止的webapp，可以立即返回onClientRequest函数。onClientRequest返回的结果不再有用，外层将向客户端浏览器发送固定的内容。
     */
    void onForceStop();

    /**
     * 当此webapp被销毁时调用。
     */
    void onDestroy();

    /**
     * 用于告知外层此webapp是否受防火墙控制。若返回true，则触发了防火墙规则后，外层将进行相应的处理而不会触发onClientRequest函数。
     * 若返回false，则防火墙对此webapp无效。
     * 典型的应用场景为：webapp级别的ip白名单机制。webapp在此函数返回true，而后在onClientRequest中自己处理少数可以访问的ip。
     */
    boolean isProtectEnabled();

    /**
     * 用于告知外层此webapp的名称。
     * @return 此webapp的名称
     */
    String getName();

    /**
     * 当有客户浏览器请求时调用。
     *
     * @param requestInfo 在这个对象中获取请求信息，包括请求方法、url、请求头等
     * @param requestContent 在这个对象中获取请求的Content，对于不应有Content的请求(如GET请求)，此参数为null。
     * @param clientIp 客户端的ip
     * @param serverPort 当前服务端的端口号
     * @param ssl false: http连接; true: https连接。
     * @return 一个HttpResponseSender对象，用于告知外层向客户浏览器返回的内容。如果返回null，则外层会直接断开连接。
     */
    HttpResponseSender onClientRequest(HttpRequestInfo requestInfo, HttpContentReceiver requestContent, String clientIp, int serverPort, boolean ssl);
}