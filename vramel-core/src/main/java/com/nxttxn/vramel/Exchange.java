package com.nxttxn.vramel;

import com.nxttxn.vramel.processor.UnitOfWorkProcessor;
import com.nxttxn.vramel.spi.Synchronization;
import com.nxttxn.vramel.spi.UnitOfWork;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Exchange {
    String AGGREGATION_STRATEGY             = "VramelAggregationStrategy";

    String BEAN_METHOD_NAME           = "VramelBeanMethodName";
    String BEAN_MULTI_PARAMETER_ARRAY = "VramelBeanMultiParameterArray";
    String CHARSET_NAME      = "VramelCharsetName";
    String CONTENT_TYPE      = "content-type";
    String CONTENT_ENCODING  = "Content-Encoding";
    String CREATED_TIMESTAMP = "VramelCreatedTimestamp";
    String CORRELATION_ID    = "VramelCorrelationId";

    String DEFAULT_CHARSET_PROPERTY  = "org.apache.camel.default.charset";
    String EXCEPTION_CAUGHT           = "VramelExceptionCaught";
    String FAILURE_ENDPOINT     = "VramelFailureEndpoint";
    String FAILURE_HANDLED      = "VramelFailureHandled";
    String FAILURE_ROUTE_ID     = "VramelFailureRouteId";
    String ERRORHANDLER_HANDLED       = "VramelErrorHandlerHandled";
    String HTTP_METHOD             = "VramelHttpMethod";
    String HTTP_QUERY              = "VramelHttpQuery";
    String HTTP_RESPONSE_CODE      = "VramelHttpResponseCode";
    String HTTP_STATUS_MESSAGE     = "VramelHttpStatusMessage";

    String FILE_NAME            = "VramelFileName";
    String FILE_NAME_ONLY       = "VramelFileNameOnly";
    String FILE_LAST_MODIFIED   = "VramelFileLastModified";
    String FILE_LENGTH          = "VramelFileLength";
    String MAXIMUM_CACHE_POOL_SIZE     = "VramelMaximumCachePoolSize";
    String MAXIMUM_ENDPOINT_CACHE_SIZE = "VramelMaximumEndpointCacheSize";

    String ON_COMPLETION      = "VramelOnCompletion";

    String ROUTE_STOP              = "VramelRouteStop";
    String SLIP_ENDPOINT      = "VramelSlipEndpoint";


    String SPLIT_INDEX        = "VramelCamelSplitIndex";
    String SPLIT_COMPLETE     = "VramelCamelSplitComplete";
    String SPLIT_SIZE         = "VramelCamelSplitSize";

    String TO_ENDPOINT           = "VramelToEndpoint";

    String TIMER_COUNTER         = "VramelTimerCounter";
    String TIMER_FIRED_TIME      = "VramelTimerFiredTime";
    String TIMER_NAME            = "VramelTimerName";
    String TIMER_PERIOD          = "VramelTimerPeriod";
    String TIMER_TIME            = "VramelTimerTime";

    String AGGREGATION_RESULTS = "VramelAggregationResults";

    Message getIn();

    Message getOut();

    void setOut(Message message);

    Boolean isFailed();


    void setException(Throwable exception);

    Exception getException();
    <T> T getException(Class<T> type);

    void setIn(Message message);

    Object getProperty(String key);
    Object getProperty(String key, Object defaultValue);
    <T> T getProperty(String key, Class<T> type);
    <T> T getProperty(String key, Object defaultValue, Class<T> type);

    void setProperty(String key, Object value);

    Object removeProperty(String name);

    Exchange copy();

    boolean hasOut();

    void setProperties(Map<String, Object> properties);

    boolean hasProperties();

    Map<String, Object> getProperties();

    VramelContext getContext();

    String getExchangeId();

    UnitOfWork getUnitOfWork();

    void setUnitOfWork(UnitOfWork uow);

    String getFromRouteId();

    void setFromRouteId(String fromRouteId);


    void setExchangeId(String id);

    ExchangePattern getPattern();
    /**
     * Allows the {@link ExchangePattern} (MEP) of this exchange to be customized.
     *
     * This typically won't be required as an exchange can be created with a specific MEP
     * by calling {@link Endpoint#createExchange(ExchangePattern)} but it is here just in case
     * it is needed.
     *
     * @param pattern  the pattern
     */
    void setPattern(ExchangePattern pattern);
    /**
     * Adds a {@link org.apache.camel.spi.Synchronization} to be invoked as callback when
     * this exchange is completed.
     *
     * @param onCompletion  the callback to invoke on completion of this exchange
     */
    void addOnCompletion(Synchronization onCompletion);

    /**
     * Checks if the passed {@link org.apache.camel.spi.Synchronization} instance is
     * already contained on this exchange.
     *
     * @param onCompletion  the callback instance that is being checked for
     * @return <tt>true</tt>, if callback instance is already contained on this exchange, else <tt>false</tt>
     */
    boolean containsOnCompletion(Synchronization onCompletion);

    /**
     * Handover all the on completions from this exchange to the target exchange.
     *
     * @param target the target exchange
     */
    void handoverCompletions(Exchange target);

    /**
     * Handover all the on completions from this exchange
     *
     * @return the on completions
     */
    List<Synchronization> handoverCompletions();

    /**
     * Returns the endpoint which originated this message exchange if a consumer on an endpoint
     * created the message exchange, otherwise this property will be <tt>null</tt>
     */
    Endpoint getFromEndpoint();
}
