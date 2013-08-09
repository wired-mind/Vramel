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
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;


/**
 * Information used by Camel to perform method invocation.
 *
 * @version
 */
public interface MethodInvocation {

    Method getMethod();

    Object[] getArguments();

    void proceed();

    Object getThis();

    AccessibleObject getStaticPart();
}
