package com.nxttxn.vramel.model.dataformat;

import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.builder.ExpressionBuilder;
import com.nxttxn.vramel.components.gson.GsonDataFormat;
import com.nxttxn.vramel.model.DataFormatDefinition;
import com.nxttxn.vramel.spi.DataFormat;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.util.ObjectHelper;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 4:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonDataFormat extends DataFormatDefinition {
    private final JsonLibrary library;
    private Boolean prettyPrint;
    private String unmarshalTypeName;
    private Class<?> unmarshalType;
    private Expression expression;

    public JsonDataFormat(JsonLibrary library) {
        this.library = library;
    }

    public JsonDataFormat(JsonLibrary library, Expression expression) {

        this.library = library;
        this.expression = expression;
    }


    public Boolean getPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(Boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }


    public Class<?> getUnmarshalType() {
        return unmarshalType;
    }

    public void setUnmarshalType(Class<?> unmarshalType) {
        this.unmarshalType = unmarshalType;
    }

    public String getUnmarshalTypeName() {
        return unmarshalTypeName;
    }

    public void setUnmarshalTypeName(String unmarshalTypeName) {
        this.unmarshalTypeName = unmarshalTypeName;
    }


    @Override
    protected DataFormat createDataFormat(FlowContext flowContext) throws Exception {

        if (expression == null) {
            expression = ExpressionBuilder.constantExpression(this.unmarshalType);
        }
//        if (library == JsonLibrary.XStream) {
//            setProperty(this, "dataFormatName", "json-xstream");
//        } else if (library == JsonLibrary.Jackson) {
//            setProperty(this, "dataFormatName", "json-jackson");
//        } else {
//            setProperty(this, "dataFormatName", "json-gson");
//        }

        //hard coding gson for now
        setProperty(this, "dataFormat", new GsonDataFormat(expression));

        if (unmarshalType == null && unmarshalTypeName != null) {
            try {
                unmarshalType = flowContext.getVramelContext().getClassResolver().resolveMandatoryClass(unmarshalTypeName);
            } catch (ClassNotFoundException e) {
                throw ObjectHelper.wrapRuntimeCamelException(e);
            }
        }



        return super.createDataFormat(flowContext);
    }

    @Override
    protected void configureDataFormat(DataFormat dataFormat) {
        if (unmarshalType != null) {
            setProperty(dataFormat, "unmarshalType", unmarshalType);
        }
        if (prettyPrint != null) {
            setProperty(dataFormat, "prettyPrint", unmarshalType);
        }
    }
}
