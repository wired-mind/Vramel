package com.nxttxn.vramel.components.rest;

import com.google.common.base.Joiner;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.ResolveEndpointFailedException;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.impl.DefaultComponent;
import com.nxttxn.vramel.spi.HeaderFilterStrategy;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.URISupport;
import com.nxttxn.vramel.util.UnsafeUriCharactersEncoder;
import org.vertx.java.core.json.JsonObject;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 7:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestComponent extends DefaultComponent {
    public RestComponent(VramelContext vramelContext) {
        super(vramelContext);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {

        Map<String, Object> httpClientParameters = new HashMap<String, Object>(parameters);
        final JsonObject config = new JsonObject(parameters).copy();

        HeaderFilterStrategy headerFilterStrategy = resolveAndRemoveReferenceParameter(parameters, "headerFilterStrategy", HeaderFilterStrategy.class);

        final String separator = ":";
        final String[] splitRemaining = remaining.split(separator);
        if (splitRemaining.length < 2) {
            throw new ResolveEndpointFailedException("Invalid Rest uri");
        }

        String method = splitRemaining[0];
        final String[] routeParts = Arrays.copyOfRange(splitRemaining, 1, splitRemaining.length);
        String route = Joiner.on(separator).join(routeParts);

        httpClientParameters.put("REST_METHOD", method);

        boolean secure = Objects.equals(config.getString("ssl", "false"), "true");

        // need to set scheme on address uri depending on if its secure or not
        String addressUri = (secure ? "https://" : "http://") + route;

        addressUri = UnsafeUriCharactersEncoder.encodeHttpURI(addressUri);
        URI uriHttpUriAddress = new URI(addressUri);

        // validate http uri that end-user did not duplicate the http part that can be a common error
        int pos = uri.indexOf("//");
        if (pos != -1) {
            String part = uri.substring(pos + 2);
            if (part.startsWith("http:") || part.startsWith("https:")) {
                throw new ResolveEndpointFailedException(uri,
                        "The uri part is not configured correctly. You have duplicated the http(s) protocol.");
            }
        }

        URI endpointUri = URISupport.createRemainingURI(uriHttpUriAddress, httpClientParameters);


        // the endpoint uri should use the component name as scheme, so we need to re-create it once more
        String scheme = ObjectHelper.before(uri, "://");
        endpointUri = URISupport.createRemainingURI(
                new URI(scheme,
                        endpointUri.getUserInfo(),
                        endpointUri.getHost(),
                        endpointUri.getPort(),
                        endpointUri.getPath(),
                        endpointUri.getQuery(),
                        endpointUri.getFragment()),
                httpClientParameters);

        // create the endpoint and set the http uri to be null
        String endpointUriString = endpointUri.toString();

        getVramelContext().getContainer().logger().debug("Creating endpoint uri {} " + endpointUriString);

        final RestChannelAdapter endpoint = new RestChannelAdapter(endpointUriString, getVramelContext(), route, method, config);

        // configure the endpoint
        setProperties(endpoint, parameters);
        parameters.clear();


        // we can not change the port of an URI, we must create a new one with an explicit port value
        URI httpUri = URISupport.createRemainingURI(
                new URI(uriHttpUriAddress.getScheme(),
                        uriHttpUriAddress.getUserInfo(),
                        uriHttpUriAddress.getHost(),
                        uriHttpUriAddress.getPort(),
                        uriHttpUriAddress.getPath(),
                        uriHttpUriAddress.getQuery(),
                        uriHttpUriAddress.getFragment()),
                parameters);

//        endpoint.setHttpUri(httpUri);
        if (headerFilterStrategy != null) {
            endpoint.setHeaderFilterStrategy(headerFilterStrategy);
        }


        return endpoint;

    }
}
