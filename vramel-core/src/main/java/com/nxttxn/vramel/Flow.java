package com.nxttxn.vramel;

import com.nxttxn.vramel.spi.FlowContext;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/3/13
 * Time: 3:39 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Flow {

    String ID_PROPERTY = "id";

    Endpoint getEndpoint();

    Consumer getConsumer();

    FlowContext getFlowContext();

    void onStartingServices(List<Service> services) throws Exception;

    List<Service> getServices();


    Map<String, Object> getProperties();
}
