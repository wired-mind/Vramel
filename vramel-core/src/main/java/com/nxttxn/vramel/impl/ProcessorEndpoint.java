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


import com.nxttxn.vramel.*;


public class ProcessorEndpoint extends DefaultEndpoint {
    private Processor processor;

    protected ProcessorEndpoint() {
    }

    @SuppressWarnings("deprecation")
    public ProcessorEndpoint(String endpointUri, VramelContext context, Processor processor) {
        super(endpointUri);
        this.setVramelContext(context);
        this.processor = processor;
    }

    public ProcessorEndpoint(String endpointUri, Component component, Processor processor) {
        super(endpointUri, component);
        this.processor = processor;
    }

    protected ProcessorEndpoint(String endpointUri, Component component) {
        super(endpointUri, component);
    }

    @Deprecated
    public ProcessorEndpoint(String endpointUri, Processor processor) {
        super(endpointUri);
        this.processor = processor;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new DefaultConsumer(this, processor);
    }

    public Producer createProducer() throws Exception {
        return new DefaultProducer(this) {

            @Override
            public void process(Exchange exchange) throws Exception {
                onExchange(exchange);
            }
        };
    }



    public Processor getProcessor() throws Exception {
        if (processor == null) {
            processor = createProcessor();
        }
        return processor;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    protected Processor createProcessor() throws Exception {
        return new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                onExchange(exchange);
            }
        };
    }

    protected void onExchange(Exchange exchange) throws Exception {
        getProcessor().process(exchange);
    }

    public boolean isSingleton() {
        return true;
    }
}
