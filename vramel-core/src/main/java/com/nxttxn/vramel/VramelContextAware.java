package com.nxttxn.vramel;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 6:42 PM
 * To change this template use File | Settings | File Templates.
 */
public interface VramelContextAware {
    VramelContext getVramelContext();
    void setVramelContext(VramelContext vramelContext);
}
