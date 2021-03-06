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
package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.NoFactoryAvailableException;
import com.nxttxn.vramel.spi.ClassResolver;
import com.nxttxn.vramel.spi.FactoryFinder;
import com.nxttxn.vramel.spi.Injector;
import com.nxttxn.vramel.util.CastUtils;
import com.nxttxn.vramel.util.IOHelper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;



/**
 * Default factory finder.
 */
public class DefaultFactoryFinder implements FactoryFinder {

    protected final ConcurrentMap<String, Class<?>> classMap = new ConcurrentHashMap<String, Class<?>>();
    private final ClassResolver classResolver;
    private final String path;

    public DefaultFactoryFinder(ClassResolver classResolver, String resourcePath) {
        this.classResolver = classResolver;
        this.path = resourcePath;
    }

    public String getResourcePath() {
        return path;
    }

    public Object newInstance(String key) throws NoFactoryAvailableException {
        try {
            return newInstance(key, null);
        } catch (Exception e) {
            throw new NoFactoryAvailableException(key, e);
        }
    }

    public <T> List<T> newInstances(String key, Injector injector, Class<T> type) throws ClassNotFoundException, IOException {
        List<Class<T>> list = CastUtils.cast(findClasses(key));
        List<T> answer = new ArrayList<T>(list.size());
        answer.add(newInstance(key, injector, type));
        return answer;
    }

    public Class<?> findClass(String key) throws ClassNotFoundException, IOException {
        return findClass(key, null);
    }

    public Class<?> findClass(String key, String propertyPrefix) throws ClassNotFoundException, IOException {
        String prefix = propertyPrefix != null ? propertyPrefix : "";

        Class<?> clazz = classMap.get(prefix + key);
        if (clazz == null) {
            clazz = newInstance(doFindFactoryProperties(key), prefix);
            if (clazz != null) {
                classMap.put(prefix + key, clazz);
            }
        }
        return clazz;
    }

    public Class<?> findClass(String key, String propertyPrefix, Class<?> clazz) throws ClassNotFoundException, IOException {
        // Just ignore clazz which is only useful for OSGiFactoryFinder
        return findClass(key, propertyPrefix);
    }

    private Object newInstance(String key, String propertyPrefix) throws IllegalAccessException,
            InstantiationException, IOException, ClassNotFoundException {
        Class<?> clazz = findClass(key, propertyPrefix);
        return clazz.newInstance();
    }

    private <T> T newInstance(String key, Injector injector, Class<T> expectedType) throws IOException,
            ClassNotFoundException {
        return newInstance(key, injector, null, expectedType);
    }

    private <T> T newInstance(String key, Injector injector, String propertyPrefix, Class<T> expectedType)
            throws IOException, ClassNotFoundException {
        Class<?> type = findClass(key, propertyPrefix);
        Object value = injector.newInstance(type);
        if (expectedType.isInstance(value)) {
            return expectedType.cast(value);
        } else {
            throw new ClassCastException("Not instanceof " + expectedType.getName() + " value: " + value);
        }
    }

    private List<Class<?>> findClasses(String key) throws ClassNotFoundException, IOException {
        return findClasses(key, null);
    }

    private List<Class<?>> findClasses(String key, String propertyPrefix) throws ClassNotFoundException, IOException {
        Class<?> type = findClass(key, propertyPrefix);
        return Collections.<Class<?>>singletonList(type);
    }

    private Class<?> newInstance(Properties properties, String propertyPrefix) throws ClassNotFoundException, IOException {
        String className = properties.getProperty(propertyPrefix + "class");
        if (className == null) {
            throw new IOException("Expected property is missing: " + propertyPrefix + "class");
        }

        Class<?> clazz = classResolver.resolveClass(className);
        if (clazz == null) {
            throw new ClassNotFoundException(className);
        }
        return clazz;
    }

    private Properties doFindFactoryProperties(String key) throws IOException {
        String uri = path + key;

        InputStream in = classResolver.loadResourceAsStream(uri);
        if (in == null) {
            throw new NoFactoryAvailableException(uri);
        }

        // lets load the file
        BufferedInputStream reader = null;
        try {
            reader = IOHelper.buffered(in);
            Properties properties = new Properties();
            properties.load(reader);
            return properties;
        } finally {
            IOHelper.close(reader, key, null);
            IOHelper.close(in, key, null);
        }
    }
}
