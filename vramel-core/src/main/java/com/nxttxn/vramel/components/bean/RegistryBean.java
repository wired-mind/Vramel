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
package com.nxttxn.vramel.components.bean;


import com.nxttxn.vramel.NoSuchBeanException;
import com.nxttxn.vramel.SyncProcessor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.spi.Registry;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.VramelContextHelper;

/**
 * An implementation of a {@link BeanHolder} which will look up a bean from the registry and act as a cache of its metadata
 *
 * @version
 */
public class RegistryBean implements BeanHolder {
    private final VramelContext context;
    private final String name;
    private final Registry registry;
    private SyncProcessor processor;
    private BeanInfo beanInfo;
    private Object bean;
    private ParameterMappingStrategy parameterMappingStrategy;

    public RegistryBean(VramelContext context, String name) {
        this.context = context;
        this.name = name;
        this.registry = context.getRegistry();
    }

    public RegistryBean(Registry registry, VramelContext context, String name) {
        this.registry = registry;
        this.context = context;
        this.name = name;
    }

    @Override
    public String toString() {
        return "bean: " + name;
    }

    public ConstantBeanHolder createCacheHolder() throws Exception {
        return new ConstantBeanHolder(getBean(), getBeanInfo());
    }

    public synchronized Object getBean() throws NoSuchBeanException {
        Object value = lookupBean();
        if (value == null) {
            // maybe its a class
            value = context.getClassResolver().resolveClass(name);
            if (value == null) {
                // no its not a class then we cannot find the bean
                throw new NoSuchBeanException(name);
            }
        }
        if (value != bean) {
            if (!ObjectHelper.equal(ObjectHelper.type(bean), ObjectHelper.type(value))) {
                beanInfo = null;
            }
            bean = value;
            processor = null;

            // could be a class then create an instance of it
            if (bean instanceof Class) {
                // bean is a class so create an instance of it
                bean = context.getInjector().newInstance((Class<?>)bean);
                value = bean;
            }
        }
        return value;
    }

    public SyncProcessor getProcessor() {
        if (processor == null && bean != null) {
            processor = VramelContextHelper.convertTo(context, SyncProcessor.class, bean);
        }
        return processor;
    }

    public BeanInfo getBeanInfo() {
        if (beanInfo == null && bean != null) {
            this.beanInfo = createBeanInfo();
        }
        return beanInfo;
    }

    public String getName() {
        return name;
    }

    public Registry getRegistry() {
        return registry;
    }

    public VramelContext getContext() {
        return context;
    }

    public ParameterMappingStrategy getParameterMappingStrategy() {
        if (parameterMappingStrategy == null) {
            parameterMappingStrategy = createParameterMappingStrategy();
        }
        return parameterMappingStrategy;
    }

    public void setParameterMappingStrategy(ParameterMappingStrategy parameterMappingStrategy) {
        this.parameterMappingStrategy = parameterMappingStrategy;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected BeanInfo createBeanInfo() {
        return new BeanInfo(context, bean.getClass(), getParameterMappingStrategy());
    }

    protected ParameterMappingStrategy createParameterMappingStrategy() {
        return BeanInfo.createParameterMappingStrategy(context);
    }

    protected Object lookupBean() {
        return registry.lookupByName(name);
    }
}