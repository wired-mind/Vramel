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
import com.nxttxn.vramel.support.TypeConverterSupport;
import com.nxttxn.vramel.util.ObjectHelper;

import java.lang.reflect.Method;



/**
 * A type converter which is used to convert from String to enum type
 * @version
 */
public class EnumTypeConverter extends TypeConverterSupport {

    @SuppressWarnings("unchecked")
    public <T> T convertTo(Class<T> type, Exchange exchange, Object value) {
        if (type.isEnum() && value != null) {
            String text = value.toString();
            Method method;
            try {
                method = type.getMethod("valueOf", String.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeVramelException("Could not find valueOf method on enum type: " + type.getName());
            }
            return (T) ObjectHelper.invokeMethod(method, null, text);
        }
        return null;
    }

}