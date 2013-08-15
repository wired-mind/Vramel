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

import java.lang.reflect.Method;
import javax.xml.bind.annotation.XmlTransient;


import com.nxttxn.vramel.*;
import com.nxttxn.vramel.components.bean.BeanInfo;
import com.nxttxn.vramel.components.bean.BeanProcessor;
import com.nxttxn.vramel.components.bean.ProxyHelper;
import com.nxttxn.vramel.processor.UnitOfWorkProcessor;
import com.nxttxn.vramel.processor.UnitOfWorkProducer;
import com.nxttxn.vramel.util.IntrospectionSupport;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.ServiceHelper;
import com.nxttxn.vramel.util.VramelContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class for Camel based injector or post processing hooks which can be reused by
 * both the <a href="http://camel.apache.org/spring.html">Spring</a>,
 * <a href="http://camel.apache.org/guice.html">Guice</a> and
 * <a href="http://camel.apache.org/blueprint.html">Blueprint</a> support.
 *
 * @version
 */
public class CamelPostProcessorHelper implements VramelContextAware {
    private static final transient Logger LOG = LoggerFactory.getLogger(CamelPostProcessorHelper.class);

    @XmlTransient
    private VramelContext vramelContext;

    public CamelPostProcessorHelper() {
    }

    public CamelPostProcessorHelper(VramelContext vramelContext) {
        this.setVramelContext(vramelContext);
    }

    public VramelContext getVramelContext() {
        return vramelContext;
    }

    public void setVramelContext(VramelContext vramelContext) {
        this.vramelContext = vramelContext;
    }

    /**
     * Does the given context match this camel context
     */
    public boolean matchContext(String context) {
        if (ObjectHelper.isNotEmpty(context)) {
            if (!getVramelContext().getName().equals(context)) {
                return false;
            }
        }
        return true;
    }

    public void consumerInjection(Method method, Object bean, String beanName) {
        Consume consume = method.getAnnotation(Consume.class);
        if (consume != null && matchContext(consume.context())) {
            LOG.debug("Creating a consumer for: " + consume);
            subscribeMethod(method, bean, beanName, consume.uri(), consume.ref(), consume.property());
        }
    }

    public void subscribeMethod(Method method, Object bean, String beanName, String endpointUri, String endpointName, String endpointProperty) {
        // lets bind this method to a listener
        String injectionPointName = method.getName();
        Endpoint endpoint = getEndpointInjection(bean, endpointUri, endpointName, endpointProperty, injectionPointName, true);
        if (endpoint != null) {
            try {
                AsyncProcessor processor = createConsumerProcessor(bean, method, endpoint);
                Consumer consumer = endpoint.createConsumer(processor);
                LOG.debug("Created processor: {} for consumer: {}", processor, consumer);
                startService(consumer, bean, beanName);
            } catch (Exception e) {
                throw ObjectHelper.wrapRuntimeCamelException(e);
            }
        }
    }

    /**
     * Stats the given service
     */
    protected void startService(Service service, Object bean, String beanName) throws Exception {
        if (isSingleton(bean, beanName)) {
            getVramelContext().addService(service);
        } else {
            LOG.debug("Service is not singleton so you must remember to stop it manually {}", service);
            ServiceHelper.startService(service);
        }
    }

    /**
     * Create a processor which invokes the given method when an incoming
     * message exchange is received
     */
    protected AsyncProcessor createConsumerProcessor(final Object pojo, final Method method, final Endpoint endpoint) {
        BeanInfo info = new BeanInfo(getVramelContext(), method);
        BeanProcessor answer = new BeanProcessor(pojo, info);
        // must ensure the consumer is being executed in an unit of work so synchronization callbacks etc is invoked
        return new UnitOfWorkProcessor(answer);
    }

    public Endpoint getEndpointInjection(Object bean, String uri, String name, String propertyName,
                                         String injectionPointName, boolean mandatory) {
        if (ObjectHelper.isEmpty(uri) && ObjectHelper.isEmpty(name)) {
            // if no uri or ref, then fallback and try the endpoint property
            return doGetEndpointInjection(bean, propertyName, injectionPointName);
        } else {
            return doGetEndpointInjection(uri, name, injectionPointName, mandatory);
        }
    }

    private Endpoint doGetEndpointInjection(String uri, String name, String injectionPointName, boolean mandatory) {
        return VramelContextHelper.getEndpointInjection(getVramelContext(), uri, name, injectionPointName, mandatory);
    }

