package com.nxttxn.vramel;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/25/13
 * Time: 10:58 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Expression {
    <T>  T evaluate(Exchange exchange, java.lang.Class<T> tClass);
}
