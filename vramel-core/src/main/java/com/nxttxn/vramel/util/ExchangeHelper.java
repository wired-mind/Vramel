package com.nxttxn.vramel.util;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.spi.UnitOfWork;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    /**
     * Extracts the body from the given exchange.
     * <p/>
     * If the exchange pattern is provided it will try to honor it and retrieve the body
     * from either IN or OUT according to the pattern.
     *
     * @param exchange the exchange
     * @param pattern  exchange pattern if given, can be <tt>null</tt>
     * @return the result body, can be <tt>null</tt>.
     * @throws VramelExecutionException is thrown if the processing of the exchange failed
     */
    public static Object extractResultBody(Exchange exchange, ExchangePattern pattern) {
        Object answer = null;
        if (exchange != null) {
            // rethrow if there was an exception during execution
            if (exchange.getException() != null) {
                throw ObjectHelper.wrapVramelExecutionException(exchange, exchange.getException());
            }

            // result could have a fault message
            if (hasFaultMessage(exchange)) {
                return exchange.getOut().getBody();
            }

            // okay no fault then return the response according to the pattern
            // try to honor pattern if provided
            boolean notOut = pattern != null && !pattern.isOutCapable();
            boolean hasOut = exchange.hasOut();
            if (hasOut && !notOut) {
                // we have a response in out and the pattern is out capable
                answer = exchange.getOut().getBody();
            } else if (!hasOut && exchange.getPattern() == ExchangePattern.InOptionalOut) {
                // special case where the result is InOptionalOut and with no OUT response
                // so we should return null to indicate this fact
                answer = null;
            } else {
                // use IN as the response
                answer = exchange.getIn().getBody();
            }
        }
        return answer;
    }

    /**
     * Tests whether the exchange has a fault message set and that its not null.
     *
     * @param exchange the exchange
     * @return <tt>true</tt> if fault message exists
     */
    public static boolean hasFaultMessage(Exchange exchange) {
        return exchange.hasOut() && exchange.getOut().isFault() && exchange.getOut().getBody() != null;
    }


    /**
     * Extracts the body from the given future, that represents a handle to an asynchronous exchange.
     * <p/>
     * Will wait until the future task is complete.
     *
     * @param context the camel context
     * @param future  the future handle
     * @param type    the expected body response type
     * @return the result body, can be <tt>null</tt>.
     * @throws VramelExecutionException is thrown if the processing of the exchange failed
     */
    public static <T> T extractFutureBody(VramelContext context, Future<Object> future, Class<T> type) {
        try {
            return doExtractFutureBody(context, future.get(), type);
        } catch (InterruptedException e) {
            throw ObjectHelper.wrapRuntimeCamelException(e);
        } catch (ExecutionException e) {
            // execution failed due to an exception so rethrow the cause
            throw ObjectHelper.wrapVramelExecutionException(null, e.getCause());
        } finally {
            // its harmless to cancel if task is already completed
            // and in any case we do not want to get hold of the task a 2nd time
            // and its recommended to cancel according to Brian Goetz in his Java Concurrency in Practice book
            future.cancel(true);
        }
    }

    /**
     * Extracts the body from the given future, that represents a handle to an asynchronous exchange.
     * <p/>
     * Will wait for the future task to complete, but waiting at most the timeout value.
     *
     * @param context the camel context
     * @param future  the future handle
     * @param timeout timeout value
     * @param unit    timeout unit
     * @param type    the expected body response type
     * @return the result body, can be <tt>null</tt>.
     * @throws VramelExecutionException is thrown if the processing of the exchange failed
     * @throws java.util.concurrent.TimeoutException is thrown if a timeout triggered
     */
    public static <T> T extractFutureBody(VramelContext context, Future<Object> future, long timeout, TimeUnit unit, Class<T> type) throws TimeoutException {
        try {
            if (timeout > 0) {
                return doExtractFutureBody(context, future.get(timeout, unit), type);
            } else {
                return doExtractFutureBody(context, future.get(), type);
            }
        } catch (InterruptedException e) {
            // execution failed due interruption so rethrow the cause
            throw ObjectHelper.wrapVramelExecutionException(null, e);
        } catch (ExecutionException e) {
            // execution failed due to an exception so rethrow the cause
            throw ObjectHelper.wrapVramelExecutionException(null, e.getCause());
        } finally {
            // its harmless to cancel if task is already completed
            // and in any case we do not want to get hold of the task a 2nd time
            // and its recommended to cancel according to Brian Goetz in his Java Concurrency in Practice book
            future.cancel(true);
        }
    }

    private static <T> T doExtractFutureBody(VramelContext context, Object result, Class<T> type) {
        if (result == null) {
            return null;
        }
        if (type.isAssignableFrom(result.getClass())) {
            return type.cast(result);
        }
        if (result instanceof Exchange) {
            Exchange exchange = (Exchange) result;
            Object answer = ExchangeHelper.extractResultBody(exchange, exchange.getPattern());
            return context.getTypeConverter().convertTo(type, exchange, answer);
        }
        return context.getTypeConverter().convertTo(type, result);
    }

    /**
     * Creates a new instance and copies from the current message exchange so that it can be
     * forwarded to another destination as a new instance. Unlike regular copy this operation
     * will not share the same {@link org.apache.camel.spi.UnitOfWork} so its should be used
     * for async messaging, where the original and copied exchange are independent.
     *
     * @param exchange original copy of the exchange
     * @param handover whether the on completion callbacks should be handed over to the new copy.
     */
    public static Exchange createCorrelatedCopy(Exchange exchange, boolean handover) {
        String id = exchange.getExchangeId();

        Exchange copy = exchange.copy();
        // do not share the unit of work
        copy.setUnitOfWork(null);
        // hand over on completion to the copy if we got any
        UnitOfWork uow = exchange.getUnitOfWork();
        if (handover && uow != null) {
            uow.handoverSynchronization(copy);
        }
        // set a correlation id so we can track back the original exchange
        copy.setProperty(Exchange.CORRELATION_ID, id);
        return copy;
    }


}
