package io.github.urusso.easyhttpclient.interfaces;

import java.io.IOException;

public interface EasySerializer {
    <T> String serialize(T object) throws IOException;
    <T> T deserialize(String data, Class<T> clazz) throws IOException;
}
