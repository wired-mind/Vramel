package com.nxttxn.vramel.components.axis2;


import com.google.common.base.Optional;
import com.nxttxn.vramel.*;
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
    private final Optional<ConfigurationContext> configurationContext;

    public Axis2ChannelAdapter(VramelContext vramelContext, String path, JsonObject config) throws AxisFault {
        super(String.format("axis2:%s", path), vramelContext);
        this.path = path;
        this.config = config;


        vramelAxisServer = new VramelAxisServer(this);
        final URL axis2Url = getAxis2Url();
        if (axis2Url != null) {
            configurationContext = Optional.of(createConfigurationContext(axis2Url));
        } else {
            configurationContext = Optional.absent();
        }
    }

    @Override
    public Consumer createConsumer(final Processor processor) throws AxisFault {
        return new Axis2Consumer(this, processor, vramelAxisServer);
    }

    @Override
    public Producer createProducer() throws Exception {
        if (!configurationContext.isPresent()) {
            throw new RuntimeVramelException("Cannot create axis2 producer. No axis2 client file specified");
        }
        return new Axis2Producer(this, configurationContext.get());
    }

    public ConfigurationContext createConfigurationContext(URL axis2Url) throws AxisFault {
        logger.info("[Client] Loading axis2 from {}", axis2Url.toString());
        return ConfigurationContextFactory.createConfigurationContextFromURIs(axis2Url, null);
    }

    private URL getAxis2Url() {
        final String axis2Path = getAxis2Path();
        return getClass().getResource(axis2Path);
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



    public String getPath() {
        return path;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public JsonObject getConfig() {
        return config;
    }
}
