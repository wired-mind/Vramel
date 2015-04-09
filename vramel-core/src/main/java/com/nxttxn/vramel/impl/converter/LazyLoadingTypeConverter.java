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
import com.nxttxn.vramel.TypeConverter;
import com.nxttxn.vramel.spi.FactoryFinder;
import com.nxttxn.vramel.spi.Injector;
import com.nxttxn.vramel.spi.PackageScanClassResolver;
import com.nxttxn.vramel.util.ObjectHelper;
import org.vertx.java.core.AsyncResultHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;



/**
 * Lazy implementation of a type converter registry used for
 * <a href="http://camel.apache.org/type-converter.html">type converters</a> in Camel.
 * <p/>
 * This implementation will lazy load type converters on-demand.
 *
 * @version
 * @deprecated will be removed in a future Camel release.
 */
@Deprecated
public class LazyLoadingTypeConverter extends BaseTypeConverterRegistry {
    private final AtomicBoolean loaded = new AtomicBoolean();

    public LazyLoadingTypeConverter(PackageScanClassResolver resolver, Injector injector, FactoryFinder factoryFinder) {
        super(resolver, injector, factoryFinder);
    }

    @Override
    protected Object doConvertTo(final Class<?> type, final Exchange exchange, final Object value, boolean tryConvert) {
        Object answer = super.doConvertTo(type, exchange, value, tryConvert);
        if (answer == null && !loaded.get()) {
            // okay we could not convert, so try again, but load the converters up front
            ensureLoaded();
            answer = super.doConvertTo(type, exchange, value, tryConvert);
        }
        return answer;
    }

    @Override
    public TypeConverter getTypeConverter(Class<?> toType, Class<?> fromType) {
        TypeConverter answer = super.getTypeConverter(toType, fromType);
        if (answer == null && !loaded.get()) {
            // okay we could not convert, so try again, but load the converters up front
            ensureLoaded();
            answer = super.getTypeConverter(toType, fromType);
        }
        return answer;
    }

    @Override
    public Set<Class<?>> getFromClassMappings() {
        if (!loaded.get()) {
            ensureLoaded();
        }
        return super.getFromClassMappings();
    }

    @Override
    public Map<Class<?>, TypeConverter> getToClassMappings(Class<?> fromClass) {
        if (!loaded.get()) {
            ensureLoaded();
        }
        return super.getToClassMappings(fromClass);
    }

    @Override
    public Map<TypeMapping, TypeConverter> getTypeMappings() {
        if (!loaded.get()) {
            ensureLoaded();
        }
        return super.getTypeMappings();
    }

    @Override
    protected TypeConverter doLookup(Class<?> toType, Class<?> fromType, boolean isSuper) {
        TypeConverter answer = super.doLookup(toType, fromType, isSuper);
        if (answer == null && !loaded.get()) {
            // okay we could not convert, so try again, but load the converters up front
            ensureLoaded();
            answer = super.doLookup(toType, fromType, isSuper);
        }
        return answer;
    }

    private synchronized void ensureLoaded() {
        if (loaded.compareAndSet(false, true)) {
            try {
                super.loadTypeConverters();
            } catch (Exception e) {
                throw ObjectHelper.wrapRuntimeCamelException(e);
            }
        }
    }

    @Override
    protected void doStart(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
        super.doStart(asyncResultHandler);
        // must load core type converters
        loadCoreTypeConverters();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        // reset loaded flag
        loaded.set(false);
    }
}
