package com.nxttxn.vramel.components.jpos;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.impl.DefaultAsyncProducer;
import com.nxttxn.vramel.impl.DefaultProducer;
import com.nxttxn.vramel.impl.jpos.JPOSClient;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;

import com.nxttxn.vramel.util.AsyncProcessorHelper;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonObject;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/30/13
 * Time: 6:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class JposProducer extends DefaultAsyncProducer {
    private final JposChannelAdapter endpoint;
    private final JPOSClient jposClient;


    public JposProducer(Endpoint endpoint) {
        super(endpoint);
        this.endpoint = (JposChannelAdapter) endpoint;

        final JsonObject config = this.endpoint.getConfig();

        final ClientFactory clientFactory = endpoint.getVramelContext().getClientFactory();

        //allow values in the config file to override what's in the flow
        URI defaultUri = URI.create(this.endpoint.getRemaining());
        final String host = config.getString("host", defaultUri.getHost());
        final Number port = config.getNumber("port", defaultUri.getPort());
        URI uri = URI.create(String.format("jpos://%s:%s", host, port));

        jposClient = clientFactory.createOrFindJPOSClient(uri, config.getString("keyFields", JPOSClient.DEFAULT_KEY));
    }




    @Override
    public boolean process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        jposClient.whenActive(30 * 1000, new AsyncResultHandler<Void>(){
            @Override
            public void handle(AsyncResult<Void> event) {
                if (event.failed()) {
                    exchange.setException(new RuntimeVramelException("JposProducer is not yet initialized."));
                    optionalAsyncResultHandler.done(exchange);
                    return;
                }

                final Message in = exchange.getIn();
                final ISOMsg txnMsg = in.getBody(ISOMsg.class);


                jposClient.sendISOMsg(txnMsg, new AsyncResultHandler<ISOMsg>() {
                    @Override
                    public void handle(AsyncResult<ISOMsg> isoMsgAsyncResult) {
                        if (isoMsgAsyncResult.failed()) {
                            exchange.setException(new RuntimeVramelException("Error sending ISOMsg.", isoMsgAsyncResult.cause()));
                            optionalAsyncResultHandler.done(exchange);
                            return;
                        }

                        try {
                            final Message out = in.copy();
                            endpoint.addISOMsgToMessage(isoMsgAsyncResult.result(), out);
                            exchange.setOut(out);
                        } catch (ISOException e) {
                            exchange.setException(e);
                        }
                        optionalAsyncResultHandler.done(exchange);
                    }
                });
            }
        });

        return false;
    }

    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }


}
