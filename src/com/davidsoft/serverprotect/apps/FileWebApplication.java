package com.davidsoft.serverprotect.apps;

import com.davidsoft.http.*;
import com.davidsoft.serverprotect.libs.HttpPath;

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
    protected HttpResponseSender onClientRequest(HttpRequestInfo requestInfo, HttpContentReceiver requestContent, String ip, HttpPath requestRelativePath) {
        return fileResponse(new File(getApplicationRootFile() + File.separator + String.join(File.separator, requestRelativePath.getPatterns())));
    }

    @Override
    protected HttpResponseSender onGetFavicon(String ip) {
        return fileResponse(new File(getApplicationRootFile() + File.separator + "favicon.ico"));
    }
}
