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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;


import com.nxttxn.vramel.*;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.processor.aggregate.UseOriginalAggregationStrategy;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.util.ExchangeHelper;
import com.nxttxn.vramel.util.IOHelper;
import com.nxttxn.vramel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.util.ObjectHelper.notNull;

/**
 * Implements a dynamic <a
 * href="http://camel.apache.org/splitter.html">Splitter</a> pattern
 * where an expression is evaluated to iterate through each of the parts of a
 * message and then each part is then send to some endpoint.
 *
 * @version
 */
public class Splitter extends MulticastProcessor implements AsyncProcessor {
    private static final transient Logger LOG = LoggerFactory.getLogger(Splitter.class);

    private final Expression expression;

    public Splitter(Expression expression, Processor destination, AggregationStrategy aggregationStrategy) {
        this(expression, destination, aggregationStrategy, false, false, false, 0, null, false);
    }

    public Splitter(Expression expression, Processor destination, AggregationStrategy aggregationStrategy,
                    boolean parallelProcessing,
                    boolean streaming, boolean stopOnException, long timeout, Processor onPrepare, boolean useSubUnitOfWork) {
        super(Collections.singleton(destination), aggregationStrategy, parallelProcessing, streaming, stopOnException, timeout, onPrepare, useSubUnitOfWork);
        this.expression = expression;
        notNull(expression, "expression");
        notNull(destination, "destination");
    }

    @Override
    public String toString() {
        return "Splitter[on: " + expression + " to: " + getProcessors().iterator().next() + " aggregate: " + getAggregationStrategy() + "]";
    }


    @Override
    public boolean process(Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final AggregationStrategy strategy = getAggregationStrategy();

        // if no custom aggregation strategy is being used then fallback to keep the original
        // and propagate exceptions which is done by a per exchange specific aggregation strategy
        // to ensure it supports async routing
        if (strategy == null) {
            UseOriginalAggregationStrategy original = new UseOriginalAggregationStrategy(exchange, true);
            setAggregationStrategyOnExchange(exchange, original);
        }

        return super.process(exchange, optionalAsyncResultHandler);
    }

    @Override
    protected Iterable<ProcessorExchangePair> createProcessorExchangePairs(Exchange exchange) throws Exception {
        Object value = expression.evaluate(exchange, Object.class);
        if (exchange.getException() != null) {
            // force any exceptions occurred during evaluation to be thrown
            throw exchange.getException();
        }

        Iterable<ProcessorExchangePair> answer;
        if (isStreaming()) {
            throw new UnsupportedOperationException("Streaming not enabled yet");
//            answer = createProcessorExchangePairsIterable(exchange, value);
        } else {
            answer = createProcessorExchangePairsList(exchange, value);
        }
        if (exchange.getException() != null) {
            // force any exceptions occurred during creation of exchange paris to be thrown
            // before returning the answer;
            throw exchange.getException();
        }

        return answer;
    }

    private Iterable<ProcessorExchangePair> createProcessorExchangePairsIterable(final Exchange exchange, final Object value) {
        final Iterator<?> iterator = ObjectHelper.createIterator(value);
        return new Iterable<ProcessorExchangePair>() {
            // create a copy which we use as master to copy during splitting
            // this avoids any side effect reflected upon the incoming exchange
            private final Exchange copy = copyExchangeNoAttachments(exchange, true);
            private final FlowContext flowContext = exchange.getUnitOfWork() != null ? exchange.getUnitOfWork().getFlowContext() : null;

            public Iterator<ProcessorExchangePair> iterator() {
                return new Iterator<ProcessorExchangePair>() {
                    private int index;
                    private boolean closed;

                    public boolean hasNext() {
                        if (closed) {
                            return false;
                        }

                        boolean answer = iterator.hasNext();
                        if (!answer) {
                            // we are now closed
                            closed = true;
                            // nothing more so we need to close the expression value in case it needs to be
                            if (value instanceof Closeable) {
                                IOHelper.close((Closeable) value, value.getClass().getName(), LOG);
                            } else if (value instanceof Scanner) {
                                // special for Scanner as it does not implement Closeable
                                Scanner scanner = (Scanner) value;
                                scanner.close();

                                IOException ioException = scanner.ioException();
                                if (ioException != null) {
                                    throw new RuntimeVramelException("Scanner aborted because of an IOException!", ioException);
                                }
                            }
                        }
                        return answer;
                    }

                    public ProcessorExchangePair next() {
                        Object part = iterator.next();
                        // create a correlated copy as the new exchange to be routed in the splitter from the copy
                        // and do not share the unit of work
                        Exchange newExchange = ExchangeHelper.createCorrelatedCopy(copy, false);
                        // if we share unit of work, we need to prepare the child exchange
                        if (isShareUnitOfWork()) {
                            prepareSharedUnitOfWork(newExchange, copy);
                        }
                        if (part instanceof Message) {
                            newExchange.setIn((Message) part);
                        } else {
                            Message in = newExchange.getIn();
                            in.setBody(part);
                        }
                        return createProcessorExchangePair(index++, getProcessors().iterator().next(), newExchange, flowContext);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("Remove is not supported by this iterator");
                    }
                };
            }

        };
    }

    private Iterable<ProcessorExchangePair> createProcessorExchangePairsList(Exchange exchange, Object value) {
        List<ProcessorExchangePair> result = new ArrayList<ProcessorExchangePair>();

        // reuse iterable and add it to the result list
        Iterable<ProcessorExchangePair> pairs = createProcessorExchangePairsIterable(exchange, value);
        for (ProcessorExchangePair pair : pairs) {
            result.add(pair);
        }

        return result;
    }

    @Override
    protected void updateNewExchange(Exchange exchange, int index, Iterable<ProcessorExchangePair> allPairs,
                                     Iterator<ProcessorExchangePair> it) {
        // do not share unit of work
        exchange.setUnitOfWork(null);

        exchange.setProperty(Exchange.SPLIT_INDEX, index);
        if (allPairs instanceof Collection) {
            // non streaming mode, so we know the total size already
            exchange.setProperty(Exchange.SPLIT_SIZE, ((Collection<?>) allPairs).size());
        }
        if (it.hasNext()) {
            exchange.setProperty(Exchange.SPLIT_COMPLETE, Boolean.FALSE);
        } else {
            exchange.setProperty(Exchange.SPLIT_COMPLETE, Boolean.TRUE);
            // streaming mode, so set total size when we are complete based on the index
            exchange.setProperty(Exchange.SPLIT_SIZE, index + 1);
        }
    }

    @Override
    protected Integer getExchangeIndex(Exchange exchange) {
        return exchange.getProperty(Exchange.SPLIT_INDEX, Integer.class);
    }

    public Expression getExpression() {
        return expression;
    }

    private static Exchange copyExchangeNoAttachments(Exchange exchange, boolean preserveExchangeId) {
        Exchange answer = ExchangeHelper.createCopy(exchange, preserveExchangeId);
        // we do not want attachments for the splitted sub-messages
//        answer.getIn().setAttachments(null);
        return answer;
    }


}
