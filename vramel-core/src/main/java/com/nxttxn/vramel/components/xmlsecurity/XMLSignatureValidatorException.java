package com.nxttxn.vramel.components.xmlsecurity;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 8/16/13
 * Time: 12:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMLSignatureValidatorException extends RuntimeException {
    public XMLSignatureValidatorException(String msg) {
        super(msg);
    }

    public XMLSignatureValidatorException(String msg, Exception ex) {
        super(msg, ex);
    }
}
