package com.nxttxn.vramel.components.jpos;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultEndpoint;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/30/13
 * Time: 5:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class JposChannelAdapter extends DefaultEndpoint {
    public static final String ISO_MTI_HEADER = "ISO_MTI";
    public static final String ISO_STATUS_HEADER = "ISO_STATUS";
    private static final Logger logger = LoggerFactory.getLogger(JposChannelAdapter.class);

    private final JsonObject config;
    private final String remaining;

    public JposChannelAdapter(VramelContext vramelContext, JsonObject config, String remaining) {
        super(String.format("jpos"), vramelContext, config);

        this.config = config;
        this.remaining = remaining;
    }

    @Override
    public Consumer createConsumer(final Processor processor) {
        return new JposConsumer(this, processor);
    }

    @Override
    public Producer createProducer() {
        return new JposProducer(this);
    }

    public JsonObject getConfig() {
        return config;
    }

    public void addISOMsgToMessage(ISOMsg isoMsg, Message message) throws ISOException {
        message.setBody(isoMsg);
        message.setHeader(ISO_MTI_HEADER, isoMsg.getMTI());
    }

    public String getRemaining() {
        return remaining;
    }
}
