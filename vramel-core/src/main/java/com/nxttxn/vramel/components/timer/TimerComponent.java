package com.nxttxn.vramel.components.timer;

import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.impl.DefaultComponent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 8/23/13
 * Time: 2:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimerComponent extends DefaultComponent {

    public TimerComponent(VramelContext vramelContext) {
        super(vramelContext);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        TimerEndpoint answer = new TimerEndpoint(uri, this, remaining);

        // convert time from String to a java.util.Date using the supported patterns
        String time = getAndRemoveParameter(parameters, "time", String.class);
        String pattern = getAndRemoveParameter(parameters, "pattern", String.class);
        if (time != null) {
            SimpleDateFormat sdf;
            if (pattern != null) {
                sdf = new SimpleDateFormat(pattern);
            } else if (time.contains("T")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            } else {
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            }
            Date date = sdf.parse(time);
            answer.setTime(date);
        }

        setProperties(answer, parameters);
        return answer;
    }


}
