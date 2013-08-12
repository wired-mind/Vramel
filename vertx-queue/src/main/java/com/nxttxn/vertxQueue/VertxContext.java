package com.nxttxn.vertxQueue;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 5/6/13
 * Time: 12:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxContext {
    private static final Logger LOG = LoggerFactory.getLogger(VertxContext.class);
    private Vertx vertx;

    public VertxContext(int port) {
        vertx = Vertx.newVertx(port, getDefaultAddress());
        LOG.info(String.format("VertxContext initialized with vertx instance using port: %s.", port));
    }

    public HazelcastInstance getHazelcastInstance() {
        Set<HazelcastInstance> instances = Hazelcast.getAllHazelcastInstances();

        return instances.iterator().next();
    }

    /*
    Get default interface to use since the user hasn't specified one
     */
    private String getDefaultAddress() {
        Enumeration<NetworkInterface> nets;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }
        NetworkInterface netinf;
        while (nets.hasMoreElements()) {
            netinf = nets.nextElement();

            Enumeration<InetAddress> addresses = netinf.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (!address.isAnyLocalAddress() && !address.isMulticastAddress()
                        && !(address instanceof Inet6Address)) {
                    return address.getHostAddress();
                }
            }
        }
        return null;
    }

    public Vertx getVertx() {
        return vertx;
    }
}
