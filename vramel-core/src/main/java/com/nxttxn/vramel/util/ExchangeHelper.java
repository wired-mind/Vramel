package com.nxttxn.vramel.util;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.TypeConversionException;
import com.nxttxn.vramel.TypeConverter;
import com.nxttxn.vramel.VramelContext;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 1:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExchangeHelper {
    public static void prepareAggregation(Exchange oldExchange, Exchange newExchange) {
        // move body/header from OUT to IN
        if (oldExchange != null) {
            if (oldExchange.hasOut()) {
                oldExchange.setIn(oldExchange.getOut());
                oldExchange.setOut(null);
            }

        }


        if (newExchange != null) {
            if (newExchange.hasOut()) {
                newExchange.setIn(newExchange.getOut());
                newExchange.setOut(null);
            }
        }

    }

    /**
     * Sets the exchange to be failure handled.
     *
     * @param exchange  the exchange
     */
    public static void setFailureHandled(Exchange exchange) {
        exchange.setProperty(Exchange.FAILURE_HANDLED, Boolean.TRUE);
        // clear exception since its failure handled
        exchange.setException(null);
    }

    /**
     * Tests whether the exchange has already been handled by the error handler
     *
     * @param exchange the exchange
     * @return <tt>true</tt> if handled already by error handler, <tt>false</tt> otherwise
     */
    public static boolean hasExceptionBeenHandledByErrorHandler(Exchange exchange) {
        return Boolean.TRUE.equals(exchange.getProperty(Exchange.ERRORHANDLER_HANDLED));
    }

    /**
     * Converts the value to the given expected type
     *
     * @return the converted value
     * @throws org.apache.camel.TypeConversionException is thrown if error during type conversion
     */
    public static <T> T convertToType(Exchange exchange, Class<T> type, Object value) throws TypeConversionException {
        VramelContext vramelContext = exchange.getContext();
        ObjectHelper.notNull(vramelContext, "VramelContext of Exchange");
        TypeConverter converter = vramelContext.getTypeConverter();
        if (converter != null) {
            return converter.convertTo(type, exchange, value);
        }
        return null;
    }
}
