package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.components.gson.GsonDataFormat;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
        DefaultExchangeHolder holder = new DefaultExchangeHolder();
        holder.exchangeId = exchange.getExchangeId();
        holder.inBody = exchange.getIn().getBody();
        holder.safeSetInHeaders(exchange);

        if (exchange.hasOut()) {
            holder.outBody = exchange.getOut().getBody();
            holder.safeSetOutHeaders(exchange);

        }
        holder.safeSetProperties(exchange);
        holder.exception = exchange.getException();
        return holder;
    }

    private void safeSetProperties(Exchange exchange) {
        if (exchange.hasProperties()) {
            this.properties = exchange.getProperties();
        }
    }

    private void safeSetOutHeaders(Exchange exchange) {
        if (exchange.getOut().hasHeaders()) {
            this.outHeaders = exchange.getOut().getHeaders();
        }
    }

    private void safeSetInHeaders(Exchange exchange) {
        if (exchange.getIn().hasHeaders()) {
            this.inHeaders = exchange.getIn().getHeaders();
        }
    }

    public byte[] getBytes() throws Exception {
        return SerializationUtils.serialize(this);
    }
}
