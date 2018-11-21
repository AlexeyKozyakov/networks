package util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonParser {
    private Gson gson = new Gson();

    public <T> T fromJson(InputStream jsonStream, Class<T> tClass) {
        T res = gson.fromJson(new InputStreamReader(jsonStream), tClass);
        if (res == null) {
            throw new JsonSyntaxException("Empty input stream");
        }
        return res;
    }

    public String toJson(Object src) {
        return gson.toJson(src);
    }
}
