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
package com.nxttxn.vramel.util;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.spi.RouteStartupOrder;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import static com.nxttxn.vramel.util.ObjectHelper.isEmpty;
import static com.nxttxn.vramel.util.ObjectHelper.isNotEmpty;
import static com.nxttxn.vramel.util.ObjectHelper.notNull;


/**
 * A number of helper methods
 *
 * @version
 */
public final class VramelContextHelper {
    public static final String COMPONENT_DESCRIPTOR = "META-INF/services/org/apache/camel/component.properties";

    /**
     * Utility classes should not have a public constructor.
     */
    private VramelContextHelper() {
    }

    /**
     * Returns the mandatory endpoint for the given URI or the
     * {@link org.apache.camel.NoSuchEndpointException} is thrown
     */
    public static Endpoint getMandatoryEndpoint(VramelContext vramelContext, String uri)
            throws NoSuchEndpointException {
        Endpoint endpoint = vramelContext.getEndpoint(uri);
        if (endpoint == null) {
            throw new NoSuchEndpointException(uri);
        } else {
            return endpoint;
        }
    }

    /**
     * Returns the mandatory endpoint for the given URI and type or the
     * {@link org.apache.camel.NoSuchEndpointException} is thrown
     */
    public static <T extends Endpoint> T getMandatoryEndpoint(VramelContext vramelContext, String uri, Class<T> type) {
        Endpoint endpoint = getMandatoryEndpoint(vramelContext, uri);
        return ObjectHelper.cast(type, endpoint);
    }

    /**
     * Converts the given value to the requested type
     */
    public static <T> T convertTo(VramelContext context, Class<T> type, Object value) {
        notNull(context, "VramelContext");
        return context.getTypeConverter().convertTo(type, value);
    }

    /**
     * Converts the given value to the specified type throwing an {@link IllegalArgumentException}
     * if the value could not be converted to a non null value
     */
    public static <T> T mandatoryConvertTo(VramelContext context, Class<T> type, Object value) {
        T answer = convertTo(context, type, value);
        if (answer == null) {
            throw new IllegalArgumentException("Value " + value + " converted to " + type.getName() + " cannot be null");
        }
        return answer;
    }

    /**
     * Creates a new instance of the given type using the {@link org.apache.camel.spi.Injector} on the given
     * {@link VramelContext}
     */
    public static <T> T newInstance(VramelContext context, Class<T> beanType) {
        return context.getInjector().newInstance(beanType);
    }

    /**
     * Look up the given named bean in the {@link org.apache.camel.spi.Registry} on the
     * {@link VramelContext}
     */
    public static Object lookup(VramelContext context, String name) {
        return context.getRegistry().lookupByName(name);
    }

    /**
     * Look up the given named bean of the given type in the {@link org.apache.camel.spi.Registry} on the
     * {@link VramelContext}
     */
    public static <T> T lookup(VramelContext context, String name, Class<T> beanType) {
        return context.getRegistry().lookupByNameAndType(name, beanType);
    }

    /**
     * Look up the given named bean in the {@link org.apache.camel.spi.Registry} on the
     * {@link VramelContext} or throws {@link NoSuchBeanException} if not found.
     */
    public static Object mandatoryLookup(VramelContext context, String name) {
        Object answer = lookup(context, name);
        if (answer == null) {
            throw new NoSuchBeanException(name);
        }
        return answer;
    }

    /**
     * Look up the given named bean of the given type in the {@link org.apache.camel.spi.Registry} on the
     * {@link VramelContext} or throws NoSuchBeanException if not found.
     */
    public static <T> T mandatoryLookup(VramelContext context, String name, Class<T> beanType) {
        T answer = lookup(context, name, beanType);
        if (answer == null) {
            throw new NoSuchBeanException(name, beanType.getName());
        }
        return answer;
    }

    /**
     * Evaluates the @EndpointInject annotation using the given context
     */
    public static Endpoint getEndpointInjection(VramelContext VramelContext, String uri, String ref, String injectionPointName, boolean mandatory) {
        if (isNotEmpty(uri) && isNotEmpty(ref)) {
            throw new IllegalArgumentException("Both uri and name is provided, only either one is allowed: uri=" + uri + ", ref=" + ref);
        }

        Endpoint endpoint;
        if (isNotEmpty(uri)) {
            endpoint = VramelContext.getEndpoint(uri);
        } else {
            // if a ref is given then it should be possible to lookup
            // otherwise we do not catch situations where there is a typo etc
            if (isNotEmpty(ref)) {
                endpoint = mandatoryLookup(VramelContext, ref, Endpoint.class);
            } else {
                if (isEmpty(ref)) {
                    ref = injectionPointName;
                }
                if (mandatory) {
                    endpoint = mandatoryLookup(VramelContext, ref, Endpoint.class);
                } else {
                    endpoint = lookup(VramelContext, ref, Endpoint.class);
                }
            }
        }
        return endpoint;
    }

