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
package com.nxttxn.vramel.processor;


import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultMessage;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.ObjectHelper;

/**
 * A processor which sets the body on the OUT message with an {@link Expression}
 */
public class TransformProcessor  implements Processor {
    private final Expression expression;

    public TransformProcessor(Expression expression) {
        ObjectHelper.notNull(expression, "expression", this);
        this.expression = expression;
    }



    @Override
    public String toString() {
        return "Transform(" + expression + ")";
    }


    @Override
    public void process(Exchange exchange) throws Exception {
        Object newBody = expression.evaluate(exchange, Object.class);

        Message old = exchange.getIn();

        // create a new message container so we do not drag specialized message objects along
        Message msg = new DefaultMessage();
        msg.copyFrom(old);
        msg.setBody(newBody);
        exchange.setOut(msg);
    }
}
