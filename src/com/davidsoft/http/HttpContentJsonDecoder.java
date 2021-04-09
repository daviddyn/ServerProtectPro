package com.davidsoft.http;

import com.davidsoft.utils.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;

public class HttpContentJsonDecoder implements HttpContentDecoder<JsonNode> {

    private final Charset charset;

    public HttpContentJsonDecoder(Charset charset) {
        this.charset = charset;
    }

    @Override
    public JsonNode onDecode(InputStream in, long contentLength) throws UnacceptableException, IOException {
        try {
            return JsonNode.parseJson(new HttpContentStringDecoder(charset).onDecode(in, contentLength));
        } catch (ParseException e) {
            e.printStackTrace();
            throw new SyntaxException(e);
        }
    }
}
