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
package com.nxttxn.vramel.language.property;


import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.IsSingleton;
import com.nxttxn.vramel.Predicate;
import com.nxttxn.vramel.builder.ExpressionBuilder;
import com.nxttxn.vramel.spi.Language;
import com.nxttxn.vramel.util.ExpressionToPredicateAdapter;

/**
 * A language for property expressions.
 */
public class PropertyLanguage implements Language, IsSingleton {

    public static Expression property(String propertyName) {
        return ExpressionBuilder.propertyExpression(propertyName);
    }

    public Predicate createPredicate(String expression) {
        return ExpressionToPredicateAdapter.toPredicate(createExpression(expression));
    }

    public Expression createExpression(String expression) {
        return PropertyLanguage.property(expression);
    }

    public boolean isSingleton() {
        return true;
    }
}
