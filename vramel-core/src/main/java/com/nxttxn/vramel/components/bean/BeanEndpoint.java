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


import com.nxttxn.vramel.Component;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.impl.ProcessorEndpoint;
import com.nxttxn.vramel.spi.UriEndpoint;
import com.nxttxn.vramel.spi.UriParam;

/**
 * Endpoint for the bean component.
 *
 * @version
 */
@UriEndpoint(scheme = "bean")
public class BeanEndpoint extends ProcessorEndpoint {
    @UriParam
    private boolean cache;
    @UriParam
    private boolean multiParameterArray;
    @UriParam
    private String beanName;
    @UriParam
    private String method;
    private BeanHolder beanHolder;

    public BeanEndpoint() {
        init();
    }

    private void init() {

    }

    public BeanEndpoint(String endpointUri, Component component, BeanProcessor processor) {
        super(endpointUri, component, processor);
        init();
    }

    public BeanEndpoint(String endpointUri, Component component) {
        super(endpointUri, component);
        init();
    }

    // Properties
    //-------------------------------------------------------------------------

    public String getBeanName() {
        return beanName;
    }

    /**
     * Sets the name of the bean to invoke
     */
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public boolean isMultiParameterArray() {
        return multiParameterArray;
    }

    public void setMultiParameterArray(boolean mpArray) {
        multiParameterArray = mpArray;
    }

    public boolean isCache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }

    public String getMethod() {
        return method;
    }

    /**
     * Sets the name of the method to invoke on the bean
     */
    public void setMethod(String method) {
        this.method = method;
    }

    public BeanHolder getBeanHolder() {
        return beanHolder;
    }

    public void setBeanHolder(BeanHolder beanHolder) {
        this.beanHolder = beanHolder;
    }


    @Override
    protected Processor createProcessor() throws Exception {
        BeanHolder holder = getBeanHolder();
        if (holder == null) {
            RegistryBean registryBean = new RegistryBean(getVramelContext(), beanName);
            if (cache) {
                holder = registryBean.createCacheHolder();
            } else {
                holder = registryBean;
            }
        }
        BeanProcessor processor = new BeanProcessor(holder);
        if (method != null) {
            processor.setMethod(method);
        }
        processor.setMultiParameterArray(isMultiParameterArray());

        return processor;
    }
}
