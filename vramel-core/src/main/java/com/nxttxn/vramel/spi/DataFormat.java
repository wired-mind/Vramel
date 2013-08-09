package com.nxttxn.vramel.spi;

import com.nxttxn.vramel.Exchange;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
//copy in camel's interface for DataFormat
public interface DataFormat {
    /**
     * Marshals the object to the given Stream.
     *
     *
     * @param exchange
     * @param graph     the object to be marshalled
     * @param stream    the output stream to write the marshalled result to
     * @throws Exception can be thrown
     */
    void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception;

    /**
     * Unmarshals the given stream into an object.
     * <p/>
     * <b>Notice:</b> The result is set as body on the exchange OUT message.
     * It is possible to mutate the OUT message provided in the given exchange parameter.
     * For instance adding headers to the OUT message will be preserved.
     * <p/>
     * It's also legal to return the <b>same</b> passed <tt>exchange</tt> as is but also a
     * {@link Message} object as well which will be used as the OUT message of <tt>exchange</tt>.
     *
     *
     * @param stream      the input stream with the object to be unmarshalled
     * @return            the unmarshalled object
     * @throws Exception can be thrown
     */
    Object unmarshal(Exchange exchange, InputStream stream) throws Exception;
}
