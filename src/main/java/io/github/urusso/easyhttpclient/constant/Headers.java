package io.github.urusso.easyhttpclient.constant;

public enum Headers {
    USER_AGENT("User-Agent"),
    ACCEPT("Accept"),
    ACCEPT_LANGUAGE("Accept-Language"),
    ACCEPT_ENCODING("Accept-Encoding"),
    AUTHORIZATION("Authorization"),
    CONTENT_TYPE("Content-Type"),
    COOKIE("Cookie"),
    REFERER("Referer"),
    ORIGIN("Origin");

    public final String code;

    Headers(String code) {
        this.code = code;
    }
}