    /**
     * Gets the injection endpoint from a bean property.
     * @param bean the bean
     * @param propertyName the property name on the bean
     */
    private Endpoint doGetEndpointInjection(Object bean, String propertyName, String injectionPointName) {
        // fall back and use the method name if no explicit property name was given
        if (ObjectHelper.isEmpty(propertyName)) {
            propertyName = injectionPointName;
        }

        // we have a property name, try to lookup a getter method on the bean with that name using this strategy
        // 1. first the getter with the name as given
        // 2. then the getter with Endpoint as postfix
        // 3. then if start with on then try step 1 and 2 again, but omit the on prefix
        try {
            Object value = IntrospectionSupport.getOrElseProperty(bean, propertyName, null);
            if (value == null) {
                // try endpoint as postfix
                value = IntrospectionSupport.getOrElseProperty(bean, propertyName + "Endpoint", null);
            }
            if (value == null && propertyName.startsWith("on")) {
                // retry but without the on as prefix
                propertyName = propertyName.substring(2);
                return doGetEndpointInjection(bean, propertyName, injectionPointName);
            }
            if (value == null) {
                return null;
            } else if (value instanceof Endpoint) {
                return (Endpoint) value;
            } else {
                String uriOrRef = getVramelContext().getTypeConverter().mandatoryConvertTo(String.class, value);
                return getVramelContext().getEndpoint(uriOrRef);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error getting property " + propertyName + " from bean " + bean + " due " + e.getMessage(), e);
        }
    }

    /**
     * Creates the object to be injected for an {@link org.apache.camel.EndpointInject} or {@link org.apache.camel.Produce} injection point
     */
    public Object getInjectionValue(Class<?> type, String endpointUri, String endpointRef, String endpointProperty,
                                    String injectionPointName, Object bean, String beanName) {
        if (type.isAssignableFrom(ProducerTemplate.class)) {
            return createInjectionProducerTemplate(endpointUri, endpointRef, endpointProperty, injectionPointName, bean);
        } else {
            Endpoint endpoint = getEndpointInjection(bean, endpointUri, endpointRef, endpointProperty, injectionPointName, true);
            if (endpoint != null) {
                if (type.isInstance(endpoint)) {
                    return endpoint;
                } else if (type.isAssignableFrom(Producer.class)) {
                    return createInjectionProducer(endpoint, bean, beanName);
                } else if (type.isInterface()) {
                    // lets create a proxy
                    try {
                        return ProxyHelper.createProxy(endpoint, type);
                    } catch (Exception e) {
                        throw createProxyInstantiationRuntimeException(type, endpoint, e);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid type: " + type.getName()
                            + " which cannot be injected via @EndpointInject/@Produce for: " + endpoint);
                }
            }
            return null;
        }
    }

    /**
     * Factory method to create a {@link org.apache.camel.ProducerTemplate} to be injected into a POJO
     */
    protected ProducerTemplate createInjectionProducerTemplate(String endpointUri, String endpointRef, String endpointProperty,
                                                               String injectionPointName, Object bean) {
        // endpoint is optional for this injection point
        Endpoint endpoint = getEndpointInjection(bean, endpointUri, endpointRef, endpointProperty, injectionPointName, false);
        ProducerTemplate answer = new DefaultProducerTemplate(getVramelContext(), endpoint);
        // start the template so its ready to use
        try {
            answer.start();
        } catch (Exception e) {
            throw ObjectHelper.wrapRuntimeCamelException(e);
        }
        return answer;
    }

    /**
     * A Factory method to create a started {@link org.apache.camel.Producer} to be injected into a POJO
     */
    protected Producer createInjectionProducer(Endpoint endpoint, Object bean, String beanName) {
        try {
            Producer producer = endpoint.createProducer();
            startService(producer, bean, beanName);
            return new UnitOfWorkProducer(producer);
        } catch (Exception e) {
            throw ObjectHelper.wrapRuntimeCamelException(e);
        }
    }

    protected RuntimeException createProxyInstantiationRuntimeException(Class<?> type, Endpoint endpoint, Exception e) {
        return new ProxyInstantiationException(type, endpoint, e);
    }

    /**
     * Implementations can override this method to determine if the bean is singleton.
     *
     * @param bean the bean
     * @return <tt>true</tt> if its singleton scoped, for prototype scoped <tt>false</tt> is returned.
     */
    protected boolean isSingleton(Object bean, String beanName) {
        if (bean instanceof IsSingleton) {
            IsSingleton singleton = (IsSingleton) bean;
            return singleton.isSingleton();
        }
        return true;
    }
}
