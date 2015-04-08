package com.nxttxn.vramel.impl;


import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.InvalidPayloadException;
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.TypeConverter;
import com.nxttxn.vramel.util.CaseInsensitiveMap;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 10:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultMessage implements Message {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessage.class);
    private boolean fault;
    private Object body;
    private Map<String, Object> headers;
    private Exchange exchange;

    @Override
    public void setBody(Object body) {

        this.body = body;
    }

    public <T> T getBody() {
        return (T) body;
    }

    @Override
    public <T> T getBody(Class<T> type) {
        return getBody(type, getBody());
    }

    protected <T> T getBody(Class<T> type, Object body) {
        // eager same instance type test to avoid the overhead of invoking the type converter
        // if already same type
        if (type.isInstance(body)) {
            return type.cast(body);
        }

        Exchange e = getExchange();
        if (e != null) {
            TypeConverter converter = e.getContext().getTypeConverter();

            // lets first try converting the body itself first
            // as for some types like InputStream v Reader its more efficient to do the transformation
            // from the body itself as its got efficient implementations of them, before trying the message
            T answer = converter.convertTo(type, e, body);
            if (answer != null) {
                return answer;
            }

            // fallback and try the message itself (e.g. used in camel-http)
            answer = converter.tryConvertTo(type, e, this);
            if (answer != null) {
                return answer;
            }
        }

        // not possible to convert
        return null;
    }



    public Object getMandatoryBody() throws InvalidPayloadException {
        Object answer = getBody();
        if (answer == null) {
            throw new InvalidPayloadException(getExchange(), Object.class, this);
        }
        return answer;
    }

    public <T> T getMandatoryBody(Class<T> type) throws InvalidPayloadException {
        // eager same instance type test to avoid the overhead of invoking the type converter
        // if already same type
        if (type.isInstance(body)) {
            return type.cast(body);
        }

        Exchange e = getExchange();
        if (e != null) {
            TypeConverter converter = e.getContext().getTypeConverter();
            try {
                return converter.mandatoryConvertTo(type, e, getBody());
            } catch (Exception cause) {
                throw new InvalidPayloadException(e, type, this, cause);
            }
        }
        throw new InvalidPayloadException(e, type, this);
    }

    public Map<String, Object> getHeaders() {
        if (headers == null) {
            headers = createHeaders();
        }
        return headers;
    }

    @Override
    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    @Override
    public <T> T getHeader(String key) {
        return (T) getHeaders().get(key);
    }

    @Override
    public <T> T getHeader(String name, T defaultValue) {
        T answer = (T) getHeaders().get(name);
        return answer != null ? answer : defaultValue;
    }

    @Override
    public <T> T getHeader(String key, Class<T> type) {
        Object value = getHeader(key);
        if (value == null) {
            // lets avoid NullPointerException when converting to boolean for null values
            if (boolean.class.isAssignableFrom(type)) {
                return (T) Boolean.FALSE;
            }
            return null;
        }

        // eager same instance type test to avoid the overhead of invoking the type converter
        // if already same type
        if (type.isInstance(value)) {
            return type.cast(value);
        }


       return type.cast(value);

    }

    @Override
    public <T> T getHeader(String name, T defaultValue, Class<T> type) {
        Object value = getHeader(name, defaultValue);
        if (value == null) {
            // lets avoid NullPointerException when converting to boolean for null values
            if (boolean.class.isAssignableFrom(type)) {
                return (T) Boolean.FALSE;
            }
            return null;
        }

        // eager same instance type test to avoid the overhead of invoking the type converter
        // if already same type
        if (type.isInstance(value)) {
            return type.cast(value);
        }

        Exchange e = getExchange();
        if (e != null) {
            //this will fail
            return e.getContext().getTypeConverter().convertTo(type, e, value);
        } else {
            return type.cast(value);
        }
    }


    public void copyFrom(Message that) {
        if (that == this) {
            // the same instance so do not need to copy
            return;
        }
        setBody(that.getBody());


        if (hasHeaders()) {
            getHeaders().clear();
        }
        if (that.hasHeaders()) {
            getHeaders().putAll(that.getHeaders());
        }

    }

    @Override
    public Exchange getExchange() {
        return exchange;
    }

    @Override
    public boolean hasHeaders() {
        if (!hasPopulatedHeaders()) {
            // force creating headers
            getHeaders();
        }
        return headers != null && !headers.isEmpty();
    }
    /**
     * A strategy method populate the initial set of headers on an inbound
     * message from an underlying binding
     *
     * @param map is the empty header map to populate
     */
    protected void populateInitialHeaders(Map<String, Object> map) {
        // do nothing by default
    }

    /**
     * A factory method to lazily create the headers to make it easy to create
     * efficient Message implementations which only construct and populate the
     * Map on demand
     *
     * @return return a newly constructed Map possibly containing headers from
     *         the underlying inbound transport
     */
    protected Map<String, Object> createHeaders() {
        Map<String, Object> map = new CaseInsensitiveMap();
        populateInitialHeaders(map);
        return map;
    }

    public void setHeader(String name, Object value) {
        if (headers == null) {
            headers = createHeaders();
        }
        headers.put(name, value);
    }

    @Override
    public <T> T removeHeader(String name) {
        if (!hasHeaders()) {
            return null;
        }
        return (T) headers.remove(name);
    }

    @Override
    public Message copy() {
        Message newMessage = new DefaultMessage();
        newMessage.copyFrom(this);
        return newMessage;
    }

    /**
     * Returns true if the headers have been mutated in some way
     */
    protected boolean hasPopulatedHeaders() {
        return headers != null;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public boolean isFault() {
        return fault;
    }

    @Override
    public void setFault(boolean fault) {
        this.fault = fault;
    }

    @Override
    public Map<Object, Object> getAttachments() {
        throw new UnsupportedOperationException("Not even the right interface yet");
    }

    @Override
    public String getMessageId() {
        throw new UnsupportedOperationException("No message id yet");
    }
}
