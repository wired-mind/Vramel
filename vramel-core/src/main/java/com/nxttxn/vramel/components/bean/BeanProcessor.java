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


import com.nxttxn.vramel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link com.nxttxn.vramel.AsyncProcessor} which converts the inbound exchange to a method
 * invocation on a POJO
 *
 * @version
 */
public class BeanProcessor implements Processor {
    private static final transient Logger LOG = LoggerFactory.getLogger(BeanProcessor.class);

    private boolean multiParameterArray;
    private String method;
    private BeanHolder beanHolder;
    private boolean shorthandMethod;

    public BeanProcessor(Object pojo, BeanInfo beanInfo) {
        this(new ConstantBeanHolder(pojo, beanInfo));
    }

    public BeanProcessor(Object pojo, VramelContext vramelContext, ParameterMappingStrategy parameterMappingStrategy) {
        this(pojo, new BeanInfo(vramelContext, pojo.getClass(), parameterMappingStrategy));
    }

    public BeanProcessor(Object pojo, VramelContext vramelContext) {
        this(pojo, vramelContext, BeanInfo.createParameterMappingStrategy(vramelContext));
    }

    public BeanProcessor(BeanHolder beanHolder) {
        this.beanHolder = beanHolder;
    }

    @Override
    public String toString() {
        return "BeanProcessor[" + beanHolder + "]";
    }



    public void process(Exchange exchange) {
        // do we have an explicit method name we always should invoke (either configured on endpoint or as a header)
        String explicitMethodName = exchange.getIn().getHeader(Exchange.BEAN_METHOD_NAME, method, String.class);

        Object bean;
        BeanInfo beanInfo;
        try {
            bean = beanHolder.getBean();
            beanInfo = beanHolder.getBeanInfo();
        } catch (Throwable e) {
            exchange.setException(e);
            return;
        }

        // do we have a custom adapter for this POJO to a AsyncProcessor
        // but only do this if allowed
        if (allowProcessor(explicitMethodName, beanInfo)) {
            Processor processor = getProcessor();
            if (processor != null) {
                LOG.trace("Using a custom adapter as bean invocation: {}", processor);
                try {
                    processor.process(exchange);
                } catch (Throwable e) {
                    exchange.setException(e);
                }
                return;
            }
        }

        Message in = exchange.getIn();

        // is the message proxied using a BeanInvocation?
        BeanInvocation beanInvoke = null;
        if (in.getBody() != null && in.getBody() instanceof BeanInvocation) {
            // BeanInvocation would be stored directly as the message body
            // do not force any type conversion attempts as it would just be unnecessary and cost a bit performance
            // so a regular instanceof check is sufficient
            beanInvoke = (BeanInvocation) in.getBody();
        }
        if (beanInvoke != null) {
            // Now it gets a bit complicated as ProxyHelper can proxy beans which we later
            // intend to invoke (for example to proxy and invoke using spring remoting).
            // and therefore the message body contains a BeanInvocation object.
            // However this can causes problem if we in a Camel route invokes another bean,
            // so we must test whether BeanHolder and BeanInvocation is the same bean or not
            LOG.trace("Exchange IN body is a BeanInvocation instance: {}", beanInvoke);
            Class<?> clazz = beanInvoke.getMethod().getDeclaringClass();
            boolean sameBean = clazz.isInstance(bean);
            if (LOG.isDebugEnabled()) {
                LOG.debug("BeanHolder bean: {} and beanInvocation bean: {} is same instance: {}", new Object[]{bean.getClass(), clazz, sameBean});
            }
            if (sameBean) {
                beanInvoke.invoke(bean, exchange);
                // propagate headers
                exchange.getOut().getHeaders().putAll(exchange.getIn().getHeaders());
                return;
            }
        }

        // set temporary header which is a hint for the bean info that introspect the bean
        if (in.getHeader(Exchange.BEAN_MULTI_PARAMETER_ARRAY) == null) {
            in.setHeader(Exchange.BEAN_MULTI_PARAMETER_ARRAY, isMultiParameterArray());
        }

        MethodInvocation invocation;
        // set explicit method name to invoke as a header, which is how BeanInfo can detect it
        if (explicitMethodName != null) {
            in.setHeader(Exchange.BEAN_METHOD_NAME, explicitMethodName);
        }
        try {
            invocation = beanInfo.createInvocation(bean, exchange);
        } catch (Throwable e) {
            exchange.setException(e);
            return;
        } finally {
            // must remove headers as they were provisional
            in.removeHeader(Exchange.BEAN_MULTI_PARAMETER_ARRAY);
            in.removeHeader(Exchange.BEAN_METHOD_NAME);
        }

        if (invocation == null) {
            exchange.setException(new IllegalStateException("No method invocation could be created, no matching method could be found on: " + bean));
            return;
        }

        // invoke invocation
        invocation.proceed();
    }

    protected Processor getProcessor() {
        return beanHolder.getProcessor();
    }

    public Object getBean() {
        return beanHolder.getBean();
    }

    // Properties
    // -----------------------------------------------------------------------

    public String getMethod() {
        return method;
    }

    public boolean isMultiParameterArray() {
        return multiParameterArray;
    }

    public void setMultiParameterArray(boolean mpArray) {
        multiParameterArray = mpArray;
    }

    /**
     * Sets the method name to use
     */
    public void setMethod(String method) {
        this.method = method;
    }

    public boolean isShorthandMethod() {
        return shorthandMethod;
    }

    /**
     * Sets whether to support getter style method name, so you can
     * say the method is called 'name' but it will invoke the 'getName' method.
     * <p/>
     * Is by default turned off.
     */
    public void setShorthandMethod(boolean shorthandMethod) {
        this.shorthandMethod = shorthandMethod;
    }



    private boolean allowProcessor(String explicitMethodName, BeanInfo info) {
        if (explicitMethodName != null) {
            // don't allow if explicit method name is given, as we then must invoke this method
            return false;
        }

        // don't allow if any of the methods has a @Handler annotation
        // as the @Handler annotation takes precedence and is supposed to trigger invocation
        // of the given method
        for (MethodInfo method : info.getMethods()) {
            if (method.hasHandlerAnnotation()) {
                return false;
            }
        }

        // fallback and allow using the processor
        return true;
    }
}
