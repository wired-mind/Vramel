package com.nxttxn.vramel.model;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.spi.DataFormat;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.util.IntrospectionSupport;
import com.nxttxn.vramel.util.ObjectHelper;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DataFormatDefinition {

    private String dataFormatName;
    private DataFormat dataFormat;

    public DataFormatDefinition(String dataFormatName) {

        this.dataFormatName = dataFormatName;
    }


    public DataFormatDefinition(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    protected DataFormatDefinition() {
    }

    /**
     * Factory method to create the data format
     *
     * @param flowContext route context
     * @param type         the data format type
     * @param ref          reference to lookup for a data format
     * @return the data format or null if not possible to create
     */
    public static DataFormat getDataFormat(FlowContext flowContext, DataFormatDefinition type, String ref) throws Exception {
        if (type == null) {
            throw new UnsupportedOperationException("This method isn't fully implemented yet");
//            ObjectHelper.notNull(ref, "ref or type");
//
//            // try to let resolver see if it can resolve it, its not always possible
//            type = ((ModelVramelContext)flowContext.getVramelContext()).resolveDataFormatDefinition(ref);
//
//            if (type != null) {
//                return type.getDataFormat(flowContext);
//            }
//
//            DataFormat dataFormat = flowContext.getVramelContext().resolveDataFormat(ref);
//            if (dataFormat == null) {
//                throw new IllegalArgumentException("Cannot find data format in registry with ref: " + ref);
//            }
//
//            return dataFormat;
        } else {
            return type.getDataFormat(flowContext);
        }
    }

    public DataFormat getDataFormat(FlowContext flowContext) throws Exception {
        if (dataFormat == null) {
            dataFormat = createDataFormat(flowContext);
            if (dataFormat != null) {
                configureDataFormat(dataFormat);
            } else {
                throw new IllegalArgumentException(
                        "Data format '" + (dataFormatName != null ? dataFormatName : "<null>") + "' could not be created. "
                                + "Ensure that the data format is valid and the associated Camel component is present on the classpath");
            }
        }
        return dataFormat;
    }

    /**
     * Factory method to create the data format instance
     */
    protected DataFormat createDataFormat(FlowContext flowContext) throws Exception {

        //for now just assume it is already set by now since we haven't implemented a resolver
        return dataFormat;
//        if (dataFormatName != null) {
//            return flowContext.getVramelContext().resolveDataFormat(dataFormatName);
//        }
//        return null;
    }

    /**
     * Allows derived classes to customize the data format
     */
    protected void configureDataFormat(DataFormat dataFormat) {
    }



    public String getDataFormatName() {
        return dataFormatName;
    }

    public void setDataFormatName(String dataFormatName) {
        this.dataFormatName = dataFormatName;
    }

    public DataFormat getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    public String getShortName() {
        String name = getClass().getSimpleName();
        if (name.endsWith("DataFormat")) {
            name = name.substring(0, name.indexOf("DataFormat"));
        }
        return name;
    }

    /**
     * Sets a named property on the data format instance using introspection
     */
    protected void setProperty(Object bean, String name, Object value) {
        try {
            IntrospectionSupport.setProperty(bean, name, value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set property: " + name + " on: " + bean + ". Reason: " + e, e);
        }
    }
}
