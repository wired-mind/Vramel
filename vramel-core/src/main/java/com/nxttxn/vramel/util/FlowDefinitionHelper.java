package com.nxttxn.vramel.util;

import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.builder.ErrorHandlerBuilder;
import com.nxttxn.vramel.model.FlowDefinition;
import com.nxttxn.vramel.model.OnExceptionDefinition;
import com.nxttxn.vramel.model.ProcessorDefinition;
import com.nxttxn.vramel.model.ProcessorDefinitionHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/10/13
 * Time: 11:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class FlowDefinitionHelper {

    /**
     * Force assigning ids to the flows
     *
     * @param context the camel context
     * @param flows  the flows
     * @throws Exception is thrown if error force assign ids to the flows
     */
    public static void forceAssignIds(VramelContext context, List<FlowDefinition> flows) throws Exception {
        for (FlowDefinition flow : flows) {
            // force id on the flow
            flow.idOrCreate(context.getNodeIdFactory());

            // if there was a custom id assigned, then make sure to support property placeholders
            //not supported yet
//            if (flow.hasCustomIdAssigned()) {
//                String id = flow.getId();
//                flow.setId(context.resolvePropertyPlaceholders(id));
//            }
        }
    }

    /**
     * Validates that the target route has no duplicate id's from any of the existing routes.
     *
     * @param target  the target route
     * @param routes  the existing routes
     * @return <tt>null</tt> if no duplicate id's detected, otherwise the first found duplicate id is returned.
     */
    public static String validateUniqueIds(FlowDefinition target, List<FlowDefinition> routes) {
        Set<String> routesIds = new LinkedHashSet<String>();
        // gather all ids for the existing route, but only include custom ids, and no abstract ids
        // as abstract nodes is cross-cutting functionality such as interceptors etc
        for (FlowDefinition route : routes) {
            // skip target route as we gather ids in a separate set
            if (route == target) {
                continue;
            }
            ProcessorDefinitionHelper.gatherAllNodeIds(route, routesIds, true, false);
        }

        // gather all ids for the target route, but only include custom ids, and no abstract ids
        // as abstract nodes is cross-cutting functionality such as interceptors etc
        Set<String> targetIds = new LinkedHashSet<String>();
        ProcessorDefinitionHelper.gatherAllNodeIds(target, targetIds, true, false);

        // now check for clash with the target route
        for (String id : targetIds) {
            if (routesIds.contains(id)) {
                return id;
            }
        }

        return null;
    }

    /**
     * Prepares the flow which supports context scoped features such as onException, interceptors and onCompletions
     * <p/>
     * This method does <b>not</b> mark the flow as prepared afterwards.
     *
     * @param context                            the camel context
     * @param flow                              the flow
     * @param onExceptions                       optional list of onExceptions
     */
    public static void prepareRoute(VramelContext context, FlowDefinition flow,
                                    List<OnExceptionDefinition> onExceptions) {

        // abstracts is the cross cutting concerns
        List<ProcessorDefinition<?>> abstracts = new ArrayList<ProcessorDefinition<?>>();

        // upper is the cross cutting concerns such as interceptors, error handlers etc
        List<ProcessorDefinition<?>> upper = new ArrayList<ProcessorDefinition<?>>();

        // lower is the regular flow
        List<ProcessorDefinition<?>> lower = new ArrayList<ProcessorDefinition<?>>();

        FlowDefinitionHelper.prepareRouteForInit(flow, abstracts, lower);

        // parent and error handler builder should be initialized first
        initParentAndErrorHandlerBuilder(context, flow, abstracts, onExceptions);

        initOnExceptions(abstracts, upper, onExceptions);

        // rebuild flow as upper + lower
        flow.clearOutput();
        flow.getOutputs().addAll(lower);
        flow.getOutputs().addAll(0, upper);
    }

    private static void initOnExceptions(List<ProcessorDefinition<?>> abstracts, List<ProcessorDefinition<?>> upper,
                                         List<OnExceptionDefinition> onExceptions) {
        // add global on exceptions if any
        if (onExceptions != null && !onExceptions.isEmpty()) {
            for (OnExceptionDefinition output : onExceptions) {
                // these are context scoped on exceptions so set this flag
                output.setFlowScoped(false);
                abstracts.add(output);
            }
        }

        // now add onExceptions to the route
        for (ProcessorDefinition output : abstracts) {
            if (output instanceof OnExceptionDefinition) {
                // on exceptions must be added at top, so the route flow is correct as
                // on exceptions should be the first outputs

                // find the index to add the on exception, it should be in the top
                // but it should add itself after any existing onException
                int index = 0;
                for (int i = 0; i < upper.size(); i++) {
                    ProcessorDefinition up = upper.get(i);
                    if (!(up instanceof OnExceptionDefinition)) {
                        index = i;
                        break;
                    } else {
                        index++;
                    }
                }
                upper.add(index, output);
            }
        }
    }


    public static void prepareRouteForInit(FlowDefinition flow, List<ProcessorDefinition<?>> abstracts,
                                           List<ProcessorDefinition<?>> lower) {
        // filter the flow into abstracts and lower
        for (ProcessorDefinition output : flow.getOutputs()) {
            if (output.isAbstract()) {
                abstracts.add(output);
            } else {
                lower.add(output);
            }
        }
    }

    private static void initParentAndErrorHandlerBuilder(VramelContext context, FlowDefinition flow,
                                                         List<ProcessorDefinition<?>> abstracts, List<OnExceptionDefinition> onExceptions) {

        if (context != null) {
            // let the flow inherit the error handler builder from camel context if none already set

            // must clone to avoid side effects while building routes using multiple RouteBuilders
            ErrorHandlerBuilder builder = context.getErrorHandlerBuilder();
            if (builder != null) {
                builder = builder.cloneBuilder();
                flow.setErrorHandlerBuilderIfNull(builder);
            }
        }

        // init parent and error handler builder on the flow
        initParentAndErrorHandlerBuilder(flow);

        // set the parent and error handler builder on the global on exceptions
        if (onExceptions != null) {
            for (OnExceptionDefinition global : onExceptions) {
                //global.setErrorHandlerBuilder(context.getErrorHandlerBuilder());
                initParentAndErrorHandlerBuilder(global);
            }
        }
    }


    private static void initParentAndErrorHandlerBuilder(ProcessorDefinition parent) {
        List<ProcessorDefinition> children = parent.getOutputs();
        for (ProcessorDefinition child : children) {
            child.setParent(parent);
            if (child.getOutputs() != null && !child.getOutputs().isEmpty()) {
                // recursive the children
                initParentAndErrorHandlerBuilder(child);
            }
        }
    }
}
