package com.nxttxn.vramelmods;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.nxttxn.vramel.FlowsBuilder;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.components.properties.PropertiesComponent;
import com.nxttxn.vramel.impl.DefaultVramelContext;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.json.JsonObject;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/24/13
 * Time: 2:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class VramelBusMod extends BusModBase {

    private DefaultVramelContext vramelContext;

    @Override
    public void start() {
        super.start();

        PropertiesComponent pc = new PropertiesComponent();
        //right now we'll just hard code this. if it works we can do something fancy
        pc.setLocation("classpath:vramel.properties");
        pc.setIgnoreMissingLocation(true);
        vramelContext = new DefaultVramelContext(this);
        vramelContext.addComponent("properties", pc);


        final Config resolvedConfig = vramelContext.getResolvedConfig();
        final String packageName = resolvedConfig.getString("package-name");

        List<Class<? extends FlowsBuilder>> nonConcreteSubtypesOf = Lists.newArrayList();
        nonConcreteSubtypesOf.add(FlowsBuilder.class);
        nonConcreteSubtypesOf.addAll(findAllNonConcreteSubtypesOf(FlowsBuilder.class));
        nonConcreteSubtypesOf.addAll(additionalFlowsBuilderTypes());


        List<FlowsBuilder> flowsBuilders = Lists.newArrayList();
        for (Class<? extends FlowsBuilder> flowBuilderType : nonConcreteSubtypesOf) {
            flowsBuilders.addAll(createConcreteInstances(packageName, flowBuilderType));
        }

        for (FlowsBuilder flow : flowsBuilders) {
            try {
                vramelContext.addFlowBuilder(flow);
            } catch (Exception e) {
                logger.error(String.format("Error adding Flowbuilder to context %s", flow), e);
            }
            doWithVramelContext(vramelContext);
        }

        try {
            vramelContext.start();
        } catch (Exception e) {
            logger.error("Cannot start vramel context.", e);
        }


    }

    public Collection<? extends Class<? extends FlowsBuilder>> additionalFlowsBuilderTypes() {
        return Collections.emptyList();
    }


    protected void doWithVramelContext(VramelContext vramelContext) {

    }




    private <T> List<Class<? extends T>> findAllNonConcreteSubtypesOf(Class<T> aClass) {
        List<Class<? extends T>> subTypes = Lists.newArrayList();
        final String prefix = "com.nxttxn.vramel";
        Reflections reflections = createReflections(prefix);
        final Set<Class<? extends T>> subTypesOf = reflections.getSubTypesOf(aClass);
        for (Class<? extends T> subtype : subTypesOf) {
            final int modifiers = subtype.getModifiers();
            if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
                subTypes.add(subtype);
            }
            subTypes.addAll(findAllNonConcreteSubtypesOf(subtype));
        }

        return subTypes;
    }

    public <T> List<? extends T> createConcreteInstances(final String packageName, Class<T> type) {

        Reflections reflections = createReflections(packageName);

        final Set<Class<? extends T>> concreteClasses = reflections.getSubTypesOf(type);


        return new ArrayList<T>() {{
            for (Class<? extends T> concreteClass : concreteClasses) {
                try {

                    logger.info(String.format("Scanning package, %s, for FlowBuilders. Found: %s. ", packageName, concreteClass.getName()));
                    final T flowsBuilder = concreteClass.newInstance();
                    add(flowsBuilder);


                } catch (Exception e) {
                    logger.error("Error creating flowbuilder", e);
                }
            }

        }};
    }

    private Reflections createReflections(String packageName) {
        final Set<URL> urls = ClasspathHelper.forPackage(packageName);


        return new Reflections(
                new ConfigurationBuilder()
                        .setUrls(urls)
                        .filterInputsBy(new FilterBuilder().includePackage(packageName))
                        .setScanners(new SubTypesScanner()));
    }





}
