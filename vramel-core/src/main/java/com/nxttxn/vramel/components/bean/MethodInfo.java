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
import com.nxttxn.vramel.processor.DynamicRouter;
import com.nxttxn.vramel.processor.RoutingSlip;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.support.ExpressionAdapter;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.StringHelper;
import org.apache.camel.util.StringQuoteHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static org.apache.camel.util.ObjectHelper.asString;


/**
 * Information about a method to be used for invocation.
 *
 * @version
 */
public class MethodInfo {
    private static final transient Logger LOG = LoggerFactory.getLogger(MethodInfo.class);

    private VramelContext vramelContext;
    private Class<?> type;
    private Method method;
    private final List<ParameterInfo> parameters;
    private final List<ParameterInfo> bodyParameters;
    private final boolean hasCustomAnnotation;
    private final boolean hasHandlerAnnotation;
    private Expression parametersExpression;
    private ExchangePattern pattern = ExchangePattern.InOut;
//    private RecipientList recipientList;
    private RoutingSlip routingSlip;
    private DynamicRouter dynamicRouter;

    /**
     * Adapter to invoke the method which has been annotated with the @DynamicRouter
     */
    private final class DynamicRouterExpression extends ExpressionAdapter {
        private final Object pojo;

        private DynamicRouterExpression(Object pojo) {
            this.pojo = pojo;
        }

        @Override
        public Object evaluate(Exchange exchange) {
            // evaluate arguments on each invocation as the parameters can have changed/updated since last invocation
            final Object[] arguments = parametersExpression.evaluate(exchange, Object[].class);
            try {
                return invoke(method, pojo, arguments, exchange);
            } catch (Exception e) {
                throw ObjectHelper.wrapRuntimeCamelException(e);
            }
        }

        @Override
        public String toString() {
            return "DynamicRouter[invoking: " + method + " on bean: " + pojo + "]";
        }
    }

    @SuppressWarnings("deprecation")
    public MethodInfo(VramelContext vramelContext, Class<?> type, Method method, List<ParameterInfo> parameters, List<ParameterInfo> bodyParameters,
                      boolean hasCustomAnnotation, boolean hasHandlerAnnotation) {
        this.vramelContext = vramelContext;
        this.type = type;
        this.method = method;
        this.parameters = parameters;
        this.bodyParameters = bodyParameters;
        this.hasCustomAnnotation = hasCustomAnnotation;
        this.hasHandlerAnnotation = hasHandlerAnnotation;
        this.parametersExpression = createParametersExpression();

        Map<Class<?>, Annotation> collectedMethodAnnotation = collectMethodAnnotations(type, method);

    }

    private Map<Class<?>, Annotation> collectMethodAnnotations(Class<?> c, Method method) {
        Map<Class<?>, Annotation> annotations = new HashMap<Class<?>, Annotation>();
        collectMethodAnnotations(c, method, annotations);
        return annotations;
    }

    private void collectMethodAnnotations(Class<?> c, Method method, Map<Class<?>, Annotation> annotations) {
        for (Class<?> i : c.getInterfaces()) {
            collectMethodAnnotations(i, method, annotations);
        }
        if (!c.isInterface() && c.getSuperclass() != null) {
            collectMethodAnnotations(c.getSuperclass(), method, annotations);
        }
        // make sure the sub class can override the definition
        try {
            Annotation[] ma = c.getDeclaredMethod(method.getName(), method.getParameterTypes()).getAnnotations();
            for (Annotation a : ma) {
                annotations.put(a.annotationType(), a);
            }
        } catch (SecurityException e) {
            // do nothing here
        } catch (NoSuchMethodException e) {
            // do nothing here
        }
    }
//
//    /**
//     * Does the given context match this camel context
//     */
//    private boolean matchContext(String context) {
//        if (ObjectHelper.isNotEmpty(context)) {
//            if (!vramelContext.getName().equals(context)) {
//                return false;
//            }
//        }
//        return true;
//    }

    public String toString() {
        return method.toString();
    }

