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
package com.nxttxn.vramel;

import com.nxttxn.vramel.spi.FlowContext;



/**
 * Channel acts as a channel between {@link AsyncProcessor}s in the route graph.
 * <p/>
 * The channel is responsible for routing the {@link Exchange} to the next {@link AsyncProcessor} in the route graph.
 *
 * @version
 */
public interface Channel extends AsyncProcessor, Navigate<Processor> {

    /**
     * Sets the processor that the channel should route the {@link Exchange} to.
     *
     * @param next  the next processor
     */
    void setNextProcessor(Processor next);

    /**
     * Sets the {@link org.apache.camel.processor.ErrorHandler} this Channel uses.
     *
     * @param errorHandler the error handler
     */
    void setErrorHandler(Processor errorHandler);

    /**
     * Gets the {@link org.apache.camel.processor.ErrorHandler} this Channel uses.
     *
     * @return the error handler, or <tt>null</tt> if no error handler is used.
     */
    Processor getErrorHandler();


    /**
     * Gets the wrapped output that at runtime should be delegated to.
     *
     * @return the output to route the {@link Exchange} to
     */
    Processor getOutput();

    /**
     * Sets the wrapped output that at runtime should be delegated to.
     *
     * @param output the output to route the {@link Exchange} to
     */
    void setOutput(Processor output);

    /**
     * Gets the next {@link AsyncProcessor} to route to (not wrapped)
     *
     * @return  the next processor
     */
    Processor getNextProcessor();

    /**
     * Gets the {@link FlowContext}
     *
     * @return the route context
     */
    FlowContext getFlowContext();
}
