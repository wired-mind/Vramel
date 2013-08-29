package com.nxttxn.vramel.support;

import com.google.common.base.Optional;
import com.nxttxn.vramel.*;
import com.nxttxn.vramel.processor.ChildUnitOfWorkProcessor;
import com.nxttxn.vramel.processor.ProcessorExchangePair;
import com.nxttxn.vramel.processor.UnitOfWorkProcessor;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.processor.async.DefaultExchangeHandler;
import com.nxttxn.vramel.processor.async.IteratorDoneStrategy;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.spi.UnitOfWork;
import com.nxttxn.vramel.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.nxttxn.vramel.processor.PipelineHelper.continueProcessing;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/27/13
 * Time: 1:17 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class MulticastSupport extends AggregationSupport {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Collection<Processor> processors;
    private final boolean parallelProcessing;
    private final boolean streaming;
    private final boolean stopOnException;
    private final long timeout;
    protected final Processor onPrepare;
    private final boolean shareUnitOfWork;
    private final ConcurrentMap<PreparedErrorHandler, Processor> errorHandlers = new ConcurrentHashMap<PreparedErrorHandler, Processor>();

    public MulticastSupport(Collection<Processor> processors, AggregationStrategy aggregationStrategy, boolean parallelProcessing, boolean streaming, boolean stopOnException, long timeout, Processor onPrepare, boolean shareUnitOfWork) {
        super(aggregationStrategy);
        this.processors = processors;
        this.parallelProcessing = parallelProcessing;
        this.streaming = streaming;
        this.stopOnException = stopOnException;
        this.timeout = timeout;
        this.onPrepare = onPrepare;
        this.shareUnitOfWork = shareUnitOfWork;
    }

    public MulticastSupport(List<Processor> processors) {
        this(processors, null, false, false, false, 0, null, false);
    }


    public Collection<Processor> getProcessors() {
        return processors;
    }
    /**
     * Is the multicast processor working in streaming mode?
     * <p/>
     * In streaming mode:
     * <ul>
     * <li>we use {@link Iterable} to ensure we can send messages as soon as the data becomes available</li>
     * <li>for parallel processing, we start aggregating responses as they get send back to the processor;
     * this means the {@link org.apache.camel.processor.aggregate.AggregationStrategy} has to take care of handling out-of-order arrival of exchanges</li>
     * </ul>
     */
    public boolean isStreaming() {
        return streaming;
    }


    public boolean isParallelProcessing() {
        return parallelProcessing;
    }

    public boolean isShareUnitOfWork() {
        return shareUnitOfWork;
    }

    public boolean isStopOnException() {
        return stopOnException;
    }

    public long getTimeout() {
        return timeout;
    }


    protected void doStart() throws Exception {
        ServiceHelper.startServices(getAggregationStrategy(), processors);
    }



    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopServices(processors, errorHandlers, getAggregationStrategy());
    }

    @Override
    protected void doShutdown() throws Exception {
        ServiceHelper.stopAndShutdownServices(processors, errorHandlers, getAggregationStrategy());

        // only clear error handlers when shutting down
        errorHandlers.clear();

    }


    protected  Iterable<ProcessorExchangePair> createProcessorExchangePairs(Exchange exchange, Collection<Processor> processorList) throws Exception {
        List<ProcessorExchangePair> result = new ArrayList<>(processorList.size());

        int index = 0;
        for (Processor processor : processorList) {
            // copy exchange, and do not share the unit of work
            Exchange copy = ExchangeHelper.createCorrelatedCopy(exchange, false);

            // if we share unit of work, we need to prepare the child exchange
            if (isShareUnitOfWork()) {
                prepareSharedUnitOfWork(copy, exchange);
            }

            // and add the pair
            FlowContext flowContext = exchange.getUnitOfWork() != null ? exchange.getUnitOfWork().getFlowContext() : null;
            final ProcessorExchangePair processorExchangePair = createProcessorExchangePair(index++, processor, copy, flowContext);
            result.add(processorExchangePair);
        }

        if (exchange.getException() != null) {
            // force any exceptions occurred during creation of exchange paris to be thrown
            // before returning the answer;
            throw exchange.getException();
        }

        return result;
    }

    /**
     * Prepares the exchange for participating in a shared unit of work
     * <p/>
     * This ensures a child exchange can access its parent {@link UnitOfWork} when it participate
     * in a shared unit of work.
     *
     * @param childExchange  the child exchange
     * @param parentExchange the parent exchange
     */
    protected void prepareSharedUnitOfWork(Exchange childExchange, Exchange parentExchange) {
        childExchange.setProperty(Exchange.PARENT_UNIT_OF_WORK, parentExchange.getUnitOfWork());
    }

    /**
     * Creates the {@link ProcessorExchangePair} which holds the processor and exchange to be send out.
     * <p/>
     * You <b>must</b> use this method to create the instances of {@link ProcessorExchangePair} as they
     * need to be specially prepared before use.
     *
     * @param index        the index
     * @param processor    the processor
     * @param exchange     the exchange
     * @param routeContext the route context
     * @return prepared for use
     */
    protected ProcessorExchangePair createProcessorExchangePair(int index, Processor processor, Exchange exchange,
                                                                FlowContext routeContext) {
        Processor prepared = processor;

        // set property which endpoint we send to
        setToEndpoint(exchange, prepared);

        // rework error handling to support fine grained error handling
        prepared = createErrorHandler(routeContext, exchange, prepared);

        // invoke on prepare on the exchange if specified
        if (onPrepare != null) {
            try {
                onPrepare.process(exchange);
            } catch (Exception e) {
                exchange.setException(e);
            }
        }
        return new DefaultProcessorExchangePair(index, processor, prepared, exchange);
    }

    protected static void setToEndpoint(Exchange exchange, Processor processor) {
        if (processor instanceof Producer) {
            Producer producer = (Producer) processor;
            exchange.setProperty(Exchange.TO_ENDPOINT, producer.getEndpoint().getEndpointUri());
        }
    }

    protected Processor createErrorHandler(FlowContext flowContext, Exchange exchange, Processor processor) {
        Processor answer;

        if (flowContext != null) {
            // wrap the producer in error handler so we have fine grained error handling on
            // the output side instead of the input side
            // this is needed to support redelivery on that output alone and not doing redelivery
            // for the entire multicast block again which will start from scratch again

            // create key for cache
            final PreparedErrorHandler key = new PreparedErrorHandler(flowContext, processor);

            // lookup cached first to reuse and preserve memory
            answer = errorHandlers.get(key);
            if (answer != null) {
                logger.trace("Using existing error handler for: {}", processor);
                return answer;
            }

            logger.trace("Creating error handler for: {}", processor);
            ErrorHandlerFactory builder = flowContext.getFlow().getErrorHandlerBuilder();
            // create error handler (create error handler directly to keep it light weight,
            // instead of using ProcessorDefinition.wrapInErrorHandler)
            try {
                processor = builder.createErrorHandler(flowContext, processor);

                // and wrap in unit of work processor so the copy exchange also can run under UoW
                answer = createUnitOfWorkProcessor(flowContext, processor, exchange);

                // must start the error handler
                ServiceHelper.startServices(answer);
            } catch (Exception e) {
                throw ObjectHelper.wrapRuntimeCamelException(e);
            }
            // here we don't cache the ChildUnitOfWorkProcessor
            // As the UnitOfWorkProcess will be delegate to the Parent
            if (!(answer instanceof ChildUnitOfWorkProcessor)) {
                // add to cache
                errorHandlers.putIfAbsent(key, answer);
            }
        } else {
            // and wrap in unit of work processor so the copy exchange also can run under UoW
            answer = createUnitOfWorkProcessor(flowContext, processor, exchange);
        }

        return answer;
    }


    /**
     * Strategy to create the {@link UnitOfWorkProcessor} to be used for the sub route
     *
     * @param flowContext the route context
     * @param processor    the processor wrapped in this unit of work processor
     * @param exchange     the exchange
     * @return the unit of work processor
     */
    protected UnitOfWorkProcessor createUnitOfWorkProcessor(FlowContext flowContext, Processor processor, Exchange exchange) {
        UnitOfWork parent = exchange.getProperty(Exchange.PARENT_UNIT_OF_WORK, UnitOfWork.class);
        if (parent != null) {
            return new ChildUnitOfWorkProcessor(parent, flowContext, processor);
        } else {
            return new UnitOfWorkProcessor(flowContext, processor);
        }
    }
    /**
     * Class that represents prepared fine grained error handlers when processing multicasted/splitted exchanges
     * <p/>
     * See the <tt>createProcessorExchangePair</tt> and <tt>createErrorHandler</tt> methods.
     */
    static final class PreparedErrorHandler extends KeyValueHolder<FlowContext, Processor> {

        public PreparedErrorHandler(FlowContext key, Processor value) {
            super(key, value);
        }

    }
    /**
     * Class that represent each step in the multicast route to do
     */
    static final class DefaultProcessorExchangePair implements ProcessorExchangePair {
        private final int index;
        private final Processor processor;
        private final Processor prepared;
        private final Exchange exchange;

        private DefaultProcessorExchangePair(int index, Processor processor, Processor prepared, Exchange exchange) {
            this.index = index;
            this.processor = processor;
            this.exchange = exchange;
            this.prepared = prepared;
        }

        public int getIndex() {
            return index;
        }

        public Exchange getExchange() {
            return exchange;
        }

        public Producer getProducer() {
            if (processor instanceof Producer) {
                return (Producer) processor;
            }
            return null;
        }

        public Processor getProcessor() {
            return prepared;
        }

        public void begin() {
            // noop
        }

        public void done() {
            // noop
        }

    }


}
