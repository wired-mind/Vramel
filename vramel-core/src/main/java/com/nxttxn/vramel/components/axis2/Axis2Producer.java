package com.nxttxn.vramel.components.axis2;

import com.google.common.base.Optional;
import com.nxttxn.axis2.transport.vertx.VertxServiceClient;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.RuntimeVramelException;
import com.nxttxn.vramel.impl.DefaultAsyncProducer;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.AsyncProcessorHelper;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.rampart.RampartMessageData;
import org.apache.rampart.policy.model.CryptoConfig;
import org.apache.rampart.policy.model.RampartConfig;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.vertx.java.core.json.JsonObject;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 1:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class Axis2Producer extends DefaultAsyncProducer {


    private final Axis2ChannelAdapter endpoint;
    private final Optional<String> wsdl;
    private final Optional<String> port;
    private final Optional<String> username;
    private final Optional<String> policyPath;
    private final Optional<String> password;
    private final Optional<String> userCertAlias;
    private final Optional<String> encryptionUser;
    private VertxServiceClient serviceClient;

    private final Optional<String> namespaceURI;
    private final Optional<String> service;
    private final Optional<String> keystorePath;
    private final Optional<String> keystorePassword;


    public Axis2Producer(Endpoint endpoint, ConfigurationContext basicConfigurationContext) throws Exception {
        super(endpoint);
        this.endpoint = (Axis2ChannelAdapter) endpoint;

        final JsonObject config = this.endpoint.getConfig();
        wsdl = Optional.of(config.getString("wsdl"));
        namespaceURI = Optional.of(config.getString("namespaceURI"));
        service = Optional.of(config.getString("service"));
        port = Optional.of(config.getString("port"));
        policyPath = Optional.fromNullable(config.getString("policy", null));
        keystorePath = Optional.fromNullable(config.getString("keystorePath", null));
        keystorePassword = Optional.fromNullable(config.getString("keystorePassword", null));
        username = Optional.fromNullable(config.getString("username", null));
        password = Optional.fromNullable(config.getString("password", null));
        userCertAlias = Optional.fromNullable(config.getString("userCertAlias", null));
        encryptionUser = Optional.fromNullable(config.getString("encryptionUser", null));
        serviceClient = new VertxServiceClient(basicConfigurationContext, getClass().getResource(wsdl.get()),
                new QName(namespaceURI.get(), service.get()), port.get());


        Options opts = new Options();
        //setting target EPR
        opts.setTo(new EndpointReference(this.endpoint.getPath()));
        opts.setReplyTo(new EndpointReference("http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous"));
        if (policyPath.isPresent()) {
            final Policy policy = loadPolicy(policyPath.get());

            final boolean shouldConfigureRampart = keystorePath.isPresent();
            if (shouldConfigureRampart) {
                RampartConfig rampartConfig = new RampartConfig();

                rampartConfig.setUser(username.get());
                if (userCertAlias.isPresent()) {
                    rampartConfig.setUserCertAlias(userCertAlias.get());
                }

                if (encryptionUser.isPresent()) {
                    rampartConfig.setEncryptionUser(encryptionUser.get());
                }

                CryptoConfig crypto = buildCrypto(keystorePath.get(), keystorePassword.get());
                //right now, just force all three to use the same config
                rampartConfig.setSigCryptoConfig(crypto);
                rampartConfig.setEncrCryptoConfig(crypto);
                rampartConfig.setDecCryptoConfig(crypto);

                policy.addAssertion(rampartConfig);

                opts.setProperty(WSHandlerConstants.PW_CALLBACK_REF, new CallbackHandler() {
                    @Override
                    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                        if (password.isPresent() == false) {
                            throw new RuntimeVramelException("Axis2Producer expects the password to be set in the config");
                        }

                        WSPasswordCallback pwcb = (WSPasswordCallback) callbacks[0];
                        int usage = pwcb.getUsage();
                        if (usage == WSPasswordCallback.USERNAME_TOKEN) {

                            pwcb.setPassword(password.get());

                        } else if (usage == WSPasswordCallback.SIGNATURE || usage == WSPasswordCallback.DECRYPT) {
                            pwcb.setPassword(password.get());
                        }
                    }
                });

            }

            opts.setProperty(RampartMessageData.KEY_RAMPART_POLICY, policy);

            //basic usernameToken support
            if (username.isPresent()) {
                opts.setUserName(username.get());
                opts.setPassword(password.get());
            }
        }

        serviceClient.setOptions(opts);

    }

    private CryptoConfig buildCrypto(String keystorePath, String keystorePassword) {
        CryptoConfig cryptoConfig = new CryptoConfig();
        cryptoConfig.setProvider("org.apache.ws.security.components.crypto.Merlin");

        Properties props = new Properties();
        props.setProperty("org.apache.ws.security.crypto.merlin.keystore.type", "JKS");
        props.setProperty("org.apache.ws.security.crypto.merlin.file", keystorePath);
        props.setProperty("org.apache.ws.security.crypto.merlin.keystore.password", keystorePassword);

        cryptoConfig.setProp(props);
        return cryptoConfig;
    }

    private Policy loadPolicy(String name) throws XMLStreamException {
        InputStream resource = this.getClass().getResourceAsStream(name);

        StAXOMBuilder builder = new StAXOMBuilder(resource);
        return PolicyEngine.getPolicy(builder.getDocumentElement());
    }


    @Override
    public boolean process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {

        final byte[] body = exchange.getIn().getBody();
        final String xmlAsString = new String(body);
        final OMElement xmlPayload = AXIOMUtil.stringToOM(xmlAsString);


        logger.info(String.format("[Axis2 Producer] [Request] [%s]", endpoint.getPath()));
        logger.debug(String.format("[Axis2 Producer] [Request] [%s] - Request body: %s", endpoint.getPath(), xmlAsString));

        serviceClient.sendWithVertx(xmlPayload, new AxisCallback() {
            @Override
            public void onMessage(MessageContext msgContext) {
                handleResponse(msgContext);
            }

            private void handleResponse(MessageContext msgContext) {
                //maybe something other than toString?
                final SOAPBody responseBody = msgContext.getEnvelope().getBody();
                final byte[] responseXml = responseBody.getFirstElement().toString().getBytes();
                exchange.getOut().setBody(responseXml);
                optionalAsyncResultHandler.done(exchange);
            }

            @Override
            public void onFault(MessageContext msgContext) {
                handleResponse(msgContext);
            }

            @Override
            public void onError(Exception e) {
                exchange.setException(e);
                optionalAsyncResultHandler.done(exchange);
            }

            @Override
            public void onComplete() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        return false;
    }

    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }
}
