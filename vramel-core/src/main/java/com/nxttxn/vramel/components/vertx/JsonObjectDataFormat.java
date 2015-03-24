package com.nxttxn.vramel.components.vertx;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.spi.DataFormat;
import com.nxttxn.vramel.util.IOHelper;
import org.apache.commons.io.IOUtils;
import org.vertx.java.core.json.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by chuck on 3/19/15.
 */
public class JsonObjectDataFormat implements DataFormat {
    @Override
    public void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception {
        BufferedWriter writer = IOHelper.buffered(new OutputStreamWriter(stream));
        final JsonObject jsonObject = (JsonObject) graph;
        writer.write(jsonObject.toString());
        writer.close();
    }

    @Override
    public Object unmarshal(Exchange exchange, InputStream stream) throws Exception {

        final StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, StandardCharsets.UTF_8.name());
        Object result = new JsonObject(writer.toString());

        return result;
    }
}
