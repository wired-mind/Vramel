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
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.builder.ExpressionBuilder;
import com.nxttxn.vramel.builder.ProcessorBuilder;
import com.nxttxn.vramel.model.language.ExpressionDefinition;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.util.ObjectHelper;
import org.apache.camel.spi.Required;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;



/**
 * Represents an XML &lt;setProperty/&gt; element
 */
@XmlRootElement(name = "setProperty")
@XmlAccessorType(XmlAccessType.FIELD)
public class SetPropertyDefinition extends NoOutputExpressionNode {
    @XmlAttribute(required = true)
    private String propertyName;

    public SetPropertyDefinition() {
    }

    public SetPropertyDefinition(String propertyName, ExpressionDefinition expression) {
        super(expression);
        setPropertyName(propertyName);
    }

    public SetPropertyDefinition(String propertyName, Expression expression) {
        super(expression);
        setPropertyName(propertyName);
    }

    public SetPropertyDefinition(String propertyName, String value) {
        super(ExpressionBuilder.constantExpression(value));
        setPropertyName(propertyName);
    }

    @Override
    public String toString() {
        return "SetProperty[" + getPropertyName() + ", " + getExpression() + "]";
    }

    @Override
    public String getLabel() {
        return "setProperty[" + getPropertyName() + "]";
    }



    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        ObjectHelper.notNull(getPropertyName(), "propertyName", this);
        Expression expr = getExpression().createExpression(flowContext);
        return ProcessorBuilder.setProperty(getPropertyName(), expr);
    }

    @Required
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }

}
