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
package com.nxttxn.vramel.spi;


import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.Service;

/**
 * An object representing the unit of work processing an {@link Exchange}
 * which allows the use of {@link Synchronization} hooks. This object might map one-to-one with
 * a transaction in JPA or Spring; or might not.
 */
public interface UnitOfWork extends Service {




    /**
     * Returns the unique ID of this unit of work, lazily creating one if it does not yet have one
     *
     * @return the unique ID
     */
    String getId();

    /**
     * Gets the original IN {@link Message} this Unit of Work was started with.
     * <p/>
     * <b>Important: </b> This is subject for change in a later Camel release, where we plan to only
     * support getting the original IN message if you have enabled this option explicit.
     *
     * @return the original IN {@link Message}, may return <tt>null</tt> in a later Camel release (see important note).
     */
    Message getOriginalInMessage();




    /**
     * Gets the {@link FlowContext} that this {@link UnitOfWork} currently is being routed through.
     * <p/>
     * Notice that an {@link Exchange} can be routed through multiple routes and thus the
     * {@link org.apache.camel.spi.RouteContext} can change over time.
     *
     * @return the route context
     * @see #pushFlowContext(FlowContext)
     * @see #popFlowContext()
     */
    FlowContext getFlowContext();

    /**
     * Pushes the {@link FlowContext} that this {@link UnitOfWork} currently is being routed through.
     * <p/>
     * Notice that an {@link Exchange} can be routed through multiple routes and thus the
     * {@link org.apache.camel.spi.RouteContext} can change over time.
     *
     * @param flowContext the route context
     */
    void pushFlowContext(FlowContext flowContext);

    /**
     * When finished being routed under the current {@link org.apache.camel.spi.RouteContext}
     * it should be removed.
     *
     * @return the route context or <tt>null</tt> if none existed
     */
    FlowContext popFlowContext();



}
