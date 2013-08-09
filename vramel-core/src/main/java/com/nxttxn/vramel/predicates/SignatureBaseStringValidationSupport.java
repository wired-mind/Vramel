package com.nxttxn.vramel.predicates;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/30/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class SignatureBaseStringValidationSupport {
    protected final Logger logger = LoggerFactory.getLogger(SignatureBaseStringValidationSupport.class);

    protected Mac mac;

    public SignatureBaseStringValidationSupport(String secret) {

        try {
            final String hmacSHA1 = "HmacSHA1";
            mac = Mac.getInstance(hmacSHA1);
            if (secret == null) {
                throw new RuntimeException("Missing Secret for XHubSignaturePredicate Key");
            }
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes("UTF-8"), hmacSHA1);
            mac.init(secretKeySpec);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean isValid(String signature, String signatureBaseString) {
        checkNotNull(signature, "Signature cannot be null");
        checkNotNull(signatureBaseString, "Signature Base String cannot be null");

        final String signatureChallenge = hmacSha1(signatureBaseString);
        logger.debug("Challenge signature is {}", signatureChallenge);
        return signature.equals(signatureChallenge);
    }

    protected String hmacSha1(String value) {
        byte[] digest;
        try {
            digest = mac.doFinal(value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        String result = Hex.encodeHexString(digest);
        return result;
    }


}
