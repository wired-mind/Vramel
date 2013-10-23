package com.nxttxn.vramel.components.axis2;

import com.google.common.base.Optional;
import com.nxttxn.axis2.transport.vertx.VertxListener;
import com.nxttxn.axis2.transport.vertx.VertxUtils;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/17/13
 * Time: 11:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class VramelAxisServer extends AxisServer{

    public static final String vramelConsumer = "VramelConsumer";
    private static final Logger logger = LoggerFactory.getLogger(VramelAxisServer.class);
    private final Axis2ChannelAdapter endpoint;

    public VramelAxisServer(Axis2ChannelAdapter endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    protected ConfigurationContext createDefaultConfigurationContext() throws AxisFault {
        try {
            return this.endpoint.createServerConfigurationContext();
        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }
    }

    public void deployService(Axis2ChannelAdapter endpoint, Axis2Consumer axis2Consumer) throws AxisFault {
        if (configContext == null) {
            configContext = getConfigurationContext();
        }
        AxisConfiguration axisConfig = configContext.getAxisConfiguration();

        final Optional<String> serviceClass = Optional.of(endpoint.getConfig().getString("serviceClass"));
        //pass null message receiver map to force the use of what we configure in our own axis2.xml
        AxisService service = AxisService.createService(serviceClass.get(), axisConfig, null, null, null, axisConfig.getSystemClassLoader());

        service.addParameter(vramelConsumer, axis2Consumer);
        //Path is used to setup vertx "route" only for a server
        service.addParameter(VertxListener.PATH, endpoint.getPath());
        service.addParameter(VertxUtils.VERTX, this.endpoint.getVramelContext().getVertx());

        axisConfig.addService(service);

        start();
    }
}