    /**
     * Gets the maximum cache pool size.
     * <p/>
     * Will use the property set on VramelContext with the key {@link Exchange#MAXIMUM_CACHE_POOL_SIZE}.
     * If no property has been set, then it will fallback to return a size of 1000.
     *
     * @param VramelContext the camel context
     * @return the maximum cache size
     * @throws IllegalArgumentException is thrown if the property is illegal
     */
    public static int getMaximumCachePoolSize(VramelContext VramelContext) throws IllegalArgumentException {
        if (VramelContext != null) {
            String s = VramelContext.getProperty(Exchange.MAXIMUM_CACHE_POOL_SIZE);
            if (s != null) {
                try {
                    // we cannot use Camel type converters as they may not be ready this early
                    Integer size = Integer.valueOf(s);
                    if (size == null || size <= 0) {
                        throw new IllegalArgumentException("Property " + Exchange.MAXIMUM_CACHE_POOL_SIZE + " must be a positive number, was: " + s);
                    }
                    return size;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Property " + Exchange.MAXIMUM_CACHE_POOL_SIZE + " must be a positive number, was: " + s, e);
                }
            }
        }

        // 1000 is the default fallback
        return 1000;
    }

    /**
     * Gets the maximum endpoint cache size.
     * <p/>
     * Will use the property set on VramelContext with the key {@link Exchange#MAXIMUM_ENDPOINT_CACHE_SIZE}.
     * If no property has been set, then it will fallback to return a size of 1000.
     *
     * @param VramelContext the camel context
     * @return the maximum cache size
     * @throws IllegalArgumentException is thrown if the property is illegal
     */
    public static int getMaximumEndpointCacheSize(VramelContext VramelContext) throws IllegalArgumentException {
        if (VramelContext != null) {
            String s = VramelContext.getProperty(Exchange.MAXIMUM_ENDPOINT_CACHE_SIZE);
            if (s != null) {
                // we cannot use Camel type converters as they may not be ready this early
                try {
                    Integer size = Integer.valueOf(s);
                    if (size == null || size <= 0) {
                        throw new IllegalArgumentException("Property " + Exchange.MAXIMUM_ENDPOINT_CACHE_SIZE + " must be a positive number, was: " + s);
                    }
                    return size;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Property " + Exchange.MAXIMUM_ENDPOINT_CACHE_SIZE + " must be a positive number, was: " + s, e);
                }
            }
        }

        // 1000 is the default fallback
        return 1000;
    }

    /**
     * Parses the given text and handling property placeholders as well
     *
     * @param VramelContext the camel context
     * @param text  the text
     * @return the parsed text, or <tt>null</tt> if the text was <tt>null</tt>
     * @throws Exception is thrown if illegal argument
     */
    public static String parseText(VramelContext VramelContext, String text) throws Exception {
        // ensure we support property placeholders
        return VramelContext.resolvePropertyPlaceholders(text);
    }

    /**
     * Parses the given text and converts it to an Integer and handling property placeholders as well
     *
     * @param VramelContext the camel context
     * @param text  the text
     * @return the integer vale, or <tt>null</tt> if the text was <tt>null</tt>
     * @throws Exception is thrown if illegal argument or type conversion not possible
     */
    public static Integer parseInteger(VramelContext VramelContext, String text) throws Exception {
        // ensure we support property placeholders
        String s = VramelContext.resolvePropertyPlaceholders(text);
        if (s != null) {
            try {
                return VramelContext.getTypeConverter().mandatoryConvertTo(Integer.class, s);
            } catch (NumberFormatException e) {
                if (s.equals(text)) {
                    throw new IllegalArgumentException("Error parsing [" + s + "] as an Integer.", e);
                } else {
                    throw new IllegalArgumentException("Error parsing [" + s + "] from property " + text + " as an Integer.", e);
                }
            }
        }
        return null;
    }

