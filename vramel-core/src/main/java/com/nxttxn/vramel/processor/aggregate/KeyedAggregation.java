package com.nxttxn.vramel.processor.aggregate;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 2:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class KeyedAggregation implements Serializable {
    Map<String, Object> results = Maps.newHashMap();
    public <V> void put(String key, V value) {
        results.put(key, value);
    }

    public <T> T get(String key) {
        return (T) results.get(key);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public <T> T get(Class<T> klass) {
        return (T)results.get(klass.getCanonicalName());
    }
}
