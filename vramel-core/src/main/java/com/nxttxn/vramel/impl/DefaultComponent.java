package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.Component;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.ResolveEndpointFailedException;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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

    @Deprecated
    protected String preProcessUri(String uri) {
        // Give components a chance to preprocess URIs and migrate to URI syntax that discourages invalid URIs
        // (see CAMEL-4425)
        // check URI string to the unsafe URI characters
        String encodedUri = UnsafeUriCharactersEncoder.encode(uri);
        if (!encodedUri.equals(uri)) {
            // uri supplied is not really valid
            LOG.warn("Supplied URI '{}' contains unsafe characters, please check encoding", uri);
        }
        return encodedUri;
    }

    /**
     * A factory method allowing derived components to create a new endpoint
     * from the given URI, remaining path and optional parameters
     *
     * @param uri the full URI of the endpoint
     * @param remaining the remaining part of the URI without the query
     *                parameters or component prefix
     * @param parameters the optional parameters passed in
     * @return a newly created endpoint or null if the endpoint cannot be
     *         created based on the inputs
     * @throws Exception is thrown if error creating the endpoint
     */
    protected abstract Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)
            throws Exception;




    @Override
    public Endpoint createEndpoint(String uri, JsonObject config) throws Exception {
        ObjectHelper.notNull(getVramelContext(), "camelContext");
        // check URI string to the unsafe URI characters
        String encodedUri = preProcessUri(uri);
        URI u = new URI(encodedUri);
        String path = useRawUri() ? u.getRawSchemeSpecificPart() : u.getSchemeSpecificPart();

        // lets trim off any query arguments
        if (path.startsWith("//")) {
            path = path.substring(2);
        }
        int idx = path.indexOf('?');
        if (idx > -1) {
            path = path.substring(0, idx);
        }

        Map<String, Object> parameters;
        if (useRawUri()) {
            // when using raw uri then the query is taking from the uri as is
            String query;
            idx = uri.indexOf('?');
            if (idx > -1) {
                query = uri.substring(idx + 1);
            } else {
                query = u.getRawQuery();
            }
            // and use method parseQuery
            parameters = URISupport.parseQuery(query, true);
        } else {
            // however when using the encoded (default mode) uri then the query,
            // is taken from the URI (ensures values is URI encoded)
            // and use method parseParameters
            parameters = URISupport.parseParameters(u);
        }

        //merge uri params with json config params allowing config file to override uri params
        parameters = new JsonObject(parameters).mergeIn(config).toMap();

        // parameters using raw syntax: RAW(value)
        // should have the token removed, so its only the value we have in parameters, as we are about to create
        // an endpoint and want to have the parameter values without the RAW tokens
        URISupport.resolveRawParameterValues(parameters);

        // use encoded or raw uri?
        uri = useRawUri() ? uri : encodedUri;

        validateURI(uri, path, parameters);
        if (LOG.isTraceEnabled()) {
            // at trace level its okay to have parameters logged, that may contain passwords
            LOG.trace("Creating endpoint uri=[{}], path=[{}], parameters=[{}]", new Object[]{URISupport.sanitizeUri(uri), URISupport.sanitizePath(path), parameters});
        } else if (LOG.isDebugEnabled()) {
            // but at debug level only output sanitized uris
            LOG.debug("Creating endpoint uri=[{}], path=[{}]", new Object[]{URISupport.sanitizeUri(uri), URISupport.sanitizePath(path)});
        }
        Endpoint endpoint = createEndpoint(uri, path, parameters);
        if (endpoint == null) {
            return null;
        }

        if (!parameters.isEmpty()) {
            endpoint.configureProperties(parameters);
            if (useIntrospectionOnEndpoint()) {
                setProperties(endpoint, parameters);
            }

            // if endpoint is strict (not lenient) and we have unknown parameters configured then
            // fail if there are parameters that could not be set, then they are probably misspell or not supported at all
            if (!endpoint.isLenientProperties()) {
                validateParameters(uri, parameters, null);
            }
        }

        afterConfiguration(uri, path, endpoint, parameters);
        return endpoint;
    }



    /**
     * Strategy to do post configuration logic.
     * <p/>
     * Can be used to construct an URI based on the remaining parameters. For example the parameters that configures
     * the endpoint have been removed from the parameters which leaves only the additional parameters left.
     *
     * @param uri the uri
     * @param remaining the remaining part of the URI without the query parameters or component prefix
     * @param endpoint the created endpoint
     * @param parameters the remaining parameters after the endpoint has been created and parsed the parameters
     * @throws Exception can be thrown to indicate error creating the endpoint
     */
    protected void afterConfiguration(String uri, String remaining, Endpoint endpoint, Map<String, Object> parameters) throws Exception {
        // noop
    }

    /**
     * Strategy for validation of parameters, that was not able to be resolved to any endpoint options.
     *
     * @param uri          the uri
     * @param parameters   the parameters, an empty map if no parameters given
     * @param optionPrefix optional prefix to filter the parameters for validation. Use <tt>null</tt> for validate all.
     * @throws ResolveEndpointFailedException should be thrown if the URI validation failed
     */
    protected void validateParameters(String uri, Map<String, Object> parameters, String optionPrefix) {
        Map<String, Object> param = parameters;
        if (optionPrefix != null) {
            param = IntrospectionSupport.extractProperties(parameters, optionPrefix);
        }

        if (param.size() > 0) {
            throw new ResolveEndpointFailedException(uri, "There are " + param.size()
                    + " parameters that couldn't be set on the endpoint."
                    + " Check the uri if the parameters are spelt correctly and that they are properties of the endpoint."
                    + " Unknown parameters=[" + param + "]");
        }
    }

    /**
     * Derived classes may wish to overload this to prevent the default introspection of URI parameters
     * on the created Endpoint instance
     */
    protected boolean useIntrospectionOnEndpoint() {
        return true;
    }


    /**
     * Strategy for validation of the uri when creating the endpoint.
     *
     * @param uri        the uri
     * @param path       the path - part after the scheme
     * @param parameters the parameters, an empty map if no parameters given
     * @throws ResolveEndpointFailedException should be thrown if the URI validation failed
     */
    protected void validateURI(String uri, String path, Map<String, Object> parameters) {
        // check for uri containing & but no ? marker
        if (uri.contains("&") && !uri.contains("?")) {
            throw new ResolveEndpointFailedException(uri, "Invalid uri syntax: no ? marker however the uri "
                    + "has & parameter separators. Check the uri if its missing a ? marker.");
        }

        // check for uri containing double && markers
        if (uri.contains("&&")) {
            throw new ResolveEndpointFailedException(uri, "Invalid uri syntax: Double && marker found. "
                    + "Check the uri and remove the duplicate & marker.");
        }

        // if we have a trailing & then that is invalid as well
        if (uri.endsWith("&")) {
            throw new ResolveEndpointFailedException(uri, "Invalid uri syntax: Trailing & marker found. "
                    + "Check the uri and remove the trailing & marker.");
        }
    }

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

    /**
     * Sets the bean properties on the given bean
     *
     * @param bean  the bean
     * @param parameters  properties to set
     */
    protected void setProperties(Object bean, Map<String, Object> parameters) throws Exception {
        // set reference properties first as they use # syntax that fools the regular properties setter
        EndpointHelper.setReferenceProperties(getVramelContext(), bean, parameters);
        EndpointHelper.setProperties(getVramelContext(), bean, parameters);
    }

    /**
     * Resolves a reference parameter in the registry and removes it from the map.
     *
     * @param <T>           type of object to lookup in the registry.
     * @param parameters    parameter map.
     * @param key           parameter map key.
     * @param type          type of object to lookup in the registry.
     * @return the referenced object or <code>null</code> if the parameter map
     *         doesn't contain the key.
     * @throws IllegalArgumentException if a non-null reference was not found in
     *         registry.
     */
    public <T> T resolveAndRemoveReferenceParameter(Map<String, Object> parameters, String key, Class<T> type) {
        return resolveAndRemoveReferenceParameter(parameters, key, type, null);
    }

    /**
     * Resolves a reference parameter in the registry and removes it from the map.
     *
     * @param <T>           type of object to lookup in the registry.
     * @param parameters    parameter map.
     * @param key           parameter map key.
     * @param type          type of object to lookup in the registry.
     * @param defaultValue  default value to use if the parameter map doesn't
     *                      contain the key.
     * @return the referenced object or the default value.
     * @throws IllegalArgumentException if referenced object was not found in
     *         registry.
     */
    public <T> T resolveAndRemoveReferenceParameter(Map<String, Object> parameters, String key, Class<T> type, T defaultValue) {
        String value = getAndRemoveParameter(parameters, key, String.class);
        if (value == null) {
            return defaultValue;
        } else {
            return EndpointHelper.resolveReferenceParameter(getVramelContext(), value.toString(), type);
        }
    }

    /**
     * Resolves a reference list parameter in the registry and removes it from
     * the map.
     *
     * @param parameters
     *            parameter map.
     * @param key
     *            parameter map key.
     * @param elementType
     *            result list element type.
     * @return the list of referenced objects or an empty list if the parameter
     *         map doesn't contain the key.
     * @throws IllegalArgumentException if any of the referenced objects was
     *         not found in registry.
     * @see EndpointHelper#resolveReferenceListParameter(VramelContext, String, Class)
     */
    public <T> List<T> resolveAndRemoveReferenceListParameter(Map<String, Object> parameters, String key, Class<T> elementType) {
        return resolveAndRemoveReferenceListParameter(parameters, key, elementType, new ArrayList<T>(0));
    }

    /**
     * Resolves a reference list parameter in the registry and removes it from
     * the map.
     *
     * @param parameters
     *            parameter map.
     * @param key
     *            parameter map key.
     * @param elementType
     *            result list element type.
     * @param defaultValue
     *            default value to use if the parameter map doesn't
     *            contain the key.
     * @return the list of referenced objects or the default value.
     * @throws IllegalArgumentException if any of the referenced objects was
     *         not found in registry.
     * @see EndpointHelper#resolveReferenceListParameter(VramelContext, String, Class)
     */
    public <T> List<T> resolveAndRemoveReferenceListParameter(Map<String, Object> parameters, String key, Class<T> elementType, List<T>  defaultValue) {
        String value = getAndRemoveParameter(parameters, key, String.class);

        if (value == null) {
            return defaultValue;
        } else {
            return EndpointHelper.resolveReferenceListParameter(getVramelContext(), value.toString(), elementType);
        }
    }
}
