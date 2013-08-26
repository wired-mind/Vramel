package com.nxttxn.vramel.components.axis2;

import com.nxttxn.axis2.transport.vertx.Axis2CallbackResult;
import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultConsumer;
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class Axis2Consumer extends DefaultConsumer {
    private static final Logger logger = LoggerFactory.getLogger(Axis2Consumer.class);
    private final Axis2ChannelAdapter endpoint;
    private final VramelAxisServer axisServer;


    public Axis2Consumer(Endpoint endpoint, final Processor processor, VramelAxisServer vramelAxisServer) throws AxisFault {
        super(endpoint, processor);
        this.endpoint = (Axis2ChannelAdapter) endpoint;

        axisServer = vramelAxisServer;

        axisServer.deployService(this.endpoint, this);



    }


    public void process(final MessageContext inMessage, final Handler<Axis2CallbackResult> doneHandler) {
        final OMElement operationWrapper = inMessage.getEnvelope().getBody().getFirstElement();
        final OMElement arg = operationWrapper.getFirstElement();
        final byte[] body = arg.toString().getBytes();

        Exchange exchange = getEndpoint().createExchange();
        final Message in = exchange.getIn();
        in.setBody(body);
        // TODO: set headers
        try {
            logger.debug("[Axis2 Consumer] Ready to process exchange: {}.", getAsyncProcessor().toString(), exchange);
            getAsyncProcessor().process(exchange, new OptionalAsyncResultHandler() {
                @Override
                public void handle(AsyncExchangeResult asyncExchangeResult) {
                    try {
                        if (asyncExchangeResult.failed()) {
                            handleError(doneHandler, asyncExchangeResult.getException());
                            return;
                        }

                        final Exchange result = asyncExchangeResult.result.get();
                        final Message out = result.getOut();
                        final byte[] body = out.getBody(byte[].class);
                        OMElement responsePayload;
                        if (body != null) {
                            responsePayload = AXIOMUtil.stringToOM(new String(body));
                        } else {
                            //fallback to string and fail if not a string
                            final String strBody = out.getBody(String.class);
                            checkNotNull(strBody);
                            responsePayload = AXIOMUtil.stringToOM(strBody);
                        }

                        doneHandler.handle(new Axis2CallbackResult(responsePayload));
                    } catch (Exception e) {
                        logger.error("[Axis2 Consumer] Error processing the exchange response.", e);
                        doneHandler.handle(new Axis2CallbackResult(e));
                    }
                }
            });
        } catch (Exception e) {
            handleError(doneHandler, e);
        }
    }

    private void handleError(Handler<Axis2CallbackResult> doneHandler, Exception e) {
        logger.error("[Axis2 Consumer] Error processing the exchange", e);
        doneHandler.handle(new Axis2CallbackResult(e));
    }

}
