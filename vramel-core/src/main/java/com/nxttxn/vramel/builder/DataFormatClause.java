package com.nxttxn.vramel.builder;

import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.model.DataFormatDefinition;
import com.nxttxn.vramel.model.ProcessorDefinition;
import com.nxttxn.vramel.model.dataformat.*;
import org.apache.camel.util.jsse.KeyStoreParameters;

import java.util.Map;


/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
//This class is a bridge to reuse camel data formats
public class DataFormatClause<T extends ProcessorDefinition<?>> {
    private final T processorType;
    private final Operation operation;

    public DataFormatClause(T processorType, Operation operation) {

        this.processorType = processorType;
        this.operation = operation;
    }

    private T dataFormat(DataFormatDefinition dataFormatType) {
        switch (operation) {
            case Unmarshal:
                return (T) processorType.unmarshal(dataFormatType);
            case Marshal:
                return (T) processorType.marshal(dataFormatType);
            default:
                throw new IllegalArgumentException("Unknown DataFormat operation: " + operation);
        }
    }

    /**
     * Uses the xmlBeans data format
     */
    public T xmlBeans() {
        return dataFormat(new XMLBeansDataFormat());
    }




    /**
     * Uses the JiBX data format.
     */
    public T jibx() {
        return dataFormat(new JibxDataFormat());
    }



    /**
     * Uses the JiBX data format with unmarshall class.
     */
    public T jibx(Class<?> unmarshallClass) {
        return dataFormat(new JibxDataFormat(unmarshallClass));
    }

    public T json() {
        return json(Object.class);
    }

    public T json(Class<?> unmarshallClass) {
        final JsonDataFormat json = new JsonDataFormat(JsonLibrary.Gson);
        json.setUnmarshalType(unmarshallClass);
        return dataFormat(json);
    }

    public T json(Expression expression) {
        return dataFormat(new JsonDataFormat(JsonLibrary.Gson, expression));
    }

