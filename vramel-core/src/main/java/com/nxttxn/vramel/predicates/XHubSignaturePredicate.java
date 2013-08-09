package com.nxttxn.vramel.predicates;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.Predicate;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 10:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class XHubSignaturePredicate extends SignatureBaseStringValidationSupport implements Predicate {

    public XHubSignaturePredicate(String secret) {
        super(secret);
    }
    @Override
    public boolean matches(Exchange input) {
        final Message in = input.getIn();
        final String signatureHeader = in.getHeader("x-hub-signature");
        final String signature = signatureHeader.replaceFirst("sha1=", "");
        final String signatureBaseString = new String(in.getBody(byte[].class));
        return isValid(signature, signatureBaseString);
    }
}
