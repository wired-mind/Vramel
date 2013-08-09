package com.nxttxn.vramel.components.jpos;

import com.nxttxn.vramel.impl.jpos.JPOSChannelOut;
import com.nxttxn.vramel.impl.jpos.JPOSServerRequest;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 8/2/13
 * Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultJPOSServerRequest implements JPOSServerRequest {
    private final JPOSChannelOut out;
    private final ISOMsg isoMsg;

    public DefaultJPOSServerRequest(JPOSChannelOut out, ISOMsg isoMsg) {

        this.out = out;
        this.isoMsg = isoMsg;
    }

    @Override
    public JPOSChannelOut getOut() {
        return out;
    }

    @Override
    public ISOMsg getIsoMsg() {
        return isoMsg;
    }

    @Override
    public String getMTI() throws ISOException {
        return isoMsg.getMTI();
    }
}
