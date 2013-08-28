package com.nxttxn.vramel.spi;

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.model.FlowDefinition;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/3/13
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FlowContext {


    Endpoint getEndpoint();


    FlowDefinition getFlow();

    VramelContext getVramelContext();


    Endpoint resolveEndpoint(String uri);


    void addEventDrivenProcessor(Processor processor);

    boolean isFlowAdded();

    void setIsFlowAdded(boolean flowAdded);

    void commit();

    <T> T mandatoryLookup(String name, Class<T> type);

    /**
     * Sets whether the object should automatically start when Camel starts.
     * <p/>
     * <b>Important:</b> Currently only routes can be disabled, as {@link VramelContext}s are always started.
     * <br/>
     * <b>Note:</b> When setting auto startup <tt>false</tt> on {@link VramelContext} then that takes precedence
     * and <i>no</i> routes is started. You would need to start {@link VramelContext} explicit using
     * the {@link org.apache.camel.CamelContext#start()} method, to start the context and the routes.
     * <p/>
     * Default is <tt>true</tt> to always start up.
     *
     * @param autoStartup whether to start up automatically.
     */
    void setAutoStartup(Boolean autoStartup);
}
