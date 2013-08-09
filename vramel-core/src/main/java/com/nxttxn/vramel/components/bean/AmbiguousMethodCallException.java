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
package com.nxttxn.vramel.components.bean;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.VramelExchangeException;

import java.util.Collection;



/**
 * An exception thrown if an attempted method invocation resulted in an ambiguous method
 * such that multiple methods match the inbound message exchange
 *
 * @version
 */
public class AmbiguousMethodCallException extends VramelExchangeException {

    private final Collection<MethodInfo> methods;

    public AmbiguousMethodCallException(Exchange exchange, Collection<MethodInfo> methods) {
        super("Ambiguous method invocations possible: " + methods, exchange);
        this.methods = methods;
    }

    /**
     * The ambiguous methods for which a single method could not be chosen
     */
    public Collection<MethodInfo> getMethods() {
        return methods;
    }
}
