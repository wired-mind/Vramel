package com.nxttxn.vramel.components.axis2;

import com.google.common.base.Optional;
import com.nxttxn.axis2.transport.vertx.VertxServiceClient;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.impl.DefaultProducer;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.rampart.RampartMessageData;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 1:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class Axis2Producer extends DefaultProducer {


    private final Axis2ChannelAdapter endpoint;
    private final Optional<String> wsdl;
    private final Optional<String> port;
    private final Optional<String> username;
    private final Optional<String> policy;
    private final Optional<String> password;
    private VertxServiceClient serviceClient;

    private final Optional<String> namespaceURI;
    private final Optional<String> service;


    public Axis2Producer(Endpoint endpoint, ConfigurationContext basicConfigurationContext) throws Exception {
        super(endpoint);
        this.endpoint = (Axis2ChannelAdapter) endpoint;

        final JsonObject config = endpoint.getConfig();
        wsdl = Optional.of(config.getString("wsdl"));
        namespaceURI = Optional.of(config.getString("namespaceURI"));
        service = Optional.of(config.getString("service"));
        port = Optional.of(config.getString("port"));
        policy = Optional.fromNullable(config.getString("policy", null));
        username = Optional.fromNullable(config.getString("username", null));
        password = Optional.fromNullable(config.getString("password", null));
        serviceClient = new VertxServiceClient(basicConfigurationContext, getClass().getResource(wsdl.get()),
                new QName(namespaceURI.get(), service.get()), port.get());



        Options opts = new Options();
        //setting target EPR
        opts.setTo(new EndpointReference(this.endpoint.getPath()));

        if (policy.isPresent()) {
            opts.setProperty(RampartMessageData.KEY_RAMPART_POLICY, loadPolicy(policy.get()));
            opts.setUserName(username.get());
            opts.setPassword(password.get());
        }

        serviceClient.setOptions(opts);

    }

    private Policy loadPolicy(String name) throws XMLStreamException {
        InputStream resource = this.getClass().getResourceAsStream(name);

        StAXOMBuilder builder = new StAXOMBuilder(resource);
        return PolicyEngine.getPolicy(builder.getDocumentElement());
    }



    @Override
    public void process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {

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
    }

}
