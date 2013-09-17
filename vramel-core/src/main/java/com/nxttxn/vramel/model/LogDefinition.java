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

import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.LoggingLevel;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.LogProcessor;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.VramelLogger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;



/**
 * Represents an XML &lt;log/&gt; element
 *
 * @version
 */
@XmlRootElement(name = "log")
@XmlAccessorType(XmlAccessType.FIELD)
public class LogDefinition extends NoOutputDefinition<LogDefinition> {
    @XmlAttribute(required = true)
    private String message;
    @XmlAttribute
    private LoggingLevel loggingLevel;
    @XmlAttribute
    private String logName;
    @XmlAttribute
    private String marker;

    public LogDefinition() {
    }

    public LogDefinition(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Log[" + message + "]";
    }

    @Override
    public String getLabel() {
        return "log";
    }

    @Override
    public String getShortName() {
        return "log";
    }

    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        ObjectHelper.notEmpty(message, "message", this);

        // use simple language for the message string to give it more power
        Expression exp = flowContext.getVramelContext().resolveLanguage("simple").createExpression(message);

        String name = getLogName();
        if (name == null) {
            name = flowContext.getFlow().getId();
        }
        // should be INFO by default
        LoggingLevel level = getLoggingLevel() != null ? getLoggingLevel() : LoggingLevel.INFO;
        VramelLogger logger = new VramelLogger(name, level, getMarker());

        return new LogProcessor(exp, logger);
    }

    @Override
    public void addOutput(ProcessorDefinition<?> output) {
        // add outputs on parent as this log does not support outputs
        getParent().addOutput(output);
    }

    public LoggingLevel getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(LoggingLevel loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLogName() {
        return logName;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }
}