    public enum Operation {
        Marshal, Unmarshal
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML() {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat();
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, boolean secureTagContents) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, secureTagContents);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, Map<String, String> namespaces, boolean secureTagContents) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, namespaces, secureTagContents);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, boolean secureTagContents, String passPhrase) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, secureTagContents, passPhrase);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, Map<String, String> namespaces, boolean secureTagContents, String passPhrase) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, namespaces, secureTagContents, passPhrase);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, boolean secureTagContents, String passPhrase, String xmlCipherAlgorithm) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, secureTagContents, passPhrase, xmlCipherAlgorithm);
        return dataFormat(xsdf);
    }


    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, Map<String, String> namespaces, boolean secureTagContents, String passPhrase, String xmlCipherAlgorithm) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, namespaces, secureTagContents, passPhrase, xmlCipherAlgorithm);
        return dataFormat(xsdf);
    }

    /**
     * @deprectaed Use {@link #secureXML(String, Map, boolean, String, String, String, String) instead.
     * Uses the XML Security data format
     */
    @Deprecated
    public T secureXML(String secureTag, boolean secureTagContents, String recipientKeyAlias, String xmlCipherAlgorithm,
                       String keyCipherAlgorithm) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, secureTagContents, recipientKeyAlias, xmlCipherAlgorithm, keyCipherAlgorithm);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, boolean secureTagContents, String recipientKeyAlias, String xmlCipherAlgorithm,
                       String keyCipherAlgorithm, String keyOrTrustStoreParametersId) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, secureTagContents, recipientKeyAlias, xmlCipherAlgorithm,
                keyCipherAlgorithm, keyOrTrustStoreParametersId);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, boolean secureTagContents, String recipientKeyAlias, String xmlCipherAlgorithm,
                       String keyCipherAlgorithm, String keyOrTrustStoreParametersId, String keyPassword) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, secureTagContents, recipientKeyAlias, xmlCipherAlgorithm,
                keyCipherAlgorithm, keyOrTrustStoreParametersId, keyPassword);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, boolean secureTagContents, String recipientKeyAlias, String xmlCipherAlgorithm,
                       String keyCipherAlgorithm, KeyStoreParameters keyOrTrustStoreParameters) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, secureTagContents, recipientKeyAlias, xmlCipherAlgorithm,
                keyCipherAlgorithm, keyOrTrustStoreParameters);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, boolean secureTagContents, String recipientKeyAlias, String xmlCipherAlgorithm,
                       String keyCipherAlgorithm, KeyStoreParameters keyOrTrustStoreParameters, String keyPassword) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, secureTagContents, recipientKeyAlias, xmlCipherAlgorithm,
                keyCipherAlgorithm, keyOrTrustStoreParameters, keyPassword);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, Map<String, String> namespaces, boolean secureTagContents, String recipientKeyAlias,
                       String xmlCipherAlgorithm, String keyCipherAlgorithm, String keyOrTrustStoreParametersId) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, namespaces, secureTagContents, recipientKeyAlias, xmlCipherAlgorithm,
                keyCipherAlgorithm, keyOrTrustStoreParametersId);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, Map<String, String> namespaces, boolean secureTagContents, String recipientKeyAlias,
                       String xmlCipherAlgorithm, String keyCipherAlgorithm, String keyOrTrustStoreParametersId, String keyPassword) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, namespaces, secureTagContents, recipientKeyAlias, xmlCipherAlgorithm,
                keyCipherAlgorithm, keyOrTrustStoreParametersId, keyPassword);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, Map<String, String> namespaces, boolean secureTagContents, String recipientKeyAlias,
                       String xmlCipherAlgorithm, String keyCipherAlgorithm, KeyStoreParameters keyOrTrustStoreParameters) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, namespaces, secureTagContents, recipientKeyAlias, xmlCipherAlgorithm,
                keyCipherAlgorithm, keyOrTrustStoreParameters);
        return dataFormat(xsdf);
    }

    /**
     * Uses the XML Security data format
     */
    public T secureXML(String secureTag, Map<String, String> namespaces, boolean secureTagContents, String recipientKeyAlias,
                       String xmlCipherAlgorithm, String keyCipherAlgorithm, KeyStoreParameters keyOrTrustStoreParameters, String keyPassword) {
        XMLSecurityDataFormat xsdf = new XMLSecurityDataFormat(secureTag, namespaces, secureTagContents, recipientKeyAlias, xmlCipherAlgorithm,
                keyCipherAlgorithm, keyOrTrustStoreParameters, keyPassword);
        return dataFormat(xsdf);
    }

    /**
     * Uses the beanio data format
     */
    public T beanio(String mapping, String streamName) {
        BeanioDataFormat dataFormat = new BeanioDataFormat();
        dataFormat.setMapping(mapping);
        dataFormat.setStreamName(streamName);
        return dataFormat(dataFormat);
    }

    /**
     * Uses the beanio data format
     */
    public T beanio(String mapping, String streamName, String encoding) {
        BeanioDataFormat dataFormat = new BeanioDataFormat();
        dataFormat.setMapping(mapping);
        dataFormat.setStreamName(streamName);
        dataFormat.setEncoding(encoding);
        return dataFormat(dataFormat);
    }

    /**
     * Uses the beanio data format
     */
    public T beanio(String mapping, String streamName, String encoding,
                    boolean ignoreUnidentifiedRecords, boolean ignoreUnexpectedRecords, boolean ignoreInvalidRecords) {
        BeanioDataFormat dataFormat = new BeanioDataFormat();
        dataFormat.setMapping(mapping);
        dataFormat.setStreamName(streamName);
        dataFormat.setEncoding(encoding);
        dataFormat.setIgnoreInvalidRecords(ignoreInvalidRecords);
        dataFormat.setIgnoreUnexpectedRecords(ignoreUnexpectedRecords);
        dataFormat.setIgnoreInvalidRecords(ignoreInvalidRecords);
        return dataFormat(dataFormat);
    }

}
