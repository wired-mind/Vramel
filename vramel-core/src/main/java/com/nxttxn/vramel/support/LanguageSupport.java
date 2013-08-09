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
package com.nxttxn.vramel.support;

import com.nxttxn.vramel.ExpressionIllegalSyntaxException;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.VramelContextAware;
import com.nxttxn.vramel.spi.Language;
import com.nxttxn.vramel.util.IOHelper;
import com.nxttxn.vramel.util.ResourceHelper;
import org.apache.camel.IsSingleton;

import java.io.InputStream;



/**
 * Base language for {@link Language} implementations.
 */
public abstract class LanguageSupport implements Language, IsSingleton, VramelContextAware {

    public static final String RESOURCE = "resource:";

    private VramelContext vramelContext;

    public VramelContext getVramelContext() {
        return vramelContext;
    }

    public void setVramelContext(VramelContext vramelContext) {
        this.vramelContext = vramelContext;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * Loads the resource if the given expression is referring to an external resource by using
     * the syntax <tt>resource:scheme:uri<tt>.
     * If the expression is not referring to a resource, then its returned as is.
     * <p/>
     * For example <tt>resource:classpath:mygroovy.groovy</tt> to refer to a groovy script on the classpath.
     *
     * @param expression the expression
     * @return the expression
     * @throws ExpressionIllegalSyntaxException is thrown if error loading the resource
     */
    protected String loadResource(String expression) throws ExpressionIllegalSyntaxException {
        if (vramelContext != null && expression.startsWith(RESOURCE)) {
            String uri = expression.substring(RESOURCE.length());
            InputStream is = null;
            try {
                is = ResourceHelper.resolveMandatoryResourceAsInputStream(vramelContext.getClassResolver(), uri);
                expression = vramelContext.getTypeConverter().mandatoryConvertTo(String.class, is);
            } catch (Exception e) {
                throw new ExpressionIllegalSyntaxException(expression, e);
            } finally {
                IOHelper.close(is);
            }
        }
        return expression;
    }
}
