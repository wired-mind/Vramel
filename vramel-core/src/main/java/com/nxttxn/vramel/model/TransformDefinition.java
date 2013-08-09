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
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.processor.TransformProcessor;
import com.nxttxn.vramel.spi.FlowContext;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;



/**
 * Represents an XML &lt;transform/&gt; element
 */
@XmlRootElement(name = "transform")
@XmlAccessorType(XmlAccessType.FIELD)
public class TransformDefinition extends NoOutputExpressionNode {

    public TransformDefinition() {
    }

    public TransformDefinition(Expression expression) {
        super(expression);
    }

    @Override
    public String toString() {
        return "Transform[" + getExpression() + "]";
    }


    @Override
    public String getLabel() {
        return "transform[" + getExpression() + "]";
    }

    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        Expression expr = getExpression().createExpression(flowContext);
        return new TransformProcessor(expr);
    }

}
