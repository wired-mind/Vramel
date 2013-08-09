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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


import com.google.common.collect.Lists;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Navigate;
import com.nxttxn.vramel.Predicate;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.async.DefaultExchangeHandler;
import com.nxttxn.vramel.processor.async.FixedSizeDoneStrategy;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.support.PipelineSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a Choice structure where one or more predicates are used which if
 * they are true their processors are used, with a default otherwise clause used
 * if none match.
 */
public class ChoiceProcessor extends PipelineSupport implements Processor, Navigate<Processor> {
    private static final transient Logger LOG = LoggerFactory.getLogger(ChoiceProcessor.class);
    private final List<FilterProcessor> filters;
    private final Processor otherwise;

    public ChoiceProcessor(List<FilterProcessor> filters, Processor otherwise) {
        this.filters = filters;
        this.otherwise = otherwise;
    }


    public void process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {

        List<FilterProcessor> matchedFilters = computeMatchedFilters(exchange);
        final boolean noMatches = matchedFilters.isEmpty();
        if (noMatches) {
            if (otherwise != null) {
                otherwise.process(exchange, new DefaultExchangeHandler(exchange, optionalAsyncResultHandler));
            } else {
                optionalAsyncResultHandler.done(exchange);
            }

            return;
        }

        final Iterable<ProcessorExchangePair<FilterProcessor>> processorExchangePairs = createProcessorExchangePairs(exchange, matchedFilters);

        final DefaultExchangeHandler defaultExchangeHandler = new DefaultExchangeHandler(exchange, optionalAsyncResultHandler, new FixedSizeDoneStrategy<Exchange>(matchedFilters.size(), new AtomicInteger(0)));

        for (ProcessorExchangePair<FilterProcessor> pair : processorExchangePairs) {
            final Exchange copy = pair.getExchange();
            final FilterProcessor matchedFilter = pair.getProcessor();
            matchedFilter.processNext(exchange, defaultExchangeHandler);
        }


    }

    private List<FilterProcessor> computeMatchedFilters(Exchange exchange) throws Exception {
        List<FilterProcessor> matchedFilters = Lists.newArrayList();
        for (int i = 0; i < filters.size(); i++) {
            FilterProcessor filter = filters.get(i);
            Predicate predicate = filter.getPredicate();

            boolean matches = false;

            // ensure we handle exceptions thrown when matching predicate
            if (predicate != null) {
                matches = predicate.matches(exchange);
            }


            if (LOG.isDebugEnabled()) {
                LOG.debug("#{} - {} matches: {} for: {}", new Object[]{i, predicate, matches, exchange});
            }

            if (matches) {
                matchedFilters.add(filter);
            }
        }
        return matchedFilters;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("choice{");
        boolean first = true;
        for (FilterProcessor processor : filters) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append("when ");
            builder.append(processor.getPredicate().toString());
            builder.append(": ");
            builder.append(processor.getProcessor());
        }
        if (otherwise != null) {
            builder.append(", otherwise: ");
            builder.append(otherwise);
        }
        builder.append("}");
        return builder.toString();
    }

    public String getTraceLabel() {
        return "choice";
    }

    public List<FilterProcessor> getFilters() {
        return filters;
    }

    public Processor getOtherwise() {
        return otherwise;
    }

    public List<Processor> next() {
        if (!hasNext()) {
            return null;
        }
        List<Processor> answer = new ArrayList<Processor>();
        if (filters != null) {
            answer.addAll(filters);
        }
        if (otherwise != null) {
            answer.add(otherwise);
        }
        return answer;
    }

    public boolean hasNext() {
        return otherwise != null || (filters != null && !filters.isEmpty());
    }


}
