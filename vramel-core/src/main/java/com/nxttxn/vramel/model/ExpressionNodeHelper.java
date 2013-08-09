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
package com.nxttxn.vramel.model;

import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.Predicate;
import com.nxttxn.vramel.builder.ValueBuilder;
import com.nxttxn.vramel.model.language.ExpressionDefinition;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/25/13
 * Time: 11:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExpressionNodeHelper {

    /**
     * Determines which {@link ExpressionDefinition} describes the given expression best possible.
     * <p/>
     * This implementation will use types such as {@link SimpleExpression}, {@link XPathExpression} etc.
     * if the given expression is detect as such a type.
     *
     * @param expression the expression
     * @return a definition which describes the expression
     */
    public static ExpressionDefinition toExpressionDefinition(Expression expression) {
//        if (expression instanceof SimpleBuilder) {
//            SimpleBuilder builder = (SimpleBuilder) expression;
//            // we keep the original expression by using the constructor that accepts an expression
//            SimpleExpression answer = new SimpleExpression(builder);
//            answer.setExpression(builder.getText());
//            answer.setResultType(builder.getResultType());
//            return answer;
//        } else if (expression instanceof XPathBuilder) {
//            XPathBuilder builder = (XPathBuilder) expression;
//            // we keep the original expression by using the constructor that accepts an expression
//            XPathExpression answer = new XPathExpression(builder);
//            answer.setExpression(builder.getText());
//            answer.setResultType(builder.getResultType());
//            return answer;
//        } else
        if (expression instanceof ValueBuilder) {
            ValueBuilder builder = (ValueBuilder) expression;
            expression = builder.getExpression();
        }

        if (expression instanceof ExpressionDefinition) {
            return (ExpressionDefinition) expression;
        }
        return new ExpressionDefinition(expression);
    }

    /**
     * Determines which {@link ExpressionDefinition} describes the given predicate best possible.
     * <p/>
     * This implementation will use types such as {@link SimpleExpression}, {@link XPathExpression} etc.
     * if the given predicate is detect as such a type.
     *
     * @param predicate the predicate
     * @return a definition which describes the predicate
     */
    public static ExpressionDefinition toExpressionDefinition(Predicate predicate) {
//        if (predicate instanceof SimpleBuilder) {
//            SimpleBuilder builder = (SimpleBuilder) predicate;
//            // we keep the original expression by using the constructor that accepts an expression
//            SimpleExpression answer = new SimpleExpression(builder);
//            answer.setExpression(builder.getText());
//            return answer;
//        } else if (predicate instanceof XPathBuilder) {
//            XPathBuilder builder = (XPathBuilder) predicate;
//            // we keep the original expression by using the constructor that accepts an expression
//            XPathExpression answer = new XPathExpression(builder);
//            answer.setExpression(builder.getText());
//            return answer;
//        }

        if (predicate instanceof ExpressionDefinition) {
            return (ExpressionDefinition) predicate;
        }
        return new ExpressionDefinition(predicate);
    }
}
