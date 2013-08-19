/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nxttxn.vramel.impl.converter;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.RuntimeVramelException;
import com.nxttxn.vramel.TypeConverter;
import com.nxttxn.vramel.spi.TypeConverterAware;
import com.nxttxn.vramel.spi.TypeConverterRegistry;
import com.nxttxn.vramel.support.TypeConverterSupport;
import com.nxttxn.vramel.util.ObjectHelper;


import java.lang.reflect.Method;


/**
 * A {@link TypeConverter} implementation which instantiates an object
 * so that an instance method can be used as a type converter
 *
 * @version
 */
public class InstanceMethodTypeConverter extends TypeConverterSupport {
    private final CachingInjector<?> injector;
    private final Method method;
    private final boolean useExchange;
    private final TypeConverterRegistry registry;

    public InstanceMethodTypeConverter(CachingInjector<?> injector, Method method, TypeConverterRegistry registry) {
        this.injector = injector;
        this.method = method;
        this.useExchange = method.getParameterTypes().length == 2;
        this.registry = registry;
    }

    @Override
    public String toString() {
        return "InstanceMethodTypeConverter: " + method;
    }

    @SuppressWarnings("unchecked")
    public <T> T convertTo(Class<T> type, Exchange exchange, Object value) {
        Object instance = injector.newInstance();
        if (instance == null) {
            throw new RuntimeVramelException("Could not instantiate an instance of: " + type.getCanonicalName());
        }
        // inject parent type converter
        if (instance instanceof TypeConverterAware) {
            if (registry instanceof TypeConverter) {
                TypeConverter parentTypeConverter = (TypeConverter) registry;
                ((TypeConverterAware) instance).setTypeConverter(parentTypeConverter);
            }
        }
        return useExchange
                ? (T) ObjectHelper.invokeMethod(method, instance, value, exchange) : (T) ObjectHelper
                .invokeMethod(method, instance, value);
    }

}
