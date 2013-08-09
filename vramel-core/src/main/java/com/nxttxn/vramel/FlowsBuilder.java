package com.nxttxn.vramel;

import com.nxttxn.vramel.impl.DefaultVramelContext;
import com.nxttxn.vramel.model.FlowDefinition;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 12:10 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FlowsBuilder {
    void addFlowsToVramelContext(VramelContext vramelContext) throws Exception;

    FlowDefinition fromF(String uri, Object... args);
}
