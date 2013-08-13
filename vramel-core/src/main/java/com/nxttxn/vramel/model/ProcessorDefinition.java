package com.nxttxn.vramel.model;

import com.google.common.collect.Lists;
import com.nxttxn.vramel.*;
import com.nxttxn.vramel.builder.DataFormatClause;
import com.nxttxn.vramel.builder.ExpressionClause;
import com.nxttxn.vramel.builder.ProcessorBuilder;
import com.nxttxn.vramel.model.language.ExpressionDefinition;
import com.nxttxn.vramel.processor.PipelineProcessor;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.processor.interceptor.DefaultChannel;
import com.nxttxn.vramel.spi.FlowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import javax.xml.bind.annotation.XmlAttribute;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ProcessorDefinition<T extends ProcessorDefinition<T>> extends OptionalIdentifiedDefinition<T> implements Block{
    protected final transient Logger log = LoggerFactory.getLogger(getClass());


    private final LinkedList<Block> blocks = new LinkedList<Block>();
    private Boolean inheritErrorHandler;

    public abstract List<ProcessorDefinition<?>> getOutputs();

    private ProcessorDefinition<?> parent;

    public ProcessorDefinition<?> getParent() {
        return parent;
    }

    public void setParent(ProcessorDefinition<?> parent) {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    public T process(Processor processor) {
        ProcessDefinition processDefinition = new ProcessDefinition(processor);
        addOutput(processDefinition);
        return (T)this;
    }

    public FilterDefinition filter(com.nxttxn.vramel.Predicate predicate) {
        FilterDefinition filterDefinition = new FilterDefinition(predicate);
        addOutput(filterDefinition);
        return filterDefinition;
    }
    /**
     * <a href="http://camel.apache.org/message-filter.html">Message Filter EIP:</a>
     * Creates a predicate expression which only if it is <tt>true</tt> then the
     * exchange is forwarded to the destination
     *
     * @return the clause used to create the filter expression
     */
    public ExpressionClause<? extends FilterDefinition> filter() {
        FilterDefinition filter = new FilterDefinition();
        addOutput(filter);
        return ExpressionClause.createAndSetExpression(filter);
    }


    /**
     * <a href="http://camel.apache.org/message-filter.html">Message Filter EIP:</a>
     * Creates a predicate expression which only if it is <tt>true</tt> then the
     * exchange is forwarded to the destination
     *
     * @param expression  the predicate expression to use
     * @return the builder
     */
    public FilterDefinition filter(ExpressionDefinition expression) {
        FilterDefinition filter = new FilterDefinition();
        filter.setExpression(expression);
        addOutput(filter);
        return filter;
    }

    public ChoiceDefinition choice() {
        ChoiceDefinition choiceDefinition = new ChoiceDefinition();
        addOutput(choiceDefinition);
        return choiceDefinition;
    }

    public void addOutput(ProcessorDefinition<?> output) {
        if (!blocks.isEmpty()) {
            // let the Block deal with the output
            Block block = blocks.getLast();
            block.addOutput(output);
            return;
        }
        output.setParent(this);
        configureChild(this);
        getOutputs().add(output);
    }

    protected void configureChild(ProcessorDefinition<?> output) {
        //noop
    }



    public Processor createChildProcessor(FlowContext flowContext) throws Exception {
        return createOutputsProcessor(flowContext);
    }

    public Processor createOutputsProcessor(FlowContext flowContext) throws Exception {
        Collection<ProcessorDefinition<?>> outputs = getOutputs();
        return createOutputsProcessor(flowContext, outputs);
    }

    protected Processor createOutputsProcessor(FlowContext flowContext, Collection<ProcessorDefinition<?>> outputs) throws Exception {
        List<Processor> list = Lists.newArrayList();
        for (ProcessorDefinition<?> processor : outputs) {
            list.add(processor.createProcessor(flowContext));
        }

        Processor processor = null;
        if (!list.isEmpty()) {
            if (list.size() == 1) {
                processor = list.get(0);
            } else {
                processor = createCompositeProcessor(flowContext, list);
            }
        }

        return processor;
    }

    protected Processor createCompositeProcessor(FlowContext flowContext, List<Processor> list) throws Exception {
        return new PipelineProcessor(list);
    }

    public DataFormatClause<ProcessorDefinition<T>> marshal() {
        return new DataFormatClause<>(this, DataFormatClause.Operation.Marshal);
    }

    @SuppressWarnings("unchecked")
    public T marshal(DataFormatDefinition dataFormatType) {
        MarshalDefinition marshalDefinition = new MarshalDefinition(dataFormatType);
        addOutput(marshalDefinition);
        return (T)this;
    }

    public DataFormatClause<ProcessorDefinition<T>> unmarshal() {
        return new DataFormatClause<>(this, DataFormatClause.Operation.Unmarshal);
    }

    @SuppressWarnings("unchecked")
    public T unmarshal(DataFormatDefinition dataFormatType) {
        UnmarshalDefinition unmarshalDefinition = new UnmarshalDefinition(dataFormatType);
        addOutput(unmarshalDefinition);
        return (T)this;
    }
    public T toF(String uri, Object... args) {
        return toF(uri, null, args);
    }

    @SuppressWarnings("unchecked")
    public T toF(String uri, JsonObject config, Object... args) {

        ToDefinition toDefinition = new ToDefinition(String.format(uri, args), config);
        addOutput(toDefinition);
        return (T)this;
    }
    public MulticastDefinition multicast(AggregationStrategy aggregationStrategy) {
        MulticastDefinition multicastDefinition = new MulticastDefinition();
        multicastDefinition.setAggregationStrategy(aggregationStrategy);
        addOutput(multicastDefinition);
        return multicastDefinition;
    }

    public MulticastDefinition multicast(AggregationStrategy aggregationStrategy, boolean parallelProcessing) {
        MulticastDefinition multicastDefinition = new MulticastDefinition();
        multicastDefinition.setAggregationStrategy(aggregationStrategy);
        multicastDefinition.setParallelProcessing(parallelProcessing);
        addOutput(multicastDefinition);
        return multicastDefinition;
    }

    public MulticastDefinition multicast() {
        MulticastDefinition multicastDefinition = new MulticastDefinition();
        addOutput(multicastDefinition);
        return multicastDefinition;
    }

    public PipelineDefinition pipeline() {
        PipelineDefinition pipelineDefinition = new PipelineDefinition();
        addOutput(pipelineDefinition);
        return pipelineDefinition;
    }

    public ProcessorDefinition<?> end() {
        if (parent == null) {
            return this.endParent();
        }
        return parent.endParent();
    }

    protected ProcessorDefinition<?> endParent() {
        return this;
    }

    @SuppressWarnings("unchecked")
    public T log(String message) {
        LogDefinition logDefinition = new LogDefinition(message);
        addOutput(logDefinition);
        return (T) this;
    }
    public T enrich(String resourceUri, AggregationStrategy aggregationStrategy) {
        addOutput(new EnrichDefinition(aggregationStrategy, resourceUri));
        return (T) this;
    }

    public DynamicRouterDefinition<T> dynamicRouter(Expression expression) {
        DynamicRouterDefinition<T> dynamicRouterDefinition = new DynamicRouterDefinition<T>(expression);
        addOutput(dynamicRouterDefinition);
        return dynamicRouterDefinition;
    }

    public RoutingSlipDefinition<T> routingSlip(Expression expression, String uriDelimiter) {
        RoutingSlipDefinition<T> routingSlipDefinition = new RoutingSlipDefinition<T>(expression, uriDelimiter);
        addOutput(routingSlipDefinition);
        return routingSlipDefinition;
    }

    public RoutingSlipDefinition<T> routingSlip(Expression expression) {
        RoutingSlipDefinition<T> routingSlipDefinition = new RoutingSlipDefinition<T>(expression);
        addOutput(routingSlipDefinition);
        return routingSlipDefinition;
    }

    @SuppressWarnings("unchecked")
    public T setHeader(String name, Expression expression) {
        SetHeaderDefinition answer = new SetHeaderDefinition(name, expression);
        addOutput(answer);
        return (T) this;
    }
    public ExpressionClause<ProcessorDefinition<T>> setBody() {
        ExpressionClause<ProcessorDefinition<T>> clause = new ExpressionClause<ProcessorDefinition<T>>(this);
        SetBodyDefinition answer = new SetBodyDefinition(clause);
        addOutput(answer);
        return clause;
    }


    @SuppressWarnings("unchecked")
    public T setBody(Expression expression) {
        SetBodyDefinition answer = new SetBodyDefinition(expression);
        addOutput(answer);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T transform(Expression expression) {
        TransformDefinition answer = new TransformDefinition(expression);
        addOutput(answer);
        return (T) this;
    }


    public ExpressionClause<ProcessorDefinition<T>> transform() {
        ExpressionClause<ProcessorDefinition<T>> clause =
                new ExpressionClause<ProcessorDefinition<T>>((ProcessorDefinition<T>) this);
        TransformDefinition answer = new TransformDefinition(clause);
        addOutput(answer);
        return clause;
    }

    public T setFaultBody(Expression expression) {
        return process(ProcessorBuilder.setFaultBody(expression));
    }


    /**
     * Ends the current block and returns back to the {@link ChoiceDefinition choice()} DSL.
     *
     * @return the builder
     */
    public ChoiceDefinition endChoice() {
        // are we already a choice?
        ProcessorDefinition<?> def = this;
        if (def instanceof ChoiceDefinition) {
            return (ChoiceDefinition) def;
        }

        // okay end this and get back to the choice
        def = end();
        if (def instanceof WhenDefinition) {
            return (ChoiceDefinition) def.getParent();
        } else if (def instanceof OtherwiseDefinition) {
            return (ChoiceDefinition) def.getParent();
        } else {
            return (ChoiceDefinition) def;
        }
    }

    /**
     * Sets the exception on the {@link org.apache.camel.Exchange}
     *
     * @param exception the exception to throw
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public T throwException(Exception exception) {
        ThrowExceptionDefinition answer = new ThrowExceptionDefinition();
        answer.setException(exception);
        addOutput(answer);
        return (T) this;
    }


    /**
     * Pushes the given block on the stack as current block
     *
     * @param block  the block
     */
    void pushBlock(Block block) {
        blocks.add(block);
    }
    /**
     * Pops the block off the stack as current block
     *
     * @return the block
     */
    Block popBlock() {
        return blocks.isEmpty() ? null : blocks.removeLast();
    }

    /**
     * Returns a label to describe this node such as the expression if some kind of expression node
     */
    public String getLabel() {
        return "";
    }

    /**
     * Adds a processor which sets the exchange property
     *
     * @param name  the property name
     * @param expression  the expression used to set the property
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public T setProperty(String name, Expression expression) {
        SetPropertyDefinition answer = new SetPropertyDefinition(name, expression);
        addOutput(answer);
        return (T) this;
    }


    public Processor createProcessor(FlowContext flowContext) throws Exception{
        throw new UnsupportedOperationException("Not implemented yet for class: " + getClass().getName());
    }

    /**
     * Adds a processor which sets the exchange property
     *
     * @param name  the property name
     * @return a expression builder clause to set the property
     */
    public ExpressionClause<ProcessorDefinition<T>> setProperty(String name) {
        ExpressionClause<ProcessorDefinition<T>> clause = new ExpressionClause<ProcessorDefinition<T>>(this);
        SetPropertyDefinition answer = new SetPropertyDefinition(name, clause);
        addOutput(answer);
        return clause;
    }

    public void addFlows(FlowContext flowContext, Collection<Flow> flows) throws Exception {
        Processor processor = makeProcessor(flowContext);
        if (processor == null) {
            // no processor to add
            return;
        }

        if (!flowContext.isFlowAdded()) {
            boolean endpointInterceptor = false;

            // are we routing to an endpoint interceptor, if so we should not add it as an event driven
            // processor as we use the producer to trigger the interceptor
//            if (processor instanceof Channel) {
//                Channel channel = (Channel) processor;
//                org.apache.camel.Processor next = channel.getNextProcessor();
//                if (next instanceof InterceptEndpointProcessor) {
//                    endpointInterceptor = true;
//                }
//            }

            // only add regular processors as event driven
            if (endpointInterceptor) {
                log.debug("Endpoint interceptor should not be added as an event driven consumer route: {}", processor);
            } else {
                log.trace("Adding event driven processor: {}", processor);
                flowContext.addEventDrivenProcessor(processor);
            }

        }
    }

    /**
     * Creates the processor and wraps it in any necessary interceptors and error handlers
     */
    protected Processor makeProcessor(FlowContext flowContext) throws Exception {
        Processor processor = null;

        // allow any custom logic before we create the processor
        preCreateProcessor();

        // resolve properties before we create the processor
        //skip: resolvePropertyPlaceholders(flowContext, this);

        // resolve constant fields (eg Exchange.FILE_NAME)
        //skip: resolveKnownConstantFields(this);

        // also resolve properties and constant fields on embedded expressions
        ProcessorDefinition<?> me = (ProcessorDefinition<?>) this;
        if (me instanceof ExpressionNode) {
            ExpressionNode exp = (ExpressionNode) me;
            ExpressionDefinition expressionDefinition = exp.getExpression();
            if (expressionDefinition != null) {
                // resolve properties before we create the processor
                //skip: resolvePropertyPlaceholders(flowContext, expressionDefinition);

                // resolve constant fields (eg Exchange.FILE_NAME)
                //skip: resolveKnownConstantFields(expressionDefinition);
            }
        }

        // at first use custom factory
//        if (flowContext.getVramelContext().getProcessorFactory() != null) {
//            processor = flowContext.getVramelContext().getProcessorFactory().createProcessor(flowContext, this);
//        }
        // fallback to default implementation if factory did not create the processor
        if (processor == null) {
            processor = createProcessor(flowContext);
        }

        if (processor == null) {
            // no processor to make
            return null;
        }
        return wrapProcessor(flowContext, processor);
    }

    /**
     * Strategy to execute any custom logic before the {@link org.apache.camel.Processor} is created.
     */
    protected void preCreateProcessor() {
        // noop
    }

    /**
     * Wraps the child processor in whatever necessary interceptors and error handlers
     */
    public Processor wrapProcessor(FlowContext flowContext, Processor processor) throws Exception {
        // dont double wrap
        if (processor instanceof Channel) {
            return processor;
        }
        return wrapChannel(flowContext, processor, null);
    }

    protected Processor wrapChannel(FlowContext flowContext, Processor processor, ProcessorDefinition<?> child) throws Exception {
        // put a channel in between this and each output to control the route flow logic
        ModelChannel channel = createChannel(flowContext);
        channel.setNextProcessor(processor);


        // must do this ugly cast to avoid compiler error on AIX/HP-UX
        ProcessorDefinition<?> defn = (ProcessorDefinition<?>) this;

        // set the child before init the channel
        channel.setChildDefinition(child);
        channel.initChannel(defn, flowContext);

        // set the error handler, must be done after init as we can set the error handler as first in the chain
        if (defn instanceof TryDefinition || defn instanceof CatchDefinition || defn instanceof FinallyDefinition) {
            // do not use error handler for try .. catch .. finally blocks as it will handle errors itself
            log.trace("{} is part of doTry .. doCatch .. doFinally so no error handler is applied", defn);
        } else if (ProcessorDefinitionHelper.isParentOfType(TryDefinition.class, defn, true)
                || ProcessorDefinitionHelper.isParentOfType(CatchDefinition.class, defn, true)
                || ProcessorDefinitionHelper.isParentOfType(FinallyDefinition.class, defn, true)) {
            // do not use error handler for try .. catch .. finally blocks as it will handle errors itself
            // by checking that any of our parent(s) is not a try .. catch or finally type
            log.trace("{} is part of doTry .. doCatch .. doFinally so no error handler is applied", defn);
        } else if (defn instanceof OnExceptionDefinition || ProcessorDefinitionHelper.isParentOfType(OnExceptionDefinition.class, defn, true)) {
            log.trace("{} is part of OnException so no error handler is applied", defn);
            // do not use error handler for onExceptions blocks as it will handle errors itself
        } else if (defn instanceof MulticastDefinition) {
            // do not use error handler for multicast as it offers fine grained error handlers for its outputs
            // however if share unit of work is enabled, we need to wrap an error handler on the multicast parent
            MulticastDefinition def = (MulticastDefinition) defn;
            if (def.isShareUnitOfWork() && child == null) {
                // only wrap the parent (not the children of the multicast)
                wrapChannelInErrorHandler(channel, flowContext);
            } else {
                log.trace("{} is part of multicast which have special error handling so no error handler is applied", defn);
            }
        } else {
            // use error handler by default or if configured to do so
            wrapChannelInErrorHandler(channel, flowContext);
        }

        // do post init at the end
        channel.postInitChannel(defn, flowContext);
        log.trace("{} wrapped in Channel: {}", defn, channel);

        return channel;
    }

    /**
     * Wraps the given channel in error handler (if error handler is inherited)
     *
     * @param channel       the channel
     * @param flowContext  the route context
     * @throws Exception can be thrown if failed to create error handler builder
     */
    private void wrapChannelInErrorHandler(Channel channel, FlowContext flowContext) throws Exception {
        if (isInheritErrorHandler() == null || isInheritErrorHandler()) {
            log.trace("{} is configured to inheritErrorHandler", this);
            Processor output = channel.getOutput();
            Processor errorHandler = wrapInErrorHandler(flowContext, output);
            // set error handler on channel
            channel.setErrorHandler(errorHandler);
        } else {
            log.debug("{} is configured to not inheritErrorHandler.", this);
        }
    }

    /**
     * Wraps the given output in an error handler
     *
     * @param flowContext the route context
     * @param output the output
     * @return the output wrapped with the error handler
     * @throws Exception can be thrown if failed to create error handler builder
     */
    protected Processor wrapInErrorHandler(FlowContext flowContext, Processor output) throws Exception {
        ErrorHandlerFactory builder = flowContext.getFlow().getErrorHandlerBuilder();
        // create error handler
        Processor errorHandler = builder.createErrorHandler(flowContext, output);

//        // invoke lifecycles so we can manage this error handler builder
//        for (LifecycleStrategy strategy : flowContext.getVramelContext().getLifecycleStrategies()) {
//            strategy.onErrorHandlerAdd(flowContext, errorHandler, builder);
//        }

        return errorHandler;
    }

    /**
     * Creates a new instance of the {@link Channel}.
     */
    protected ModelChannel createChannel(FlowContext flowContext) throws Exception {
        return new DefaultChannel();
    }

    /**
     * <a href="http://camel.apache.org/exception-clause.html">Exception clause</a>
     * for catching certain exceptions and handling them.
     *
     * @param exceptionType  the exception to catch
     * @return the exception builder to configure
     */
    public OnExceptionDefinition onException(Class<? extends Throwable> exceptionType) {
        OnExceptionDefinition answer = new OnExceptionDefinition(exceptionType);
        answer.setFlowScoped(true);
        addOutput(answer);
        return answer;
    }

    /**
     * <a href="http://camel.apache.org/exception-clause.html">Exception clause</a>
     * for catching certain exceptions and handling them.
     *
     * @param exceptions list of exceptions to catch
     * @return the exception builder to configure
     */
    public OnExceptionDefinition onException(Class<? extends Throwable>... exceptions) {
        OnExceptionDefinition answer = new OnExceptionDefinition(Arrays.asList(exceptions));
        answer.setFlowScoped(true);
        addOutput(answer);
        return answer;
    }


    public Boolean isInheritErrorHandler() {
        return inheritErrorHandler;
    }

    @XmlAttribute
    public void setInheritErrorHandler(Boolean inheritErrorHandler) {
        this.inheritErrorHandler = inheritErrorHandler;
    }

    public boolean isAbstract() {
        return false;  //we're ignoring this for now.. might need to implement later
    }

    public void clearOutput() {
        getOutputs().clear();
        blocks.clear();
    }
}
