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
package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Service;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.util.ServiceHelper;
import com.nxttxn.vramel.util.VramelContextHelper;
import org.apache.camel.util.LRUSoftCache;

import java.util.Map;



/**
 * Endpoint registry which is a based on a {@link org.apache.camel.util.LRUSoftCache}.
 * <p/>
 * We use a soft reference cache to allow the JVM to re-claim memory if it runs low on memory.
 */
public class EndpointRegistry extends LRUSoftCache<EndpointKey, Endpoint> implements Service {
    private static final long serialVersionUID = 1L;
    private final VramelContext context;

    public EndpointRegistry(VramelContext context) {
        // do not stop on eviction, as the endpoint may still be in use
        super(VramelContextHelper.getMaximumEndpointCacheSize(context), VramelContextHelper.getMaximumEndpointCacheSize(context), false);
        this.context = context;
    }

    public EndpointRegistry(VramelContext context, Map<EndpointKey, Endpoint> endpoints) {
        this(context);
        putAll(endpoints);
    }

    @Override
    public void start() throws Exception {
        resetStatistics();
    }

    @Override
    public void stop() throws Exception {
        if (!isEmpty()) {
            ServiceHelper.stopServices(values());
        }
        purge();
    }

    /**
     * Purges the cache
     */
    public void purge() {
        clear();
    }

    @Override
    public String toString() {
        return "EndpointRegistry for " + context.getName() + ", capacity: " + getMaxCacheSize();
    }
}
