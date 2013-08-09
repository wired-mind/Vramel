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


import com.nxttxn.vramel.SyncProcessor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.VramelContextHelper;

/**
 * A constant (singleton) bean implementation of {@link BeanHolder}
 *
 * @version
 */
public class ConstantBeanHolder implements BeanHolder {
    private final Object bean;
    private final BeanInfo beanInfo;
    private final SyncProcessor processor;

    public ConstantBeanHolder(Object bean, BeanInfo beanInfo) {
        ObjectHelper.notNull(bean, "bean");
        ObjectHelper.notNull(beanInfo, "beanInfo");

        this.bean = bean;
        this.beanInfo = beanInfo;
        this.processor = VramelContextHelper.convertTo(beanInfo.getVramelContext(), SyncProcessor.class, bean);
    }

    public ConstantBeanHolder(Object bean, VramelContext context) {
        this(bean, new BeanInfo(context, bean.getClass()));
    }

    public ConstantBeanHolder(Object bean, VramelContext context, ParameterMappingStrategy parameterMappingStrategy) {
        this(bean, new BeanInfo(context, bean.getClass(), parameterMappingStrategy));
    }

    @Override
    public String toString() {
        // avoid invoke toString on bean as it may be a remote proxy
        return ObjectHelper.className(bean) + "(" + ObjectHelper.getIdentityHashCode(bean) + ")";
    }

    public Object getBean()  {
        return bean;
    }

    public SyncProcessor getProcessor() {
        return processor;
    }

    public BeanInfo getBeanInfo() {
        return beanInfo;
    }
}
