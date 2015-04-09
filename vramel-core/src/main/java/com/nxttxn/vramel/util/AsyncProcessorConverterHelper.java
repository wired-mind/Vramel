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
package com.nxttxn.vramel.util;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import org.vertx.java.core.AsyncResultHandler;

import java.util.ArrayList;
import java.util.List;



/**
 * A simple converter that can convert any {@link Processor} to an {@link AsyncProcessor}.
 * Processing will still occur synchronously but it will provide the required
 * notifications that the caller expects.
 *
 * @version
 */
public final class AsyncProcessorConverterHelper {

    private AsyncProcessorConverterHelper() {
        // Helper class
    }

    /**
     * Creates a {@link AsyncProcessor} that delegates to the given processor.
     * It is important that this implements {@link DelegateProcessor}
     */
    private static final class ProcessorToAsyncProcessorBridge implements DelegateProcessor, AsyncProcessor, Navigate<Processor>, Service {
        protected Processor processor;

        private ProcessorToAsyncProcessorBridge(Processor processor) {
            this.processor = processor;
        }

        public boolean process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) {
            if (processor == null) {
                // no processor then we are done
                optionalAsyncResultHandler.done(exchange);
                return true;
            }
            try {
                processor.process(exchange);
            } catch (Throwable e) {
                // must catch throwable so we catch all
                exchange.setException(e);
            } finally {
                // we are bridging a sync processor as async so callback with true
                optionalAsyncResultHandler.done(exchange);
            }
            return true;
        }

        @Override
        public String toString() {
            if (processor != null) {
                return processor.toString();
            } else {
                return "Processor is null";
            }
        }

        public void process(Exchange exchange) throws Exception {
            processNext(exchange);
        }

        protected void processNext(Exchange exchange) throws Exception {
            if (processor != null) {
                processor.process(exchange);
            }
        }

        public void start(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
            ServiceHelper.startServices(processor);
        }

        public void stop() throws Exception {
            ServiceHelper.stopServices(processor);
        }

        public boolean hasNext() {
            return processor != null;
        }

        public List<Processor> next() {
            if (!hasNext()) {
                return null;
            }
            List<Processor> answer = new ArrayList<Processor>(1);
            answer.add(processor);
            return answer;
        }

        @Override
        public Processor getProcessor() {
            return processor;
        }
    }

    public static AsyncProcessor convert(Processor value) {
        if (value instanceof AsyncProcessor) {
            return (AsyncProcessor)value;
        }
        return new ProcessorToAsyncProcessorBridge(value);
    }
}
