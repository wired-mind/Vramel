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

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultEndpoint;
import com.nxttxn.vramel.spi.UriEndpoint;
import com.nxttxn.vramel.spi.UriParam;
import org.vertx.java.core.AsyncResultHandler;

import java.util.Date;


/**
 * Represents a timer endpoint that can generate periodic inbound exchanges triggered by a timer.
 *
 * @version
 */

@UriEndpoint(scheme = "timer", consumerClass = TimerConsumer.class)
public class TimerEndpoint extends DefaultEndpoint implements MultipleConsumersSupport {
    @UriParam
    private String timerName;
    @UriParam
    private Date time;
    @UriParam
    private long period = 1000;
    @UriParam
    private long delay = 1000;
    @UriParam
    private boolean fixedRate;
    @UriParam
    private long repeatCount;

    public TimerEndpoint() {
    }



    public TimerEndpoint(String uri, Component component, String timerName) {
        super(uri, component);
        this.timerName = timerName;
    }

    @Override
    protected void doStart(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
        super.doStart(asyncResultHandler);
        // do nothing, the timer will be set when the first consumer will request it
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }



    public Producer createProducer() throws Exception {
        throw new RuntimeVramelException("Cannot produce to a TimerEndpoint: " + getEndpointUri());
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        return new TimerConsumer(this, processor);
    }


    public boolean isMultipleConsumersSupported() {
        return true;
    }

    public String getTimerName() {
        if (timerName == null) {
            timerName = getEndpointUri();
        }
        return timerName;
    }

    public void setTimerName(String timerName) {
        this.timerName = timerName;
    }



    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public boolean isFixedRate() {
        return fixedRate;
    }
    public void setFixedRate(boolean fixedRate) {
        this.fixedRate = fixedRate;
    }

     public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public long getRepeatCount() {
        return repeatCount;
    }


    public void setRepeatCount(long repeatCount) {
        this.repeatCount = repeatCount;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }


    public boolean isSingleton() {
        return true;
    }







    public String getEndpointUri() {
        return super.getEndpointUri();
    }



}