    public MethodInvocation createMethodInvocation(final Object pojo, final Exchange exchange) {
        final Object[] arguments = parametersExpression.evaluate(exchange, Object[].class);
        return new MethodInvocation() {
            public Method getMethod() {
                return method;
            }

            public Object[] getArguments() {
                return arguments;
            }

            public void proceed() {
                try {
                    doProceed();
                } catch (InvocationTargetException e) {
                    exchange.setException(e.getTargetException());
                } catch (Throwable e) {
                    exchange.setException(e);
                }
            }

            private void doProceed() throws Exception {
                // dynamic router should be invoked beforehand
//                if (dynamicRouter != null) {
//
//                    // use a expression which invokes the method to be used by dynamic router
//                    Expression expression = new DynamicRouterExpression(pojo);
//                    dynamicRouter.doRoutingSlip(exchange, expression, optionalAsyncResultHandler);
//                    return;
//                }

                // invoke pojo
                if (LOG.isTraceEnabled()) {
                    LOG.trace(">>>> invoking: {} on bean: {} with arguments: {} for exchange: {}", new Object[]{method, pojo, asString(arguments), exchange});
                }
                Object result = invoke(method, pojo, arguments, exchange);

//                if (recipientList != null) {
//                    // ensure its started
//                    if (!recipientList.isStarted()) {
//                        ServiceHelper.startService(recipientList);
//                    }
//                    return recipientList.sendToRecipientList(exchange, result, callback);
//                }
//                if (routingSlip != null) {
//                    routingSlip.doRoutingSlip(exchange, result, optionalAsyncResultHandler);
//                    return;
//                }

                // if the method returns something then set the value returned on the Exchange
                if (!getMethod().getReturnType().equals(Void.TYPE) && result != Void.TYPE) {
//                    if (exchange.getPattern().isOutCapable()) {
                        // force out creating if not already created (as its lazy)
                        LOG.debug("Setting bean invocation result on the OUT message: {}", result);
                        exchange.getOut().setBody(result);
                        // propagate headers
                        exchange.getOut().getHeaders().putAll(exchange.getIn().getHeaders());
//                    } else {
//                        // if not out then set it on the in
//                        LOG.debug("Setting bean invocation result on the IN message: {}", result);
//                        exchange.getIn().setBody(result);
//                    }
                }


            }

            public Object getThis() {
                return pojo;
            }

            public AccessibleObject getStaticPart() {
                return method;
            }
        };
    }

    public Class<?> getType() {
        return type;
    }

    public Method getMethod() {
        return method;
    }

    /**
     * Returns the {@link org.apache.camel.ExchangePattern} that should be used when invoking this method. This value
     * defaults to {@link org.apache.camel.ExchangePattern#InOut} unless some {@link org.apache.camel.Pattern} annotation is used
     * to override the message exchange pattern.
     *
     * @return the exchange pattern to use for invoking this method.
     */
    public ExchangePattern getPattern() {
        return pattern;
    }

    public Expression getParametersExpression() {
        return parametersExpression;
    }

    public List<ParameterInfo> getBodyParameters() {
        return bodyParameters;
    }

    public Class<?> getBodyParameterType() {
        if (bodyParameters.isEmpty()) {
            return null;
        }
        ParameterInfo parameterInfo = bodyParameters.get(0);
        return parameterInfo.getType();
    }

    public boolean bodyParameterMatches(Class<?> bodyType) {
        Class<?> actualType = getBodyParameterType();
        return actualType != null && ObjectHelper.isAssignableFrom(bodyType, actualType);
    }

    public List<ParameterInfo> getParameters() {
        return parameters;
    }

    public boolean hasBodyParameter() {
        return !bodyParameters.isEmpty();
    }

    public boolean hasCustomAnnotation() {
        return hasCustomAnnotation;
    }

    public boolean hasHandlerAnnotation() {
        return hasHandlerAnnotation;
    }

    public boolean isReturnTypeVoid() {
        return method.getReturnType().getName().equals("void");
    }

    public boolean isStaticMethod() {
        return Modifier.isStatic(method.getModifiers());
    }

