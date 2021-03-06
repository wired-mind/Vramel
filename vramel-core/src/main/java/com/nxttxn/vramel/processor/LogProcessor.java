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


import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.AsyncProcessorHelper;
import com.nxttxn.vramel.util.VramelLogger;
import org.vertx.java.core.AsyncResultHandler;

/**
 * A processor which evaluates an {@link Expression} and logs it.
 *
 * @version
 */
public class LogProcessor extends ServiceSupport implements AsyncProcessor {

    private final Expression expression;
    private final VramelLogger logger;

    public LogProcessor(Expression expression, VramelLogger logger) {
        this.expression = expression;
        this.logger = logger;
    }

    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }

    @Override
    public boolean process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) {
        try {
            if (logger.shouldLog()) {
                String msg = expression.evaluate(exchange, String.class);
                logger.doLog(msg);
            }
        } catch (Exception e) {
            exchange.setException(e);
        } finally {
            // optionalAsyncResultHandler must be invoked
            optionalAsyncResultHandler.done(exchange);
        }
        return true;
    }

    @Override
    public String toString() {
        return "Log(" + logger.getLog().getName() + ")[" + expression + "]";
    }


    @Override
    protected void doStart(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
        // noop
    }

    @Override
    protected void doStop() throws Exception {
        // noop
    }
}
