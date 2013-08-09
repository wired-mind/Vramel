package com.nxttxn.vramel.spi;

import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.ErrorHandlerFactory;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.builder.ErrorHandlerBuilder;
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
}
