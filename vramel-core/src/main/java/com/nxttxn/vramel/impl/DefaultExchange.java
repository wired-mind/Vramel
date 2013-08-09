package com.nxttxn.vramel.impl;

import com.google.common.collect.Maps;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.RuntimeVramelException;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.spi.UnitOfWork;
import com.nxttxn.vramel.util.ObjectHelper;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 2:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultExchange implements Exchange {
    private final VramelContext vramelContext;
    private Map<String, Object> properties;
    private Message in;
    private Message out;
    private Exception exception;
    private UnitOfWork unitOfWork;
    private String fromRouteId;
    private String exchangeId;

    public DefaultExchange(VramelContext vramelContext) {

        this.vramelContext = vramelContext;
    }

    public DefaultExchange(Exchange parent) {
        this.vramelContext = parent.getContext();

        this.fromRouteId = parent.getFromRouteId();
        this.unitOfWork = parent.getUnitOfWork();
    }

    @Override
    public Message getIn() {
        if (in == null) {
            in = new DefaultMessage();
            configureMessage(in);
        }
        return in;
    }

    @Override
    public Message getOut() {
        if (out == null) {
            out = new DefaultMessage();
            configureMessage(out);
        }
        return out;
    }

    @Override
    public void setIn(Message message) {
        in = message;
        configureMessage(in);
    }

    @Override
    public Object getProperty(String key) {
        if (properties != null) {
            return properties.get(key);
        }
        return null;
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        Object answer = getProperty(key);
        return answer != null ? answer : defaultValue;
    }

    @Override
    public <T> T getProperty(String key, Class<T> type) {
        Object value = getProperty(key);
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

        throw new RuntimeVramelException("Cannot cast value");
    }

    @Override
    public <T> T getProperty(String key, Object defaultValue, Class<T> type) {
        Object value = getProperty(key, defaultValue);
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

        throw new RuntimeVramelException("Cannot cast value");
    }


    @Override
    public void setProperty(String key, Object value) {
        if (value != null) {
            // avoid the NullPointException
            getProperties().put(key, value);
        } else {
            // if the value is null, we just remove the key from the map
            if (key != null) {
                getProperties().remove(key);
            }
        }
    }

    @Override
    public Object removeProperty(String name) {
        if (!hasProperties()) {
            return null;
        }
        return getProperties().remove(name);
    }

    @Override
    public Exchange copy() {
        DefaultExchange exchange = new DefaultExchange(this);

        if (hasProperties()) {
            exchange.setProperties(safeCopy(getProperties()));
        }

        exchange.setIn(getIn().copy());
        if (hasOut()) {
            exchange.setOut(getOut().copy());
        }
        exchange.setException(getException());
        return exchange;
    }

    @Override
    public boolean hasOut() {
        return out != null;
    }
    @Override
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    private static Map<String, Object> safeCopy(Map<String, Object> properties) {
        if (properties == null) {
            return null;
        }
        return new ConcurrentHashMap<String, Object>(properties);
    }


    @Override
    public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }


    @Override
    public void setOut(Message message) {
        out = message;
        configureMessage(out);
    }



    @Override
    public Boolean isFailed() {
        return (hasOut() && getOut().isFault()) || getException() != null;
    }

    @Override
    public void setException(Throwable t) {
        if (t == null) {
            this.exception = null;
        } else if (t instanceof Exception) {
            this.exception = (Exception) t;
        } else {
            // wrap throwable into an exception
            this.exception = ObjectHelper.wrapVramelExecutionException(this, t);
        }

    }
    public <T> T getException(Class<T> type) {
        return ObjectHelper.getException(type, exception);
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public Map<String, Object> getProperties() {
        if (properties == null) {
            properties = Maps.newConcurrentMap();
        }
        return properties;
    }

    @Override
    public VramelContext getContext() {
        return vramelContext;
    }


    public UnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    public void setUnitOfWork(UnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }


    @Override
    public String getFromRouteId() {
        return fromRouteId;
    }

    @Override
    public void setFromRouteId(String fromRouteId) {
        this.fromRouteId = fromRouteId;
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    private void configureMessage(Message message) {
        if (message instanceof DefaultMessage) {
            DefaultMessage defaultMessage = (DefaultMessage)message;
            defaultMessage.setExchange(this);
        }
    }

    public String getExchangeId() {
        if (exchangeId == null) {
            exchangeId = createExchangeId();
        }
        return exchangeId;
    }

    @Override
    public void setExchangeId(String id) {
        this.exchangeId = id;
    }

    @SuppressWarnings("deprecation")
    protected String createExchangeId() {
        String answer = null;

        if (answer == null) {
            answer = vramelContext.getUuidGenerator().generateUuid();
        }
        return answer;
    }
}
