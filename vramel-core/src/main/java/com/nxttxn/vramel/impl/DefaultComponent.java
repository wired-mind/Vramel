package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.Component;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.util.VramelContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.net.URI;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 6:47 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DefaultComponent implements Component{
    private static final transient Logger LOG = LoggerFactory.getLogger(DefaultComponent.class);
    private VramelContext vramelContext;

    public DefaultComponent(VramelContext vramelContext) {

        this.vramelContext = vramelContext;
    }

    protected DefaultComponent() {
    }


    @Override
    public Endpoint createEndpoint(String uri, JsonObject config) throws Exception {
        checkNotNull(getVramelContext());
        // check URI string to the unsafe URI characters

        URI u = new URI(uri);
        String path = useRawUri() ? u.getRawSchemeSpecificPart() : u.getSchemeSpecificPart();

        // lets trim off any query arguments
        if (path.startsWith("//")) {
            path = path.substring(2);
        }
        int idx = path.indexOf('?');
        if (idx > -1) {
            path = path.substring(0, idx);
        }


        Endpoint endpoint = createEndpoint(uri, path, config);


        return endpoint;
    }

    @Override
    public Endpoint createEndpoint(String uri) throws Exception {
        return createEndpoint(uri, new JsonObject());
    }

    /**
     * A factory method allowing derived components to create a new endpoint
     * from the given URI, remaining path and optional config
     *
     *
     * @param uri the full URI of the endpoint
     * @param remaining the remaining part of the URI without the query
     *                config or component prefix
     * @param config the optional config passed in
     * @return a newly created endpoint or null if the endpoint cannot be
     *         created based on the inputs
     * @throws Exception is thrown if error creating the endpoint
     */
    protected abstract Endpoint createEndpoint(String uri, String remaining, JsonObject config)
            throws Exception;

    @Override
    public VramelContext getVramelContext() {
        return vramelContext;
    }

    @Override
    public void setVramelContext(VramelContext vramelContext) {
        this.vramelContext = vramelContext;
    }


    public boolean useRawUri() {
        // should use encoded uri by default
        return false;
    }


    /**
     * Gets the parameter and remove it from the parameter map. This method doesn't resolve
     * reference parameters in the registry.
     *
     * @param parameters the parameters
     * @param key        the key
     * @param type       the requested type to convert the value from the parameter
     * @return  the converted value parameter, <tt>null</tt> if parameter does not exists.
     * @see #resolveAndRemoveReferenceParameter(Map, String, Class)
     */
    public <T> T getAndRemoveParameter(Map<String, Object> parameters, String key, Class<T> type) {
        return getAndRemoveParameter(parameters, key, type, null);
    }

    /**
     * Gets the parameter and remove it from the parameter map. This method doesn't resolve
     * reference parameters in the registry.
     *
     * @param parameters    the parameters
     * @param key           the key
     * @param type          the requested type to convert the value from the parameter
     * @param defaultValue  use this default value if the parameter does not contain the key
     * @return  the converted value parameter
     * @see #resolveAndRemoveReferenceParameter(Map, String, Class, Object)
     */
    public <T> T getAndRemoveParameter(Map<String, Object> parameters, String key, Class<T> type, T defaultValue) {
        Object value = parameters.remove(key);
        if (value == null) {
            value = defaultValue;
        }
        if (value == null) {
            return null;
        }

        return VramelContextHelper.convertTo(getVramelContext(), type, value);
    }

}
