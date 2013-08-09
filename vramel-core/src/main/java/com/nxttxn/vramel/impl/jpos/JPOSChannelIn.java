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
public class JPOSChannelIn implements WriteStream {


    protected final Logger logger = LoggerFactory.getLogger(JPOSChannelIn.class);
    private Handler<Void> drainHandler;
    private Handler<Exception> exceptionHandler;
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
    public void writeBuffer(Buffer data) {
        isoXmlParser.handle(data);
    }

    @Override
    public void setWriteQueueMaxSize(int maxSize) {
        //no op
    }

    @Override
    public boolean writeQueueFull() {
        return false;
    }

    @Override
    public void drainHandler(Handler<Void> drainHandler) {

        this.drainHandler = drainHandler;
    }

    @Override
    public void exceptionHandler(Handler<Exception> exceptionHandler) {

        this.exceptionHandler = exceptionHandler;
    }


    public void newISOMsgHandler(Handler<ISOMsg> newISOMsgHandler) {
        this.newISOMsgHandler = newISOMsgHandler;
    }


}
