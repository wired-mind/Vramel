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
package com.nxttxn.vramel.converter;


import com.nxttxn.vramel.*;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;

/**
 * Some useful converters for Camel APIs such as to convert a {@link Predicate} or {@link Expression}
 * to a {@link com.nxttxn.vramel.AsyncProcessor}
 *
 * @version
 */
@Converter
public final class CamelConverter {

    /**
     * Utility classes should not have a public constructor.
     */
    private CamelConverter() {
    }

    @Converter
    public static Processor toProcessor(final Predicate predicate) {
        return new Processor() {
            public void process(Exchange exchange) throws Exception {
                boolean answer = predicate.matches(exchange);
                Message out = exchange.getOut();
                out.copyFrom(exchange.getIn());
                out.setBody(answer);
            }
        };

    }

    @Converter
    public static Processor toProcessor(final Expression expresion) {
        return new Processor() {
            public void process(Exchange exchange) throws Exception {
                Object answer = expresion.evaluate(exchange, Object.class);
                Message out = exchange.getOut();
                out.copyFrom(exchange.getIn());
                out.setBody(answer);
            }
        };
    }

}
