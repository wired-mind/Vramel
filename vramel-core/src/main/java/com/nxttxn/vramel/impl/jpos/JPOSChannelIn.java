package com.nxttxn.vramel.impl.jpos;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.XMLPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.parsetools.RecordParser;
import org.vertx.java.core.streams.WriteStream;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/29/13
 * Time: 9:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class JPOSChannelIn implements WriteStream<JPOSChannelIn> {


    protected final Logger logger = LoggerFactory.getLogger(JPOSChannelIn.class);
    private Handler<Void> drainHandler;
    private Handler<Throwable> exceptionHandler;
    private Handler<ISOMsg> newISOMsgHandler;

    public static final String ISOMSG_DELIM = "</isomsg>\n";

    private RecordParser isoXmlParser = RecordParser.newDelimited(ISOMSG_DELIM, new Handler<Buffer>() {
        @Override
        public void handle(Buffer isoMsgBuffer) {
            isoMsgBuffer.appendString(ISOMSG_DELIM);
            logger.debug("Incoming jpos data {}", isoMsgBuffer.toString());
            try {
                final ISOMsg isoMsg = buildISOMsgFromBytes(isoMsgBuffer.getBytes());
                newISOMsgHandler.handle(isoMsg);
            } catch (Exception e) {
                handleException(e);
            }
        }
    });

    public static ISOMsg buildISOMsgFromBytes(byte[] bytes) throws ISOException {
        ISOMsg result = null;
        result = new ISOMsg();
        result.setPackager(new XMLPackager());
        result.unpack(bytes);

        return result;
    }

    private void handleException(Exception e) {
        if (exceptionHandler == null) {
            return;
        }

        exceptionHandler.handle(e);
    }

    @Override
    public JPOSChannelIn write(Buffer data) {
        isoXmlParser.handle(data);
        return this;
    }

    @Override
    public JPOSChannelIn setWriteQueueMaxSize(int maxSize) {
        //no op
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return false;
    }

    @Override
    public JPOSChannelIn drainHandler(Handler<Void> drainHandler) {

        this.drainHandler = drainHandler;
        return this;
    }

    @Override
    public JPOSChannelIn exceptionHandler(Handler<Throwable> exceptionHandler) {

        this.exceptionHandler = exceptionHandler;
        return this;
    }


    public void newISOMsgHandler(Handler<ISOMsg> newISOMsgHandler) {
        this.newISOMsgHandler = newISOMsgHandler;
    }


}
