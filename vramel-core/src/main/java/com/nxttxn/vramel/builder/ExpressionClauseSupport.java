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
import com.nxttxn.vramel.model.language.*;

import java.util.Map;



/**
 * A support class for building expression clauses.
 *
 * @version
 */
public class ExpressionClauseSupport<T> {

    private T result;
    private Expression expressionValue;
    private ExpressionDefinition expressionType;

    public ExpressionClauseSupport(T result) {
        this.result = result;
    }

    // Helper expressions
    // -------------------------------------------------------------------------

    /**
     * Specify an {@link com.nxttxn.vramel.Expression} instance
     */
    public T expression(Expression expression) {
        setExpressionValue(expression);
        return result;
    }

    public T expression(ExpressionDefinition expression) {
        setExpressionType(expression);
        return result;
    }

    /**
     * Specify the constant expression value
     */
    public T constant(Object value) {
        if (value instanceof String) {
            return expression(new ConstantExpression((String) value));
        } else {
            return expression(ExpressionBuilder.constantExpression(value));
        }
    }

    /**
     * An expression of the exchange
     */
    public T exchange() {
        return expression(ExpressionBuilder.exchangeExpression());
    }

    /**
     * An expression of an inbound message
     */
    public T inMessage() {
        return expression(ExpressionBuilder.inMessageExpression());
    }

    /**
     * An expression of an inbound message
     */
    public T outMessage() {
        return expression(ExpressionBuilder.outMessageExpression());
    }

    /**
     * An expression of an inbound message body
     */
    public T body() {
        return expression(ExpressionBuilder.bodyExpression());
    }

    /**
     * An expression of an inbound message body converted to the expected type
     */
    public T body(Class<?> expectedType) {
        return expression(ExpressionBuilder.bodyExpression(expectedType));
    }

    /**
     * An expression of an outbound message body
     */
    public T outBody() {
        return expression(ExpressionBuilder.outBodyExpression());
    }

    /**
     * An expression of an outbound message body converted to the expected type
     */
    public T outBody(Class<?> expectedType) {
        return expression(ExpressionBuilder.outBodyExpression(expectedType));
    }

    /**
     * An expression of an inbound message header of the given name
     */
    public T header(String name) {
        return expression(new HeaderExpression(name));
    }

    /**
     * An expression of the inbound headers
     */
    public T headers() {
        return expression(ExpressionBuilder.headersExpression());
    }

    /**
     * An expression of an outbound message header of the given name
     */
    public T outHeader(String name) {
        return expression(ExpressionBuilder.outHeaderExpression(name));
    }




    /**
     * An expression of an exchange property of the given name
     */
    public T property(String name) {
        return expression(new PropertyExpression(name));
    }

    /**
     * An expression of the exchange properties
     */
    public T properties() {
        return expression(ExpressionBuilder.propertiesExpression());
    }

    // Languages
    // -------------------------------------------------------------------------


    /**
     * Evaluates a <a href="http://camel.apache.org/simple.html">Simple
     * expression</a>
     *
     * @param text the expression to be evaluated
     * @return the builder to continue processing the DSL
     */
    public T simple(String text) {
        return expression(new SimpleExpression(text));
    }

    /**
     * Evaluates a <a href="http://camel.apache.org/simple.html">Simple
     * expression</a>
     *
     * @param text the expression to be evaluated
     * @param resultType the result type
     * @return the builder to continue processing the DSL
     */
    public T simple(String text, Class<?> resultType) {
        SimpleExpression expression = new SimpleExpression(text);
        expression.setResultType(resultType);
        setExpressionType(expression);
        return result;
    }


    // Properties
    // -------------------------------------------------------------------------

    public Expression getExpressionValue() {
        return expressionValue;
    }

    public void setExpressionValue(Expression expressionValue) {
        this.expressionValue = expressionValue;
    }

    public ExpressionDefinition getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(ExpressionDefinition expressionType) {
        this.expressionType = expressionType;
    }



}
