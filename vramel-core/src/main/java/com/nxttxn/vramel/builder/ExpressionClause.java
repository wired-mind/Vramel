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
import com.nxttxn.vramel.model.ExpressionNode;
import com.nxttxn.vramel.model.language.ExpressionDefinition;

import java.util.Map;



/**
 * Represents an expression clause within the DSL which when the expression is
 * complete the clause continues to another part of the DSL
 *
 * @version
 */
public class ExpressionClause<T> extends ExpressionDefinition {
    private ExpressionClauseSupport<T> delegate;

    public ExpressionClause(T result) {
        this.delegate = new ExpressionClauseSupport<T>(result);
    }

    public static <T extends ExpressionNode> ExpressionClause<T> createAndSetExpression(T result) {
        ExpressionClause<T> clause = new ExpressionClause<T>(result);
        result.setExpression(clause);
        return clause;
    }

    // Helper expressions
    // -------------------------------------------------------------------------

    /**
     * Specify an {@link Expression} instance
     */
    public T expression(Expression expression) {
        return delegate.expression(expression);
    }

    /**
     * Specify the constant expression value
     */
    public T constant(Object value) {
        return delegate.constant(value);
    }

    /**
     * An expression of the exchange
     */
    public T exchange() {
        return delegate.exchange();
    }

    /**
     * An expression of an inbound message
     */
    public T inMessage() {
        return delegate.inMessage();
    }

    /**
     * An expression of an inbound message
     */
    public T outMessage() {
        return delegate.outMessage();
    }

    /**
     * An expression of an inbound message body
     */
    public T body() {
        return delegate.body();
    }

    /**
     * An expression of an inbound message body converted to the expected type
     */
    public T body(Class<?> expectedType) {
        return delegate.body(expectedType);
    }

    /**
     * An expression of an outbound message body
     */
    public T outBody() {
        return delegate.outBody();
    }

    /**
     * An expression of an outbound message body converted to the expected type
     */
    public T outBody(Class<?> expectedType) {
        return delegate.outBody(expectedType);
    }

    /**
     * An expression of an inbound message header of the given name
     */
    public T header(String name) {
        return delegate.header(name);
    }

    /**
     * An expression of the inbound headers
     */
    public T headers() {
        return delegate.headers();
    }

    /**
     * An expression of an outbound message header of the given name
     */
    public T outHeader(String name) {
        return delegate.outHeader(name);
    }


    /**
     * An expression of an exchange property of the given name
     */
    public T property(String name) {
        return delegate.property(name);
    }

    /**
     * An expression of the exchange properties
     */
    public T properties() {
        return delegate.properties();
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
        return delegate.simple(text);
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
        return delegate.simple(text, resultType);
    }


    // Properties
    // -------------------------------------------------------------------------

    @Override
    public Expression getExpressionValue() {
        return delegate.getExpressionValue();
    }

    @Override
    protected void setExpressionValue(Expression expressionValue) {
        delegate.setExpressionValue(expressionValue);
    }

    @Override
    public ExpressionDefinition getExpressionType() {
        return delegate.getExpressionType();
    }

    @Override
    protected void setExpressionType(ExpressionDefinition expressionType) {
        delegate.setExpressionType(expressionType);
    }
}
