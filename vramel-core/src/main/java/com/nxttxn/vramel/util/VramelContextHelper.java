package com.nxttxn.vramel.util;

import com.nxttxn.vramel.NoSuchBeanException;
import com.nxttxn.vramel.SyncProcessor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.components.bean.BeanProcessor;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 7:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class VramelContextHelper {
    public static <T> T lookup(VramelContext vramelContext, String name, Class<T> beanType) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /**
     * Look up the given named bean of the given type in the {@link org.apache.camel.spi.Registry} on the
     * {@link VramelContext} or throws NoSuchBeanException if not found.
     */
    public static <T> T mandatoryLookup(VramelContext context, String name, Class<T> beanType) {
        T answer = lookup(context, name, beanType);
        if (answer == null) {
            throw new NoSuchBeanException(name, beanType.getName());
        }
        return answer;
    }
    public static <T> T convertTo(VramelContext context, Class<T> type, Object value) {
        //might need to actually implement type converters :(

        return null;
    }
}
