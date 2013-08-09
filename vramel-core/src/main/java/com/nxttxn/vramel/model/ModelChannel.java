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


import com.nxttxn.vramel.Channel;
import com.nxttxn.vramel.spi.FlowContext;

public interface ModelChannel extends Channel {
    /**
     * Initializes the channel.
     *
     * @param outputDefinition  the route definition the {@link Channel} represents
     * @param flowContext      the route context
     * @throws Exception is thrown if some error occurred
     */
    void initChannel(ProcessorDefinition<?> outputDefinition, FlowContext flowContext) throws Exception;

    /**
     * Post initializes the channel.
     *
     * @param outputDefinition  the route definition the {@link Channel} represents
     * @param flowContext      the route context
     * @throws Exception is thrown if some error occurred
     */
    void postInitChannel(ProcessorDefinition<?> outputDefinition, FlowContext flowContext) throws Exception;

    /**
     * If the initialized output definition contained outputs (children) then we need to
     * set the child so we can leverage fine grained tracing
     *
     * @param child the child
     */
    void setChildDefinition(ProcessorDefinition<?> child);

    /**
     * Gets the definition of the next processor
     *
     * @return the processor definition
     */
    ProcessorDefinition<?> getProcessorDefinition();
}