    protected Object invoke(Method mth, Object pojo, Object[] arguments, Exchange exchange) throws InvocationTargetException {
        try {
            return mth.invoke(pojo, arguments);
        } catch (IllegalAccessException e) {
            throw new RuntimeExchangeException("IllegalAccessException occurred invoking method: " + mth + " using arguments: " + Arrays.asList(arguments), exchange, e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeExchangeException("IllegalArgumentException occurred invoking method: " + mth + " using arguments: " + Arrays.asList(arguments), exchange, e);
        }
    }

    protected Expression createParametersExpression() {
        final int size = parameters.size();
        LOG.trace("Creating parameters expression for {} parameters", size);

        final Expression[] expressions = new Expression[size];
        for (int i = 0; i < size; i++) {
            Expression parameterExpression = parameters.get(i).getExpression();
            expressions[i] = parameterExpression;
            LOG.trace("Parameter #{} has expression: {}", i, parameterExpression);
        }
        return new Expression() {
            @SuppressWarnings("unchecked")
            public <T> T evaluate(Exchange exchange, Class<T> type) {
                Object[] answer = new Object[size];
                Object body = exchange.getIn().getBody();
                boolean multiParameterArray = false;
                if (exchange.getIn().getHeader(Exchange.BEAN_MULTI_PARAMETER_ARRAY) != null) {
                    multiParameterArray = exchange.getIn().getHeader(Exchange.BEAN_MULTI_PARAMETER_ARRAY, Boolean.class);
                }

                // if there was an explicit method name to invoke, then we should support using
                // any provided parameter values in the method name
                String methodName = exchange.getIn().getHeader(Exchange.BEAN_METHOD_NAME, "", String.class);
                // the parameter values is between the parenthesis
                String methodParameters = ObjectHelper.between(methodName, "(", ")");
                // use an iterator to walk the parameter values
                Iterator<?> it = null;
                if (methodParameters != null) {
                    // split the parameters safely separated by comma, but beware that we can have
                    // quoted parameters which contains comma as well, so do a safe quote split
                    String[] parameters = StringQuoteHelper.splitSafeQuote(methodParameters, ',', false);
                    it = ObjectHelper.createIterator(parameters, ",", true);
                }

                // remove headers as they should not be propagated
                // we need to do this before the expressions gets evaluated as it may contain
                // a @Bean expression which would by mistake read these headers. So the headers
                // must be removed at this point of time
                exchange.getIn().removeHeader(Exchange.BEAN_MULTI_PARAMETER_ARRAY);
                exchange.getIn().removeHeader(Exchange.BEAN_METHOD_NAME);

                for (int i = 0; i < size; i++) {
                    // grab the parameter value for the given index
                    Object parameterValue = it != null && it.hasNext() ? it.next() : null;
                    // and the expected parameter type
                    Class<?> parameterType = parameters.get(i).getType();
                    // the value for the parameter to use
                    Object value = null;

                    if (multiParameterArray) {
                        // get the value from the array
                        value = ((Object[])body)[i];
                    } else {
                        // prefer to use parameter value if given, as they override any bean parameter binding
                        // we should skip * as its a type placeholder to indicate any type
                        if (parameterValue != null && !parameterValue.equals("*")) {
                            // evaluate the parameter value binding
                            value = evaluateParameterValue(exchange, i, parameterValue, parameterType);
                        }

                        // use bean parameter binding, if still no value
                        Expression expression = expressions[i];
                        if (value == null && expression != null) {
                            value = evaluateParameterBinding(exchange, expression, i, parameterType);
                        }
                    }

                    // remember the value to use
                    if (value != Void.TYPE) {
                        answer[i] = value;
                    }
                }

                return (T) answer;
            }

            /**
             * Evaluate using parameter values where the values can be provided in the method name syntax.
             * <p/>
             * This methods returns accordingly:
             * <ul>
             *     <li><tt>null</tt> - if not a parameter value</li>
             *     <li><tt>Void.TYPE</tt> - if an explicit null, forcing Camel to pass in <tt>null</tt> for that given parameter</li>
             *     <li>a non <tt>null</tt> value - if the parameter was a parameter value, and to be used</li>
             * </ul>
             *
             * @since 2.9
             */
            private Object evaluateParameterValue(Exchange exchange, int index, Object parameterValue, Class<?> parameterType) {
                Object answer = null;

                // convert the parameter value to a String
                String exp = exchange.getContext().getTypeConverter().convertTo(String.class, exchange, parameterValue);
                if (exp != null) {
                    // check if its a valid parameter value
                    boolean valid = BeanHelper.isValidParameterValue(exp);

                    if (!valid) {
                        // it may be a parameter type instead, and if so, then we should return null,
                        // as this method is only for evaluating parameter values
                        Boolean isClass = BeanHelper.isAssignableToExpectedType(exchange.getContext().getClassResolver(), exp, parameterType);
                        // the method will return a non null value if exp is a class
                        if (isClass != null) {
                            return null;
                        }
                    }

                    // use simple language to evaluate the expression, as it may use the simple language to refer to message body, headers etc.
                    Expression expression = null;
                    try {
                        expression = exchange.getContext().resolveLanguage("simple").createExpression(exp);
                        parameterValue = expression.evaluate(exchange, Object.class);
                    } catch (Exception e) {
                        throw new ExpressionEvaluationException(expression, "Cannot create/evaluate simple expression: " + exp
                                + " to be bound to parameter at index: " + index + " on method: " + getMethod(), exchange, e);
                    }

                    if (parameterValue != null) {

                        // special for explicit null parameter values (as end users can explicit indicate they want null as parameter)
                        // see method javadoc for details
                        if ("null".equals(parameterValue)) {
                            return Void.TYPE;
                        }

                        // the parameter value was not already valid, but since the simple language have evaluated the expression
                        // which may change the parameterValue, so we have to check it again to see if its now valid
                        exp = exchange.getContext().getTypeConverter().convertTo(String.class, parameterValue);
                        // String values from the simple language is always valid
                        if (!valid) {
                            // re validate if the parameter was not valid the first time (String values should be accepted)
                            valid = parameterValue instanceof String || BeanHelper.isValidParameterValue(exp);
                        }

                        if (valid) {
                            // we need to unquote String parameters, as the enclosing quotes is there to denote a parameter value
                            if (parameterValue instanceof String) {
                                parameterValue = StringHelper.removeLeadingAndEndingQuotes((String) parameterValue);
                            }
                            try {
                                // its a valid parameter value, so convert it to the expected type of the parameter
                                answer = exchange.getContext().getTypeConverter().mandatoryConvertTo(parameterType, parameterValue);
                                if (LOG.isTraceEnabled()) {
                                    LOG.trace("Parameter #{} evaluated as: {} type: ", new Object[]{index, answer, ObjectHelper.type(answer)});
                                }
                            } catch (NoTypeConversionAvailableException e) {
                                throw ObjectHelper.wrapVramelExecutionException(exchange, e);
                            }
                        }
                    }
                }

                return answer;
            }

            /**
             * Evaluate using classic parameter binding using the pre compute expression
             */
            private Object evaluateParameterBinding(Exchange exchange, Expression expression, int index, Class<?> parameterType) {
                Object answer = null;

                // use object first to avoid type conversion so we know if there is a value or not
                Object result = expression.evaluate(exchange, Object.class);
                if (result != null) {
                    // we got a value now try to convert it to the expected type
                    try {
                        answer = exchange.getContext().getTypeConverter().mandatoryConvertTo(parameterType, result);
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Parameter #{} evaluated as: {} type: ", new Object[]{index, answer, ObjectHelper.type(answer)});
                        }
                    } catch (NoTypeConversionAvailableException e) {
                        throw ObjectHelper.wrapVramelExecutionException(exchange, e);
                    }
                } else {
                    LOG.trace("Parameter #{} evaluated as null", index);
                }

                return answer;
            }

            @Override
            public String toString() {
                return "ParametersExpression: " + Arrays.asList(expressions);
            }

        };
    }


    /**
     * Adds the current class and all of its base classes (apart from {@link Object} to the given list
     */
    protected void addTypeAndSuperTypes(Class<?> type, List<Class<?>> result) {
        for (Class<?> t = type; t != null && t != Object.class; t = t.getSuperclass()) {
            result.add(t);
        }
    }

    protected boolean hasExceptionParameter() {
        for (ParameterInfo parameter : parameters) {
            if (Exception.class.isAssignableFrom(parameter.getType())) {
                return true;
            }
        }
        return false;
    }

}
