package com.nxttxn.vramel.components.jpos;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultConsumer;
import com.nxttxn.vramel.impl.jpos.JPOSServer;
import com.nxttxn.vramel.impl.jpos.JPOSServerRequest;
import com.nxttxn.vramel.impl.jpos.MTIMatcher;
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import org.jpos.iso.ISOMsg;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.net.URI;

import static com.nxttxn.vramel.util.CastUtils.as;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/30/13
 * Time: 5:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class JposConsumer extends DefaultConsumer {

    private final JposChannelAdapter endpoint;
    private final JsonObject jposConfig;
    private final Vertx vertx;
    private final JPOSServer jposServer;
    public JposConsumer(final Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = (JposChannelAdapter) endpoint;

        vertx = endpoint.getVramelContext().getVertx();
        jposConfig = this.endpoint.getConfig();

        final String host = jposConfig.getString("host", "0.0.0.0");
        final Number port = jposConfig.getNumber("port", 7676);

        final URI uri = URI.create(String.format("jpos://%s:%s", host, port));

        final ServerFactory serverFactory = endpoint.getVramelContext().getServerFactory();
        jposServer = serverFactory.createOrFindJPOSServer(uri);

        MTIMatcher mtiMatcher = findOrCreateMTIMatcher();

        final String mti = this.endpoint.getRemaining();
        mtiMatcher.add(mti, mtiHandler);
    }

    private MTIMatcher findOrCreateMTIMatcher() {
        MTIMatcher mtiMatcher = as(MTIMatcher.class, jposServer.jposServerRequestHandler());
        if (mtiMatcher == null) {
            mtiMatcher = new MTIMatcher();
            jposServer.jposServerRequestHandler(mtiMatcher);
        }
        return mtiMatcher;
    }


    private final Handler<JPOSServerRequest> mtiHandler = new Handler<JPOSServerRequest>() {
        @Override
        public void handle(final JPOSServerRequest jposServerRequest) {
            Exchange exchange = getEndpoint().createExchange();
            final Message in = exchange.getIn();

            try {
                final ISOMsg isoMsg = jposServerRequest.getIsoMsg();
                getEndpoint().addISOMsgToMessage(isoMsg, in);
                logger.info(String.format("[JposConsumer] Incoming request - %s", jposServerRequest.getMTI()));

                logger.debug("[JposConsumer] Ready to process exchange: {}.", getProcessor().toString(), exchange);
                getProcessor().process(exchange, new OptionalAsyncResultHandler() {
                    @Override
                    public void handle(AsyncExchangeResult asyncExchangeResult) {
                        if (asyncExchangeResult.failed()) {
                            logger.error("Not sure yet what to return to JPOS if we fail", asyncExchangeResult.getException());
                            return;
                        }

                        try {
                            final Exchange result = asyncExchangeResult.result.get();
                            final ISOMsg body = result.getOut().getBody(ISOMsg.class);

                            ISOMsg response = body;
                            if (response == null) {
                                isoMsg.setResponseMTI();
                                response = isoMsg;
                            }
                            jposServerRequest.getOut().sendISOMsg(response);
                        } catch (Exception e) {
                            logError(e);
                        }
                    }
                });
            } catch (Exception e) {
                logError(e);
            }
        }
    };




    public JposChannelAdapter getEndpoint() {
        return endpoint;
    }
}
