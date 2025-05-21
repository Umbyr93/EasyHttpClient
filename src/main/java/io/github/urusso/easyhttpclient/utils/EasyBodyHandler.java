package io.github.urusso.easyhttpclient.utils;

import io.github.urusso.easyhttpclient.dto.Body;
import io.github.urusso.easyhttpclient.exception.DeserializationException;
import io.github.urusso.easyhttpclient.exception.FileNotFoundRuntimeException;
import io.github.urusso.easyhttpclient.exception.SerializationException;
import io.github.urusso.easyhttpclient.interfaces.EasySerializer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class EasyBodyHandler {
    private final EasySerializer serializer;

    public EasyBodyHandler(EasySerializer serializer) {
        this.serializer = serializer;
    }

    @SuppressWarnings("unchecked")
    public <T> HttpResponse.BodyHandler<T> getResponseBodyHandler(Class<T> responseClass) {
        if(responseClass == null || responseClass == Void.class) {
            return (HttpResponse.BodyHandler<T>) HttpResponse.BodyHandlers.discarding();
        } else if (responseClass == String.class) {
            return (HttpResponse.BodyHandler<T>) HttpResponse.BodyHandlers.ofString();
        } else if (responseClass == byte[].class) {
            return (HttpResponse.BodyHandler<T>) HttpResponse.BodyHandlers.ofByteArray();
        } else if (responseClass == InputStream.class) {
            return (HttpResponse.BodyHandler<T>) HttpResponse.BodyHandlers.ofInputStream();
        } else if (responseClass == Path.class) {
            return (HttpResponse.BodyHandler<T>) HttpResponse.BodyHandlers.ofFile(Path.of("response.tmp"));
        } else {
            return jacksonResponseHandler(serializer, responseClass);
        }
    }

    public HttpRequest.BodyPublisher getRequestBodyPublisher(Body body) {
        if (body == null || body.content() == null)
            return HttpRequest.BodyPublishers.noBody();

        Class<?> bodyClass = body.type();

        if (bodyClass == String.class) {
            return HttpRequest.BodyPublishers.ofString((String) body.content());
        } else if (bodyClass == byte[].class) {
            return HttpRequest.BodyPublishers.ofByteArray((byte[]) body.content());
        } else if (InputStream.class.isAssignableFrom(bodyClass)) {
            return HttpRequest.BodyPublishers.ofInputStream(() -> (InputStream) body.content());
        } else if (Path.class.isAssignableFrom(bodyClass)) {
            try {
                return HttpRequest.BodyPublishers.ofFile((Path) body.content());
            } catch (FileNotFoundException e) {
                throw new FileNotFoundRuntimeException(e);
            }
        } else {
            return jacksonRequestHandler(serializer, body.content());
        }
    }

    private static <T> HttpResponse.BodyHandler<T> jacksonResponseHandler(EasySerializer serializer, Class<T> clazz) {
        return responseInfo -> HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8),
                body -> {
                    try {
                        if(body != null && !body.isBlank())
                            return serializer.deserialize(body, clazz);
                        else
                            return null;
                    } catch (Exception e) {
                        throw new DeserializationException(e);
                    }
                }
        );
    }

    private static <T> HttpRequest.BodyPublisher jacksonRequestHandler(EasySerializer serializer, T data) {
        try {
            String json = serializer.serialize(data);
            return HttpRequest.BodyPublishers.ofString(json);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}
