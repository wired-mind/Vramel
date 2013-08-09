package com.nxttxn.vramel;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 10:19 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Message {
    void setBody(Object body);
    <T> T getBody();
    <T> T getBody(Class<T> type);

    boolean hasHeaders();
    Map<String, Object> getHeaders();
    <T> T getHeader(String key);
    <T> T getHeader(String name, T defaultValue);
    <T> T getHeader(String name, T defaultValue, Class<T> type);
    <T> T getHeader(String key, Class<T> type);
    void setHeaders(Map<String, Object> headers);
    <T> void setHeader(String key, T value);
    <T> T removeHeader(String name);

    Message copy();
    void copyFrom(Message that);

    Exchange getExchange();


    Object getMandatoryBody() throws InvalidPayloadException;

    <T> T getMandatoryBody(Class<T> type) throws InvalidPayloadException;

    boolean isFault();

    void setFault(boolean fault);

    Map<Object, Object> getAttachments();

    String getMessageId();
}
