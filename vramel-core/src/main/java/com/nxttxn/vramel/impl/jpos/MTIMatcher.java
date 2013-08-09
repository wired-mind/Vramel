package com.nxttxn.vramel.impl.jpos;

import com.google.common.collect.Maps;
import org.jpos.iso.ISOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

import java.util.Map;

/**
* Created with IntelliJ IDEA.
* User: chuck
* Date: 8/2/13
* Time: 1:43 PM
* To change this template use File | Settings | File Templates.
*/
public class MTIMatcher implements Handler<JPOSServerRequest> {
    protected final Logger logger = LoggerFactory.getLogger(MTIMatcher.class);

    private Map<String, Handler<JPOSServerRequest>> bindings = Maps.newHashMap();

    public void add(String mti, Handler<JPOSServerRequest> handler) {
        bindings.put(mti, handler);
    }
    @Override
    public void handle(JPOSServerRequest jposServerRequest) {
        try {
            final Handler<JPOSServerRequest> isoMsgHandler = bindings.get(jposServerRequest.getMTI());
            if (isoMsgHandler == null) {
                logger.warn("Received ISOMsg with MTI {}. But not handlers are bound to this MTI.", jposServerRequest.getMTI());
                return;
            }
            isoMsgHandler.handle(jposServerRequest);
        } catch (ISOException e) {
            logger.error("Error matching MTI", e);
        }
    }
}
