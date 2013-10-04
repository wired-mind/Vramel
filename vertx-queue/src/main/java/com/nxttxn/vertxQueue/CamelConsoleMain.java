package com.nxttxn.vertxQueue;

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

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * A main class to run the example from your editor.
 */
public final class CamelConsoleMain {
    private static final Logger LOG = LoggerFactory.getLogger(CamelConsoleMain.class);
    private static final int defaultConcurrentConsumers = 40;
    public static final String defaultQueueName = "default";

    private CamelConsoleMain() {
    }

    public static void main(String[] args) throws Exception {
        // Main makes it easier to run a Spring application
        final Main main = new Main();

        main.addOption(main.new ParameterOption("cluster-port", "Cluster Port", "The vertx cluster port", "cluster-port") {
            @Override
            protected void doProcess(String arg, String parameter, LinkedList<String> remainingArgs) {
                System.setProperty("cluster-port", parameter);
            }
        });
        //for now configure a default queue automatically. Don't allow any overriding or customization. Feel free to add these options if needed.
        setupQueue(main, defaultQueueName, defaultConcurrentConsumers);

        main.addOption(main.new ParameterOption("q", "Queue", "Name of queue to setup", "queue") {
            @Override
            protected void doProcess(String arg, final String parameter, LinkedList<String> remainingArgs) {
                setupQueue(main, parameter, defaultConcurrentConsumers);
            }
        });


        // configure the location of the Spring XML file
        main.setApplicationContextUri("META-INF/spring/camel-context.xml");
        // enable hangup support allows Camel to detect when the JVM is terminated
        main.enableHangupSupport();
        // run and block until Camel is stopped (or JVM terminated)
        main.run(args);
    }

    private static void setupQueue(Main main, String queueName, int concurrentConsumers) {
        main.addRouteBuilder(configureRouteForQueue(queueName, concurrentConsumers));
        main.addRouteBuilder(configureBacklogFlushRouteForQueue(queueName, concurrentConsumers));
    }

    private static RouteBuilder configureBacklogFlushRouteForQueue(final String queueName, final int concurrentConsumers) {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final String backlogName = String.format("%s.backlog", queueName);

                onException(Throwable.class)
                        .maximumRedeliveries(8)
                        .useExponentialBackOff()
                        .retryAttemptedLogLevel(LoggingLevel.WARN)
                        .backOffMultiplier(2)
                        .redeliveryDelay(30 * 1000)
                        .maximumRedeliveryDelay(60 * 60 * 1000);

                fromF("hazelcast:seda:%s?concurrentConsumers=%s&transacted=true", backlogName, concurrentConsumers)

                        .choice()
                        .when(method("backlogRoutePolicy", "lastExchangeSuccessful"))
                        .toF("hazelcast:queue:%s", queueName)
                        .otherwise()
                        .throwException(new RuntimeException("Cannot flush backlog yet. Last exchange failed."));
            }
        };
    }

    private static RouteBuilder configureRouteForQueue(final String queueName, final int concurrentConsumers) {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {


                final String backlogName = String.format("%s.backlog", queueName);

                onException(EventBusException.class)
                        .maximumRedeliveries(2)
                        .useExponentialBackOff()
                        .retryAttemptedLogLevel(LoggingLevel.WARN)
                        .backOffMultiplier(2)
                        .redeliveryDelay(30 * 1000)
                        .maximumRedeliveryDelay(5 * 60 * 1000)
                        .handled(true)
                        .setProperty("sentToBacklog", constant(true))
                        .to("hazelcast:queue:" + backlogName);

                fromF("hazelcast:seda:%s?concurrentConsumers=%s&transacted=true", queueName, concurrentConsumers)
                        .routePolicyRef("backlogRoutePolicy")
                        .processRef("eventBusBridge")
                        .setProperty("sentToEventBus", constant(true));


            }
        };
    }
}