package com.nxttxn.vramel.components.axis2;


import com.google.common.base.Optional;
import com.nxttxn.vramel.Consumer;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.Producer;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.impl.DefaultEndpoint;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class Axis2ChannelAdapter extends DefaultEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(Axis2ChannelAdapter.class);
    public static final String defaultAxis2 = "/com/nxttxn/vramel/components/axis2/axis2.xml";
    private final String path;
    private final JsonObject config;
    private final VramelAxisServer vramelAxisServer;

    public Axis2ChannelAdapter(VramelContext vramelContext, String path, JsonObject config) throws AxisFault {
        super(String.format("axis2:%s", path), vramelContext, config);
        this.path = path;

        this.config = config;


        vramelAxisServer = new VramelAxisServer(this);
    }

    @Override
    public Consumer createConsumer(final Processor processor) throws AxisFault {
        return new Axis2Consumer(this, processor, vramelAxisServer);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new Axis2Producer(this, createConfigurationContext());
    }

    public ConfigurationContext createConfigurationContext() throws AxisFault {
        final String axis2Path = getAxis2Path();
        final URL axis2Url = getClass().getResource(axis2Path);
        logger.info("[Client] Loading axis2 from {}", axis2Url.toString());
        return ConfigurationContextFactory.createConfigurationContextFromURIs(axis2Url, null);
    }

    public ConfigurationContext createServerConfigurationContext() throws Exception {
        final String axis2Path = getAxis2Path();
        logger.info("[Server] Loading axis2 from {}", axis2Path);
        return ConfigurationContextFactory.createBasicConfigurationContext(axis2Path);
    }

    private String getAxis2Path() {
        Optional<String> axis2 = Optional.fromNullable(config.getString("axis2", null));
        return axis2.isPresent() ? axis2.get() : defaultAxis2;
    }


    public JsonObject getConfig() {
        return config;
    }

    public String getPath() {
        return path;
    }
}
