package com.nxttxn.vramel.model;

import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.MulticastProcessor;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.processor.aggregate.UseLatestAggregationStrategy;
import com.nxttxn.vramel.spi.FlowContext;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/19/13
 * Time: 9:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class MulticastDefinition extends OutputDefinition<MulticastDefinition> {
    @XmlAttribute
    private Boolean parallelProcessing;
    @XmlAttribute
    private Boolean streaming;
    @XmlAttribute
    private Boolean stopOnException;
    @XmlAttribute
    private Long timeout;
    @XmlTransient
    private AggregationStrategy aggregationStrategy;
    @XmlAttribute
    private String onPrepareRef;
    @XmlTransient
    private Processor onPrepare;
    @XmlAttribute
    private Boolean shareUnitOfWork;

    public Boolean getParallelProcessing() {
        return parallelProcessing;
    }

    public void setParallelProcessing(Boolean parallelProcessing) {
        this.parallelProcessing = parallelProcessing;
    }

    public boolean isParallelProcessing() {
        return parallelProcessing != null && parallelProcessing;
    }

    public Boolean getStreaming() {
        return streaming;
    }

    public void setStreaming(Boolean streaming) {
        this.streaming = streaming;
    }

    public boolean isStreaming() {
        return streaming != null && streaming;
    }

    public Boolean getStopOnException() {
        return stopOnException;
    }

    public void setStopOnException(Boolean stopOnException) {
        this.stopOnException = stopOnException;
    }

    public Boolean isStopOnException() {
        return stopOnException != null && stopOnException;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public String getOnPrepareRef() {
        return onPrepareRef;
    }

    public void setOnPrepareRef(String onPrepareRef) {
        this.onPrepareRef = onPrepareRef;
    }

    public Processor getOnPrepare() {
        return onPrepare;
    }

    public void setOnPrepare(Processor onPrepare) {
        this.onPrepare = onPrepare;
    }

    public Boolean getShareUnitOfWork() {
        return shareUnitOfWork;
    }

    public void setShareUnitOfWork(Boolean shareUnitOfWork) {
        this.shareUnitOfWork = shareUnitOfWork;
    }

    public boolean isShareUnitOfWork() {
        return shareUnitOfWork != null && shareUnitOfWork;
    }

    public MulticastDefinition parallelProcessing() {
        setParallelProcessing(true);
        return this;
    }


    /**
     * Aggregates the responses as the are done (e.g. out of order sequence)
     *
     * @return the builder
     */
    public MulticastDefinition streaming() {
        setStreaming(true);
        return this;
    }

    /**
     * Will now stop further processing if an exception or failure occurred during processing of an
     * {@link org.apache.camel.Exchange} and the caused exception will be thrown.
     * <p/>
     * Will also stop if processing the exchange failed (has a fault message) or an exception
     * was thrown and handled by the error handler (such as using onException). In all situations
     * the multicast will stop further processing. This is the same behavior as in pipeline, which
     * is used by the routing engine.
     * <p/>
     * The default behavior is to <b>not</b> stop but continue processing till the end
     *
     * @return the builder
     */
    public MulticastDefinition stopOnException() {
        setStopOnException(true);
        return this;
    }

    /**
     * Uses the {@link Processor} when preparing the {@link org.apache.camel.Exchange} to be send.
     * This can be used to deep-clone messages that should be send, or any custom logic needed before
     * the exchange is send.
     *
     * @param onPrepare the processor
     * @return the builder
     */
    public MulticastDefinition onPrepare(Processor onPrepare) {
        setOnPrepare(onPrepare);
        return this;
    }

    /**
     * Uses the {@link Processor} when preparing the {@link org.apache.camel.Exchange} to be send.
     * This can be used to deep-clone messages that should be send, or any custom logic needed before
     * the exchange is send.
     *
     * @param onPrepareRef reference to the processor to lookup in the {@link org.apache.camel.spi.Registry}
     * @return the builder
     */
    public MulticastDefinition onPrepareRef(String onPrepareRef) {
        setOnPrepareRef(onPrepareRef);
        return this;
    }

    /**
     * Sets a timeout value in millis to use when using parallelProcessing.
     *
     * @param timeout timeout in millis
     * @return the builder
     */
    public MulticastDefinition timeout(long timeout) {
        setTimeout(timeout);
        return this;
    }

    /**
     * Shares the {@link org.apache.camel.spi.UnitOfWork} with the parent and each of the sub messages.
     *
     * @return the builder.
     * @see org.apache.camel.spi.SubUnitOfWork
     */
    public MulticastDefinition shareUnitOfWork() {
        setShareUnitOfWork(true);
        return this;
    }

    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        return createChildProcessor(flowContext);
    }

    @Override
    protected Processor createCompositeProcessor(FlowContext flowContext, List<Processor> list) throws Exception {
        if (getAggregationStrategy() == null) {
            // default to use latest aggregation strategy
            setAggregationStrategy(new UseLatestAggregationStrategy());
        }
        long timeout = getTimeout() != null ? getTimeout() : 0;
        return new MulticastProcessor(list, getAggregationStrategy(), isParallelProcessing(), isStreaming(), isStopOnException(), timeout, onPrepare, isShareUnitOfWork());
    }

    public AggregationStrategy getAggregationStrategy() {
        return aggregationStrategy;
    }

    public void setAggregationStrategy(AggregationStrategy aggregationStrategy) {
        this.aggregationStrategy = aggregationStrategy;
    }

}
