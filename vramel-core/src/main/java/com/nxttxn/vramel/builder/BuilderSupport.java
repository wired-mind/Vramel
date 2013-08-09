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
package com.nxttxn.vramel.builder;

import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.model.language.HeaderExpression;
import com.nxttxn.vramel.model.language.MethodCallExpression;
import com.nxttxn.vramel.model.language.PropertyExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



/**
 * Base class for implementation inheritance for different clauses in the <a
 * href="http://camel.apache.org/dsl.html">Java DSL</a>
 *
 * @version
 */
public abstract class BuilderSupport {
    private ErrorHandlerBuilder errorHandlerBuilder;

    // Builder methods
    // -------------------------------------------------------------------------

    /**
     * Returns a value builder for the given header
     */
    public ValueBuilder header(String name) {
        return Builder.header(name);
    }

    /**
     * Returns a value builder for the given property
     */
    public ValueBuilder property(String name) {
        PropertyExpression expression = new PropertyExpression(name);
        return new ValueBuilder(expression);
    }

    /**
     * Returns a predicate and value builder for the inbound body on an exchange
     */
    public ValueBuilder body() {
        return Builder.body();
    }

    /**
     * Returns a predicate and value builder for the inbound message body as a
     * specific type
     */
    public <T> ValueBuilder body(Class<T> type) {
        return Builder.bodyAs(type);
    }

    /**
     * Returns a predicate and value builder for the outbound body on an
     * exchange
     */
    public ValueBuilder outBody() {
        return Builder.outBody();
    }

    /**
     * Returns a predicate and value builder for the outbound message body as a
     * specific type
     */
    public <T> ValueBuilder outBody(Class<T> type) {
        return Builder.outBodyAs(type);
    }


    /**
     * Returns a value builder for the given system property
     */
    public ValueBuilder systemProperty(String name) {
        return Builder.systemProperty(name);
    }

    /**
     * Returns a value builder for the given system property
     */
    public ValueBuilder systemProperty(String name, String defaultValue) {
        return Builder.systemProperty(name, defaultValue);
    }

    /**
     * Returns a constant expression value builder
     */
    public ValueBuilder constant(Object value) {
        return Builder.constant(value);
    }


    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a>
     * value builder
     * <p/>
     * This method accepts dual parameters. Either an bean instance or a reference to a bean (String).
     *
     * @param beanOrBeanRef  either an instanceof a bean or a reference to bean to lookup in the Registry
     * @return the builder
     * @deprecated use {@link #method(Object)} instead
     */
    @Deprecated
    public ValueBuilder bean(Object beanOrBeanRef) {
        return bean(beanOrBeanRef, null);
    }

    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a>
     * value builder
     * <p/>
     * This method accepts dual parameters. Either an bean instance or a reference to a bean (String).
     *
     * @param beanOrBeanRef  either an instanceof a bean or a reference to bean to lookup in the Registry
     * @param method   name of method to invoke
     * @return the builder
     * @deprecated use {@link #method(Object, String)} instead
     */
    @Deprecated
    public ValueBuilder bean(Object beanOrBeanRef, String method) {
        MethodCallExpression expression;
        if (beanOrBeanRef instanceof String) {
            expression = new MethodCallExpression((String) beanOrBeanRef, method);
        } else {
            expression = new MethodCallExpression(beanOrBeanRef, method);
        }
        return new ValueBuilder(expression);
    }

    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a>
     * value builder
     *
     * @param beanType the Class of the bean which we want to invoke
     * @return the builder
     * @deprecated use {@link #method(Class)} instead
     */
    @Deprecated
    public ValueBuilder bean(Class<?> beanType) {
        MethodCallExpression expression = new MethodCallExpression(beanType);
        return new ValueBuilder(expression);
    }

    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a>
     * value builder
     *
     * @param beanType the Class of the bean which we want to invoke
     * @param method   name of method to invoke
     * @return the builder
     * @deprecated use {@link #method(Class, String)} instead
     */
    @Deprecated
    public ValueBuilder bean(Class<?> beanType, String method) {
        MethodCallExpression expression = new MethodCallExpression(beanType, method);
        return new ValueBuilder(expression);
    }

    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a>
     * value builder
     * <p/>
     * This method accepts dual parameters. Either an bean instance or a reference to a bean (String).
     *
     * @param beanOrBeanRef  either an instanceof a bean or a reference to bean to lookup in the Registry
     * @return the builder
     */
    public ValueBuilder method(Object beanOrBeanRef) {
        return method(beanOrBeanRef, null);
    }

    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a>
     * value builder
     * <p/>
     * This method accepts dual parameters. Either an bean instance or a reference to a bean (String).
     *
     * @param beanOrBeanRef  either an instanceof a bean or a reference to bean to lookup in the Registry
     * @param method   name of method to invoke
     * @return the builder
     */
    public ValueBuilder method(Object beanOrBeanRef, String method) {
        MethodCallExpression expression;
        if (beanOrBeanRef instanceof String) {
            expression = new MethodCallExpression((String) beanOrBeanRef, method);
        } else {
            expression = new MethodCallExpression(beanOrBeanRef, method);
        }
        return new ValueBuilder(expression);
    }

    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a>
     * value builder
     *
     * @param beanType the Class of the bean which we want to invoke
     * @return the builder
     */
    public ValueBuilder method(Class<?> beanType) {
        MethodCallExpression expression = new MethodCallExpression(beanType);
        return new ValueBuilder(expression);
    }

    /**
     * Returns a simple expression value builder
     */
    public SimpleBuilder simple(String value) {
        return SimpleBuilder.simple(value);
    }

    /**
     * Returns a simple expression value builder
     */
    public SimpleBuilder simple(String value, Class<?> resultType) {
        return SimpleBuilder.simple(value, resultType);
    }
    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a>
     * value builder
     *
     * @param beanType the Class of the bean which we want to invoke
     * @param method   name of method to invoke
     * @return the builder
     */
    public ValueBuilder method(Class<?> beanType, String method) {
        MethodCallExpression expression = new MethodCallExpression(beanType, method);
        return new ValueBuilder(expression);
    }

    /**
     * Returns an expression value builder that replaces all occurrences of the
     * regular expression with the given replacement
     */
    public ValueBuilder regexReplaceAll(Expression content, String regex, String replacement) {
        return Builder.regexReplaceAll(content, regex, replacement);
    }

    /**
     * Returns an expression value builder that replaces all occurrences of the
     * regular expression with the given replacement
     */
    public ValueBuilder regexReplaceAll(Expression content, String regex, Expression replacement) {
        return Builder.regexReplaceAll(content, regex, replacement);
    }

    /**
     * Returns a exception expression value builder
     */
    public ValueBuilder exceptionMessage() {
        return Builder.exceptionMessage();
    }


    public ErrorHandlerBuilder getErrorHandlerBuilder() {
        if (errorHandlerBuilder == null) {
            errorHandlerBuilder = createErrorHandlerBuilder();
        }
        return errorHandlerBuilder;
    }

    protected ErrorHandlerBuilder createErrorHandlerBuilder() {
        return new DefaultErrorHandlerBuilder();
    }

    /**
     * Sets the error handler to use with processors created by this builder
     */
    public void setErrorHandlerBuilder(ErrorHandlerBuilder errorHandlerBuilder) {
        this.errorHandlerBuilder = errorHandlerBuilder;
    }
}
