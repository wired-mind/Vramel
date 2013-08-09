package com.nxttxn.vramel.impl.jpos;

import com.nxttxn.vramel.components.jpos.DefaultJPOSServerRequest;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 8/2/13
 * Time: 1:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class JPOSServer {
    protected final Logger logger = LoggerFactory.getLogger(JPOSServer.class);
    private final Vertx vertx;
    private final JPOSChannelIn in;
    private final JPOSChannelOut out;
    private JPOSChannel jposChannel;

    private Handler<JPOSServerRequest> jposServerRequestHandler;



    public JPOSServer(Vertx vertx) {
        this.vertx = vertx;
        in = new JPOSChannelIn();
        out = new JPOSChannelOut();
    }


    public Handler<JPOSServerRequest> jposServerRequestHandler() {
        return jposServerRequestHandler;
    }

    public JPOSServer jposServerRequestHandler(Handler<JPOSServerRequest> jposServerRequestHandler) {
        this.jposServerRequestHandler = jposServerRequestHandler;
        return this;
    }

    public JPOSServer listen(int port, String host) {
        checkNotNull(jposServerRequestHandler);

        in.newISOMsgHandler(new Handler<ISOMsg>() {
            @Override
            public void handle(ISOMsg isoMsg) {
                jposServerRequestHandler.handle(new DefaultJPOSServerRequest(out, isoMsg));
            }
        });
        jposChannel = new JPOSChannel(in, out);
        vertx.createNetServer()
                .connectHandler(jposChannel)
                .listen(port, host);
        return this;
    }
}
