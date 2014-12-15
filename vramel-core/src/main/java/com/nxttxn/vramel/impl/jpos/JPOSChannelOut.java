package com.nxttxn.vramel.impl.jpos;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.XMLPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/29/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class JPOSChannelOut implements ReadStream<JPOSChannelOut> {
    protected final Logger logger = LoggerFactory.getLogger(JPOSChannelOut.class);
    private Handler<Buffer> dataHandler;
    private boolean paused;
    private Handler<Throwable> exceptionHandler;
    private Handler<Void> endHandler;



    @Override
    public JPOSChannelOut dataHandler(Handler<Buffer> dataHandler) {

        this.dataHandler = dataHandler;
        return this;
    }

    @Override
    public JPOSChannelOut pause() {
        paused = true;
        return this;
    }



    @Override
    public JPOSChannelOut resume() {
        paused = false;
        return this;
    }

    @Override
    public JPOSChannelOut exceptionHandler(Handler<Throwable> exceptionHandler) {

        this.exceptionHandler = exceptionHandler;
        return this;
    }

    @Override
    public JPOSChannelOut endHandler(Handler<Void> endHandler) {

        this.endHandler = endHandler;
        return this;
    }


    public void sendISOMsg(ISOMsg isoMsg) throws Exception {
        if (dataHandler == null) {
            throw new RuntimeException("JPOSChannelOut is not configured properly. Please set the data handler.");
        }

        if (paused) {
            throw new RuntimeException("JPOSChannelOut is currently paused.");
        }


        XMLPackager packager = new XMLPackager();
        isoMsg.setPackager(packager);
        final Buffer packedXmlBuffer = new Buffer(isoMsg.pack());

        logger.info("[JPOSChannelOut] Sending this message to jpos {}", packedXmlBuffer.toString());

        dataHandler.handle(packedXmlBuffer);
    }
}
