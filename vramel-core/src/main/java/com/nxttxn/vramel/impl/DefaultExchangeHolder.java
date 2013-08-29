package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.util.ObjectHelper;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/19/13
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultExchangeHolder implements Serializable {
    private String exchangeId;
    private Object inBody;
    private Object outBody;
    private Map<String, Object> inHeaders;
    private Map<String, Object> outHeaders;
    private Exception exception;
    protected static final Logger logger = LoggerFactory.getLogger(DefaultExchangeHolder.class);
    private Map<String, Object> properties;
//    static GsonDataFormat exhangeFormatter;
//
//    static {
//        try {
//            exhangeFormatter = new GsonDataFormat(DefaultExchangeHolder.class);
//            exhangeFormatter.setPolymorphic(true);
//        } catch (Exception e) {
//            logger.error("Cannot create gson exchange formatter", e);
//        }
//    }

    public static void unmarshal(Exchange exchange, byte[] bytes) throws Exception {
        DefaultExchangeHolder holder = (DefaultExchangeHolder) SerializationUtils.deserialize(bytes);

        exchange.setExchangeId(holder.exchangeId);
        exchange.getIn().setBody(holder.inBody);

        if (holder.inHeaders != null) {
            exchange.getIn().setHeaders(holder.inHeaders);
        }
        if (holder.outBody != null) {
            exchange.getOut().setBody(holder.outBody);
            if (holder.outHeaders != null) {
                exchange.getOut().setHeaders(holder.outHeaders);
            }
        }
        if (holder.properties != null) {
            for (String key : holder.properties.keySet()) {
                exchange.setProperty(key, holder.properties.get(key));
            }

        }
        exchange.setException(holder.exception);


    }

    public static DefaultExchangeHolder marshal(Exchange exchange) {
        return marshal(exchange, true);
    }

    public static DefaultExchangeHolder marshal(Exchange exchange, boolean includeProperties) {
        DefaultExchangeHolder holder = new DefaultExchangeHolder();
        holder.exchangeId = exchange.getExchangeId();
        holder.inBody = checkSerializableBody("in body", exchange, exchange.getIn().getBody());
        holder.safeSetInHeaders(exchange);

        if (exchange.hasOut()) {
            holder.outBody = checkSerializableBody("out body", exchange, exchange.getOut().getBody());
            holder.safeSetOutHeaders(exchange);

        }
        if (includeProperties) {
            holder.safeSetProperties(exchange);
        }
        holder.exception = exchange.getException();
        return holder;
    }

    private Map<String, Object> safeSetProperties(Exchange exchange) {
        if (exchange.hasProperties()) {
            Map<String, Object> map = checkMapSerializableObjects("properties", exchange, exchange.getProperties());
            if (map != null && !map.isEmpty()) {
                properties = new LinkedHashMap<String, Object>(map);
            }
        }
        return null;
    }


    private Map<String, Object> safeSetOutHeaders(Exchange exchange) {
        if (exchange.hasOut() && exchange.getOut().hasHeaders()) {
            Map<String, Object> map = checkMapSerializableObjects("out headers", exchange, exchange.getOut().getHeaders());
            if (map != null && !map.isEmpty()) {
                outHeaders = new LinkedHashMap<String, Object>(map);
            }
        }
        return null;
    }

    private Map<String, Object> safeSetInHeaders(Exchange exchange) {
        if (exchange.getIn().hasHeaders()) {
            Map<String, Object> map = checkMapSerializableObjects("in headers", exchange, exchange.getIn().getHeaders());
            if (map != null && !map.isEmpty()) {
                inHeaders = new LinkedHashMap<String, Object>(map);
            }
        }
        return null;
    }

    public byte[] getBytes() throws Exception {
        return SerializationUtils.serialize(this);
    }

    private static Object checkSerializableBody(String type, Exchange exchange, Object object) {
        if (object == null) {
            return null;
        }

        Serializable converted = exchange.getContext().getTypeConverter().convertTo(Serializable.class, exchange, object);
        if (converted != null) {
            return converted;
        } else {
            logger.warn("Exchange " + type + " containing object: " + object + " of type: " + object.getClass().getCanonicalName() + " cannot be serialized, it will be excluded by the holder.");
            return null;
        }
    }

    private static Map<String, Object> checkMapSerializableObjects(String type, Exchange exchange, Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {

            // silently skip any values which is null
            if (entry.getValue() != null) {
                Serializable converted = exchange.getContext().getTypeConverter().convertTo(Serializable.class, exchange, entry.getValue());

                // if the converter is a map/collection we need to check its content as well
                if (converted instanceof Collection) {
                    Collection<?> valueCol = (Collection<?>) converted;
                    if (!collectionContainsAllSerializableObjects(valueCol, exchange)) {
                        logCannotSerializeObject(type, entry.getKey(), entry.getValue());
                        continue;
                    }
                } else if (converted instanceof Map) {
                    Map<?, ?> valueMap = (Map<?, ?>) converted;
                    if (!mapContainsAllSerializableObjects(valueMap, exchange)) {
                        logCannotSerializeObject(type, entry.getKey(), entry.getValue());
                        continue;
                    }
                }

                if (converted != null) {
                    result.put(entry.getKey(), converted);
                } else {
                    logCannotSerializeObject(type, entry.getKey(), entry.getValue());
                }
            }
        }

        return result;
    }

    private static void logCannotSerializeObject(String type, String key, Object value) {
        if (key.startsWith("Camel")) {
            // log Camel at DEBUG level
            if (logger.isDebugEnabled()) {
                logger.debug("Exchange {} containing key: {} with object: {} of type: {} cannot be serialized, it will be excluded by the holder."
                        , new Object[]{type, key, value, ObjectHelper.classCanonicalName(value)});
            }
        } else {
            // log regular at WARN level
            logger.warn("Exchange {} containing key: {} with object: {} of type: {} cannot be serialized, it will be excluded by the holder."
                    , new Object[]{type, key, value, ObjectHelper.classCanonicalName(value)});
        }
    }

    private static boolean collectionContainsAllSerializableObjects(Collection<?> col, Exchange exchange) {
        for (Object value : col) {
            if (value != null) {
                Serializable converted = exchange.getContext().getTypeConverter().convertTo(Serializable.class, exchange, value);
                if (converted == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean mapContainsAllSerializableObjects(Map<?, ?> map, Exchange exchange) {
        for (Object value : map.values()) {
            if (value != null) {
                Serializable converted = exchange.getContext().getTypeConverter().convertTo(Serializable.class, exchange, value);
                if (converted == null) {
                    return false;
                }
            }
        }
        return true;
    }
}
