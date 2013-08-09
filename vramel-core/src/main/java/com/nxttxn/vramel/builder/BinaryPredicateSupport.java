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



import com.nxttxn.vramel.BinaryPredicate;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Expression;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * A useful base class for {@link com.nxttxn.vramel.Predicate} implementations
 *
 * @version
 */
public abstract class BinaryPredicateSupport implements BinaryPredicate {

    private final Expression left;
    private final Expression right;

    protected BinaryPredicateSupport(Expression left, Expression right) {
        checkNotNull(left, "left");
        checkNotNull(right, "right");

        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return left + " " + getOperationText() + " " + right;
    }

    public boolean matches(Exchange exchange) {
        return matchesReturningFailureMessage(exchange) == null;
    }

    public String matchesReturningFailureMessage(Exchange exchange) {
        // we must not store any state, so we can be thread safe
        // and thus we offer this method which returns a failure message if
        // we did not match
        String answer = null;

        // must be thread safe and store result in local objects
        Object leftValue = left.evaluate(exchange, Object.class);
        Object rightValue = right.evaluate(exchange, Object.class);
        if (!matches(exchange, leftValue, rightValue)) {
            answer = leftValue + " " + getOperator() + " " + rightValue;
        }

        return answer;
    }

    protected abstract boolean matches(Exchange exchange, Object leftValue, Object rightValue);

    protected abstract String getOperationText();

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    public String getOperator() {
        return getOperationText();
    }

}
