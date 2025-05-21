package io.github.urusso.easyhttpclient.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.urusso.easyhttpclient.interfaces.EasySerializer;

import java.io.IOException;

public class EasyJacksonSerializer implements EasySerializer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public <T> String serialize(T object) throws IOException {
        return MAPPER.writeValueAsString(object);
    }

    @Override
    public <T> T deserialize(String data, Class<T> clazz) throws IOException {
        return MAPPER.readValue(data, clazz);
    }
}
