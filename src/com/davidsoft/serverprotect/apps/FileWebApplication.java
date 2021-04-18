package com.davidsoft.serverprotect.apps;

import com.davidsoft.net.http.*;
import com.davidsoft.url.URI;

import java.io.File;
import java.io.IOException;

public class FileWebApplication extends BaseWebApplication {

    private HttpResponseSender fileResponse(File file) {
        //找不到就404
        if (!file.isFile()) {
            return new HttpResponseSender(new HttpResponseInfo(404), null);
        }
        //找到但不能读就500
        if (!file.canRead()) {
            return new HttpResponseSender(new HttpResponseInfo(500), null);
        }
        HttpContentFileProvider provider;
        try {
            provider = new HttpContentFileProvider(file, null);
        } catch (IOException e) {
            e.printStackTrace();
            return new HttpResponseSender(new HttpResponseInfo(500), null);
        }
        return new HttpResponseSender(new HttpResponseInfo(200), provider);
    }

    @Override
    protected HttpResponseSender onClientRequest(HttpRequestInfo requestInfo, HttpContentReceiver requestContent, int clientIp, URI requestRelativeURI) {
        return fileResponse(new File(getApplicationRootFile() + requestRelativeURI.toString(File.separator, null)));
    }

    @Override
    protected HttpResponseSender onGetFavicon(int ip) {
        return fileResponse(new File(getApplicationRootFile() + "favicon.ico"));
    }
}
