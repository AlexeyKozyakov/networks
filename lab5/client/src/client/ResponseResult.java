package client;

import java.io.InputStream;

public class ResponseResult {

    private int code;
    private InputStream body;

    public ResponseResult(int code, InputStream body) {
        this.code = code;
        this.body = body;
    }

    public int getCode() {
        return code;
    }

    public InputStream getBody() {
        return body;
    }

}
