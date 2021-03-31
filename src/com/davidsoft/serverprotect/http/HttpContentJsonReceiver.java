package com.davidsoft.serverprotect.http;

import com.davidsoft.utils.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;

public class HttpContentJsonReceiver implements HttpContentReceiver<JsonNode> {

    private final Charset charset;

    public HttpContentJsonReceiver(Charset charset) {
        this.charset = charset;
    }

    @Override
    public JsonNode onReceive(InputStream in, long contentLength) throws UnacceptableException, IOException {
        try {
            return JsonNode.parseJson(new HttpContentStringReceiver(charset).onReceive(in, contentLength));
        } catch (ParseException e) {
            e.printStackTrace();
            throw new SyntaxException(e);
        }
    }
}
