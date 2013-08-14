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
import com.nxttxn.vramel.support.TypeConverterSupport;
import org.apache.camel.util.ObjectHelper;

import java.lang.reflect.Method;



/**
 * A {@link com.nxttxn.vramel.TypeConverter} implementation which invokes a static method to convert from a type to another type
 *
 * @version
 */
public class StaticMethodTypeConverter extends TypeConverterSupport {
    private final Method method;
    private final boolean useExchange;

    public StaticMethodTypeConverter(Method method) {
        this.method = method;
        this.useExchange = method.getParameterTypes().length == 2;
    }

    @Override
    public String toString() {
        return "StaticMethodTypeConverter: " + method;
    }

    @SuppressWarnings("unchecked")
    public <T> T convertTo(Class<T> type, Exchange exchange, Object value) {
        return useExchange ? (T) ObjectHelper.invokeMethod(method, null, value, exchange)
                : (T)ObjectHelper.invokeMethod(method, null, value);
    }

}
