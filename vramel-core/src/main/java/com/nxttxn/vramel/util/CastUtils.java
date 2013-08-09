package com.nxttxn.vramel.util;

import javax.naming.NamingEnumeration;
import java.util.*;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 8:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class CastUtils {
    private CastUtils() {
        //utility class, never constructed
    }

    public static <T, U> Map<T, U> cast(Map<?, ?> p) {
        return (Map<T, U>) p;
    }

    public static <T, U> Map<T, U> cast(Map<?, ?> p, Class<T> t, Class<U> u) {
        return (Map<T, U>) p;
    }

    public static <T> Collection<T> cast(Collection<?> p) {
        return (Collection<T>) p;
    }

    public static <T> Collection<T> cast(Collection<?> p, Class<T> cls) {
        return (Collection<T>) p;
    }

    public static <T> List<T> cast(List<?> p) {
        return (List<T>) p;
    }

    public static <T> List<T> cast(List<?> p, Class<T> cls) {
        return (List<T>) p;
    }

    public static <T> Iterator<T> cast(Iterator<?> p) {
        return (Iterator<T>) p;
    }

    public static <T> Iterator<T> cast(Iterator<?> p, Class<T> cls) {
        return (Iterator<T>) p;
    }

    public static <T> Set<T> cast(Set<?> p) {
        return (Set<T>) p;
    }

    public static <T> Set<T> cast(Set<?> p, Class<T> cls) {
        return (Set<T>) p;
    }

    public static <T> Queue<T> cast(Queue<?> p) {
        return (Queue<T>) p;
    }

    public static <T> Queue<T> cast(Queue<?> p, Class<T> cls) {
        return (Queue<T>) p;
    }

    public static <T> Deque<T> cast(Deque<?> p) {
        return (Deque<T>) p;
    }

    public static <T> Deque<T> cast(Deque<?> p, Class<T> cls) {
        return (Deque<T>) p;
    }

    public static <T, U> Hashtable<T, U> cast(Hashtable<?, ?> p) {
        return (Hashtable<T, U>) p;
    }

    public static <T, U> Hashtable<T, U> cast(Hashtable<?, ?> p, Class<T> pc, Class<U> uc) {
        return (Hashtable<T, U>) p;
    }

    public static <T, U> Map.Entry<T, U> cast(Map.Entry<?, ?> p) {
        return (Map.Entry<T, U>) p;
    }

    public static <T, U> Map.Entry<T, U> cast(Map.Entry<?, ?> p, Class<T> pc, Class<U> uc) {
        return (Map.Entry<T, U>) p;
    }

    public static <T> Enumeration<T> cast(Enumeration<?> p) {
        return (Enumeration<T>) p;
    }

    public static <T> NamingEnumeration<T> cast(NamingEnumeration<?> p) {
        return (NamingEnumeration<T>) p;
    }

    public static <T> Class<T> cast(Class<?> p) {
        return (Class<T>) p;
    }

    public static <T> Class<T> cast(Class<?> p, Class<T> cls) {
        return (Class<T>) p;
    }

    public static <T> Future<T> cast(Future<?> p) {
        return (Future<T>) p;
    }

    public static <T> T as(Class<T> clazz, Object o){
        if(clazz.isInstance(o)){
            return clazz.cast(o);
        }
        return null;
    }
}
