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
package com.nxttxn.vramel.components.direct;


import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.SuspendableService;
import com.nxttxn.vramel.impl.DefaultConsumer;

/**
 * The direct consumer.
 *
 * @version 
 */
public class DirectConsumer extends DefaultConsumer implements SuspendableService {

    private DirectEndpoint endpoint;

    public DirectConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = (DirectEndpoint) endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        // add consumer to endpoint
        boolean existing = this == endpoint.getConsumer();
        if (!existing && endpoint.hasConsumer(this)) {
            throw new IllegalArgumentException("Cannot add a 2nd consumer to the same endpoint. Endpoint " + endpoint + " only allows one consumer.");
        }
        if (!existing) {
            endpoint.addConsumer(this);
        }
    }

    @Override
    protected void doStop() throws Exception {
        endpoint.removeConsumer(this);
    }

    @Override
    protected void doSuspend() throws Exception {
        endpoint.removeConsumer(this);
    }

    @Override
    protected void doResume() throws Exception {
        // resume by using the start logic
        doStart();
    }

    public void prepareShutdown(boolean forced) {
        // noop
    }
}
