package com.nxttxn.vramel.components.gson;

import com.google.gson.*;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.builder.ExpressionBuilder;
import com.nxttxn.vramel.spi.DataFormat;
import com.nxttxn.vramel.util.IOHelper;

import java.io.*;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 4:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class GsonDataFormat implements DataFormat {
    private final Expression unmarshalTypeExpression;
    private Gson gson;

    private List<ExclusionStrategy> exclusionStrategies;
    private LongSerializationPolicy longSerializationPolicy;
    private FieldNamingPolicy fieldNamingPolicy;
    private FieldNamingStrategy fieldNamingStrategy;
    private Boolean serializeNulls;
    private Boolean prettyPrinting;
    private String dateFormatPattern;
    private Boolean polymorphic;

    public GsonDataFormat() throws Exception {
        this(ExpressionBuilder.constantExpression(Map.class));
    }

    /**
     * Use the default Gson {@link Gson} and with a custom
     * unmarshal type
     *
     * @param unmarshalTypeExpression the custom unmarshal type
     */
    public GsonDataFormat(Expression unmarshalTypeExpression) throws Exception {
        this(null, unmarshalTypeExpression);
    }


    /**
     * Use a custom Gson mapper and and unmarshal type
     *
     * @param gson          the custom mapper
     * @param unmarshalTypeExpression the custom unmarshal type
     */
    public GsonDataFormat(Gson gson, Expression unmarshalTypeExpression) throws Exception {
        this.gson = gson;
        this.unmarshalTypeExpression = unmarshalTypeExpression;
    }

    @Override
    public void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception {
        BufferedWriter writer = IOHelper.buffered(new OutputStreamWriter(stream));
        getGson().toJson(graph, writer);
        writer.close();
    }

    @Override
    public Object unmarshal(Exchange exchange, InputStream stream) throws Exception {
        Class unmarshalType = this.unmarshalTypeExpression.evaluate(exchange, Class.class);
        BufferedReader reader = IOHelper.buffered(new InputStreamReader(stream));
        //Object result = getGson().fromJson(reader, unmarshalType);

        final Gson hgson = new GsonBuilder().setDateFormat("YYYY-MM-DD HH:MM:SS").setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        Object result = hgson.fromJson(reader, unmarshalType);

        reader.close();
        return result;
    }

    protected void buildDefaultGson() throws Exception {
        GsonBuilder builder = new GsonBuilder().setDateFormat("YYYY-MM-DD HH:MM:SS").setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        if (exclusionStrategies != null && !exclusionStrategies.isEmpty()) {
            ExclusionStrategy[] strategies = exclusionStrategies.toArray(new ExclusionStrategy[exclusionStrategies.size()]);
            builder.setExclusionStrategies(strategies);
        }
        if (longSerializationPolicy != null) {
            builder.setLongSerializationPolicy(longSerializationPolicy);
        }
        if (fieldNamingPolicy != null) {
            builder.setFieldNamingPolicy(fieldNamingPolicy);
        }
        if (fieldNamingStrategy != null) {
            builder.setFieldNamingStrategy(fieldNamingStrategy);
        }
        if (serializeNulls != null && serializeNulls) {
            builder.serializeNulls();
        }
        if (prettyPrinting != null && prettyPrinting) {
            builder.setPrettyPrinting();
        }
        if (dateFormatPattern != null) {
            builder.setDateFormat(dateFormatPattern);
        }
//        if (polymorphic != null && polymorphic) {
//            builder.registerTypeAdapter(unmarshalType, new InheritanceAdapter<Object>());
//        }
        gson =  builder.create();
    }


    // Properties
    // -------------------------------------------------------------------------

//    public Class<?> getUnmarshalType() {
//        return this.unmarshalType;
//    }
//
//    public void setUnmarshalType(Class<?> unmarshalType) {
//        this.unmarshalType = unmarshalType;
//    }

    public List<ExclusionStrategy> getExclusionStrategies() {
        return exclusionStrategies;
    }

    public void setExclusionStrategies(List<ExclusionStrategy> exclusionStrategies) {
        this.exclusionStrategies = exclusionStrategies;
    }

    public LongSerializationPolicy getLongSerializationPolicy() {
        return longSerializationPolicy;
    }

    public void setLongSerializationPolicy(LongSerializationPolicy longSerializationPolicy) {
        this.longSerializationPolicy = longSerializationPolicy;
    }

    public FieldNamingPolicy getFieldNamingPolicy() {
        return fieldNamingPolicy;
    }

    public void setFieldNamingPolicy(FieldNamingPolicy fieldNamingPolicy) {
        this.fieldNamingPolicy = fieldNamingPolicy;
    }

    public FieldNamingStrategy getFieldNamingStrategy() {
        return fieldNamingStrategy;
    }

    public void setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
        this.fieldNamingStrategy = fieldNamingStrategy;
    }

    public Boolean getSerializeNulls() {
        return serializeNulls;
    }

    public void setSerializeNulls(Boolean serializeNulls) {
        this.serializeNulls = serializeNulls;
    }

    public Boolean getPrettyPrinting() {
        return prettyPrinting;
    }

    public void setPrettyPrinting(Boolean prettyPrinting) {
        this.prettyPrinting = prettyPrinting;
    }

    public String getDateFormatPattern() {
        return dateFormatPattern;
    }

    public void setDateFormatPattern(String dateFormatPattern) {
        this.dateFormatPattern = dateFormatPattern;
    }

    public Gson getGson() throws Exception {
        if (gson == null) {
            buildDefaultGson();
        }
        return this.gson;
    }

    public void setPolymorphic(Boolean polymorphic) {
        this.polymorphic = polymorphic;
    }

    private class InheritanceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T>{

        private static final String CLASSNAME = "CLASSNAME";
        private static final String INSTANCE  = "INSTANCE";

        @Override
        public JsonElement serialize(T src, Type typeOfSrc,
                                     JsonSerializationContext context) {

            JsonObject retValue = new JsonObject();
            String className = src.getClass().getCanonicalName();
            retValue.addProperty(CLASSNAME, className);
            JsonElement elem = context.serialize(src);
            retValue.add(INSTANCE, elem);
            return retValue;
        }

        @Override
        public T deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext context) throws JsonParseException  {
            JsonObject jsonObject =  json.getAsJsonObject();
            JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
            String className = prim.getAsString();

            Class<?> klass = null;
            try {
                klass = Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new JsonParseException(e.getMessage());
            }
            return context.deserialize(jsonObject.get(INSTANCE), klass);
        }

    }
}
