package com.nxttxn.vramel.components.axis2;

import com.nxttxn.axis2.transport.vertx.Axis2CallbackResult;
import com.nxttxn.axis2.transport.vertx.HttpAxis2ServerHandler;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.axis2.util.MessageContextBuilder;
import org.vertx.java.core.Handler;


/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/17/13
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class VramelAxis2MessageReceiver extends AbstractMessageReceiver {
    @Override
    protected void invokeBusinessLogic(final MessageContext messageCtx) throws AxisFault {
        final SOAPFactory soapFactory = getSOAPFactory(messageCtx);

        final MessageContext outMsgContext = MessageContextBuilder.createOutMessageContext(messageCtx);
        outMsgContext.getOperationContext().addMessageContext(outMsgContext);

        invokeBusinessLogic(messageCtx, new Handler<Axis2CallbackResult>() {
            @Override
            public void handle(Axis2CallbackResult result) {
                try {
                    final SOAPEnvelope envelope;

                    if (result.failed()) {
                        envelope = soapFactory.getDefaultFaultEnvelope();
                        envelope.getBody().getFault().getReason().setText("Internal error.");
                    } else {
                        //setup normal envelope
                        envelope = soapFactory.getDefaultEnvelope();
                        envelope.getBody().addChild(result.getResponsePayload());
                    }
                    outMsgContext.setEnvelope(envelope);
                    outMsgContext.setProperty(HttpAxis2ServerHandler.RESPONSE, messageCtx.getProperty(HttpAxis2ServerHandler.RESPONSE));

                    replicateState(messageCtx);
                    AxisEngine.send(outMsgContext);
                } catch (Exception e) {
                    log.error("[VramelAxis2MessageReceiver] Error processing the callback", e);

                    //Not sure if this is the ideal way to handle the fault. But we'll start with it.
                    try {
                        outMsgContext.setEnvelope(soapFactory.getDefaultFaultEnvelope());
                        outMsgContext.setProperty(HttpAxis2ServerHandler.RESPONSE, messageCtx.getProperty(HttpAxis2ServerHandler.RESPONSE));

                        replicateState(messageCtx);
                        AxisEngine.send(outMsgContext);
                    } catch (Exception e1) {
                        log.error("[VramelAxis2MessageReceiver] Error processing fault", e1);
                    }

                }

            }
        });


    }


    public void invokeBusinessLogic(MessageContext inMessage, Handler<Axis2CallbackResult> donehandler) throws AxisFault {
        AxisService service = inMessage.getAxisService();
        Axis2Consumer axis2Consumer = (Axis2Consumer) service.getParameter(VramelAxisServer.vramelConsumer).getValue();
        axis2Consumer.process(inMessage, donehandler);
    }
}
