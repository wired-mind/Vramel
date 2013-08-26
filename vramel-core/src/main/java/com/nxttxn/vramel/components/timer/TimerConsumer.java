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
package com.nxttxn.vramel.components.timer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;


import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.impl.DefaultConsumer;
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;



/**
 * The timer consumer.
 *
 * @version
 */
public class TimerConsumer extends DefaultConsumer {
    private static final transient Logger LOG = LoggerFactory.getLogger(TimerConsumer.class);
    private final TimerEndpoint endpoint;
    private volatile TimerTask task;
    private final Vertx vertx;

    public TimerConsumer(final TimerEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;

        vertx = getEndpoint().getVramelContext().getVertx();

        configureTask(new Handler<Long>() {
            // counter
            private final AtomicLong counter = new AtomicLong();

            @Override
            public void handle(Long timerId) {


                try {
                    long count = counter.incrementAndGet();

                    boolean fire = endpoint.getRepeatCount() <= 0 || count <= endpoint.getRepeatCount();
                    if (fire) {
                        sendTimerExchange(count);
                    } else {
                        // no need to fire anymore as we exceeded repeat count
                        LOG.debug("Cancelling {} timer as repeat count limit reached after {} counts.", endpoint.getTimerName(), endpoint.getRepeatCount());
                        vertx.cancelTimer(timerId);
                    }
                } catch (Throwable e) {
                    // catch all to avoid the JVM closing the thread and not firing again
                    LOG.warn("Error processing exchange. This exception will be ignored, to let the timer be able to trigger again.", e);
                }
            }
        });

    }



    protected void configureTask(final Handler<Long> task) {

            if (endpoint.getTime() != null) {

                final long computedDelay = endpoint.getTime().getTime() - DateTime.now().getMillis();
                if (endpoint.getPeriod() > 0) {
                    delayTask(computedDelay, new TaskRepeater(task, endpoint.getPeriod()));
                } else {
                    delayTask(computedDelay, task);
                }
            } else {
                if (endpoint.getPeriod() > 0) {
                    delayTask(endpoint.getDelay(), new TaskRepeater(task, endpoint.getPeriod()));
                } else {
                    delayTask(endpoint.getDelay(), task);
                }
            }
    }

    private void delayTask(long delay, Handler<Long> delayDone) {
        vertx.setTimer(delay, delayDone);
    }

    protected void sendTimerExchange(long counter) {
        Exchange exchange = endpoint.createExchange();
        exchange.setProperty(Exchange.TIMER_COUNTER, counter);
        exchange.setProperty(Exchange.TIMER_NAME, endpoint.getTimerName());
        exchange.setProperty(Exchange.TIMER_TIME, endpoint.getTime());
        exchange.setProperty(Exchange.TIMER_PERIOD, endpoint.getPeriod());

        Date now = new Date();
        exchange.setProperty(Exchange.TIMER_FIRED_TIME, now);
        // also set now on in header with same key as quartz to be consistent
        exchange.getIn().setHeader("firedTime", now);

        LOG.trace("Timer {} is firing #{} count", endpoint.getTimerName(), counter);
        try {
            getAsyncProcessor().process(exchange, new OptionalAsyncResultHandler() {
                @Override
                public void handle(AsyncExchangeResult asyncExchangeResult) {
                    final Exchange result = asyncExchangeResult.result.get();
                    // handle any thrown exception
                    if (result.getException() != null) {
                        getExceptionHandler().handleException("Error processing exchange", result, result.getException());
                    }
                }
            });
        } catch (Exception e) {
            exchange.setException(e);
        }

        // handle any thrown exception
        if (exchange.getException() != null) {
            getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
        }
    }

    private class TaskRepeater implements Handler<Long> {
        private final Handler<Long> task;
        private long period;

        public TaskRepeater(Handler<Long> task, long period) {
            this.task = task;
            this.period = period;
        }

        @Override
        public void handle(Long event) {
            vertx.setPeriodic(period, task);
        }
    }
}
