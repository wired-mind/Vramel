package com.nxttxn.vramel.impl.jpos;

import com.nxttxn.vramel.impl.jpos.JPOSChannelOut;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 8/2/13
 * Time: 2:09 PM
 * To change this template use File | Settings | File Templates.
 */
public interface JPOSServerRequest {
    JPOSChannelOut getOut();

    ISOMsg getIsoMsg();

    String getMTI() throws ISOException;
}