    /**
     * Parses the given text and converts it to an Long and handling property placeholders as well
     *
     * @param VramelContext the camel context
     * @param text  the text
     * @return the long vale, or <tt>null</tt> if the text was <tt>null</tt>
     * @throws Exception is thrown if illegal argument or type conversion not possible
     */
    public static Long parseLong(VramelContext VramelContext, String text) throws Exception {
        // ensure we support property placeholders
        String s = VramelContext.resolvePropertyPlaceholders(text);
        if (s != null) {
            try {
                return VramelContext.getTypeConverter().mandatoryConvertTo(Long.class, s);
            } catch (NumberFormatException e) {
                if (s.equals(text)) {
                    throw new IllegalArgumentException("Error parsing [" + s + "] as a Long.", e);
                } else {
                    throw new IllegalArgumentException("Error parsing [" + s + "] from property " + text + " as a Long.", e);
                }
            }
        }
        return null;
    }

    /**
     * Parses the given text and converts it to a Double and handling property placeholders as well
     *
     * @param VramelContext the camel context
     * @param text  the text
     * @return the double vale, or <tt>null</tt> if the text was <tt>null</tt>
     * @throws Exception is thrown if illegal argument or type conversion not possible
     */
    public static Double parseDouble(VramelContext VramelContext, String text) throws Exception {
        // ensure we support property placeholders
        String s = VramelContext.resolvePropertyPlaceholders(text);
        if (s != null) {
            try {
                return VramelContext.getTypeConverter().mandatoryConvertTo(Double.class, s);
            } catch (NumberFormatException e) {
                if (s.equals(text)) {
                    throw new IllegalArgumentException("Error parsing [" + s + "] as an Integer.", e);
                } else {
                    throw new IllegalArgumentException("Error parsing [" + s + "] from property " + text + " as an Integer.", e);
                }
            }
        }
        return null;
    }

    /**
     * Parses the given text and converts it to an Boolean and handling property placeholders as well
     *
     * @param VramelContext the camel context
     * @param text  the text
     * @return the boolean vale, or <tt>null</tt> if the text was <tt>null</tt>
     * @throws Exception is thrown if illegal argument or type conversion not possible
     */
    public static Boolean parseBoolean(VramelContext VramelContext, String text) throws Exception {
        // ensure we support property placeholders
        String s = VramelContext.resolvePropertyPlaceholders(text);
        if (s != null) {
            s = s.trim().toLowerCase(Locale.ENGLISH);
            if (s.equals("true") || s.equals("false")) {
                return "true".equals(s) ? Boolean.TRUE : Boolean.FALSE;
            } else {
                if (s.equals(text)) {
                    throw new IllegalArgumentException("Error parsing [" + s + "] as a Boolean.");
                } else {
                    throw new IllegalArgumentException("Error parsing [" + s + "] from property " + text + " as a Boolean.");
                }
            }
        }
        return null;
    }

    /**
     * Finds all possible Components on the classpath and Registry
     */
    public static SortedMap<String, Properties> findComponents(VramelContext VramelContext) throws LoadPropertiesException {
        SortedMap<String, Properties> map = new TreeMap<String, Properties>();
        Enumeration<URL> iter = VramelContext.getClassResolver().loadResourcesAsURL(COMPONENT_DESCRIPTOR);
        while (iter.hasMoreElements()) {
            URL url = iter.nextElement();
            try {
                Properties properties = new Properties();
                properties.load(url.openStream());
                String names = properties.getProperty("components");
                if (names != null) {
                    StringTokenizer tok = new StringTokenizer(names);
                    while (tok.hasMoreTokens()) {
                        String name = tok.nextToken();
                        map.put(name, properties);
                    }
                }
            } catch (IOException e) {
                throw new LoadPropertiesException(url, e);
            }
        }

        // lets see what other components are in the registry
        Map<String, Component> beanMap = VramelContext.getRegistry().findByTypeWithName(Component.class);
        Set<Map.Entry<String, Component>> entries = beanMap.entrySet();
        for (Map.Entry<String, Component> entry : entries) {
            String name = entry.getKey();
            if (!map.containsKey(name)) {
                Properties properties = new Properties();
                Component component = entry.getValue();
                if (component != null) {
                    properties.put("component", component);
                    properties.put("class", component.getClass().getName());
                    map.put(name, properties);
                }
            }
        }
        return map;
    }

//    Notsupported yet
//    /**
//     * Gets the route startup order for the given route id
//     *
//     * @param VramelContext  the camel context
//     * @param routeId       the id of the route
//     * @return the startup order, or <tt>0</tt> if not possible to determine
//     */
//    public static int getRouteStartupOrder(VramelContext VramelContext, String routeId) {
//        for (RouteStartupOrder order : VramelContext.getRouteStartupOrder()) {
//            if (order.getFlow().getId().equals(routeId)) {
//                return order.getStartupOrder();
//            }
//        }
//        return 0;
//    }

}
