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

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.model.language.HeaderExpression;
import com.nxttxn.vramel.processor.RoutingSlip;
import com.nxttxn.vramel.spi.FlowContext;

import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Represents an XML &lt;routingSlip/&gt; element
 */
@XmlRootElement(name = "routingSlip")
@XmlAccessorType(XmlAccessType.FIELD)
public class RoutingSlipDefinition<Type extends ProcessorDefinition<Type>> extends NoOutputExpressionNode {
    public static final String DEFAULT_DELIMITER = ",";

    @XmlAttribute
    private String uriDelimiter;
    @XmlAttribute
    private Boolean ignoreInvalidEndpoints;

    public RoutingSlipDefinition() {
        this((String)null, DEFAULT_DELIMITER);
    }

    public RoutingSlipDefinition(String headerName) {
        this(headerName, DEFAULT_DELIMITER);
    }

    public RoutingSlipDefinition(String headerName, String uriDelimiter) {
        super(new HeaderExpression(headerName));
        setUriDelimiter(uriDelimiter);
    }

    public RoutingSlipDefinition(Expression expression, String uriDelimiter) {
        super(expression);
        setUriDelimiter(uriDelimiter);
    }

    public RoutingSlipDefinition(Expression expression) {
        this(expression, DEFAULT_DELIMITER);
    }

    @Override
    public String toString() {
        return "RoutingSlip[" + getExpression() + "]";
    }



    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        Expression expression = getExpression().createExpression(flowContext);
        String delimiter = getUriDelimiter() != null ? getUriDelimiter() : DEFAULT_DELIMITER;

        RoutingSlip routingSlip = new RoutingSlip(flowContext.getVramelContext(), expression, delimiter);

        return routingSlip;
    }

    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        return Collections.emptyList();
    }

    public void setUriDelimiter(String uriDelimiter) {
        this.uriDelimiter = uriDelimiter;
    }

    public String getUriDelimiter() {
        return uriDelimiter;
    }

    public void setIgnoreInvalidEndpoints(Boolean ignoreInvalidEndpoints) {
        this.ignoreInvalidEndpoints = ignoreInvalidEndpoints;
    }

    public Boolean getIgnoreInvalidEndpoints() {
        return ignoreInvalidEndpoints;
    }

    // Fluent API
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public Type end() {
        // allow end() to return to previous type so you can continue in the DSL
        return (Type) super.end();
    }

    /**
     * Ignore the invalidate endpoint exception when try to create a producer with that endpoint
     *
     * @return the builder
     */
    public RoutingSlipDefinition<Type> ignoreInvalidEndpoints() {
        setIgnoreInvalidEndpoints(true);
        return this;
    }

    /**
     * Sets the uri delimiter to use
     *
     * @param uriDelimiter the delimiter
     * @return the builder
     */
    public RoutingSlipDefinition<Type> uriDelimiter(String uriDelimiter) {
        setUriDelimiter(uriDelimiter);
        return this;
    }
}
