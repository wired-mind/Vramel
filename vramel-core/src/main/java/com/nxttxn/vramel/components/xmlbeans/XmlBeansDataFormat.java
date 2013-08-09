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
package com.nxttxn.vramel.components.xmlbeans;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.RuntimeVramelException;
import com.nxttxn.vramel.spi.DataFormat;
import org.apache.xmlbeans.XmlObject;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A <a href="http://camel.apache.org/data-format.html">data format</a>
 * ({@link DataFormat}) using XmlBeans to marshal to and from XML
 */
public class XmlBeansDataFormat implements DataFormat {



    public void marshal(Exchange exchange, Object body, OutputStream stream) throws Exception {

        final boolean isXmlObject = XmlObject.class.isInstance(body);
        if (!isXmlObject) {
            throw new RuntimeVramelException("Cannot marshal using XmlBeans. Must be XmlObject");
        }
        //no type converter implementation yet
//        XmlObject object = ExchangeHelper.convertToMandatoryType(exchange, XmlObject.class, body);
        ((XmlObject)body).save(stream);
    }


    public Object unmarshal(Exchange exchange, InputStream stream) throws Exception {
        return XmlObject.Factory.parse(stream);
    }
}
