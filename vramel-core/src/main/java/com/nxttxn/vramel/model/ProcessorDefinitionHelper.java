package com.nxttxn.vramel.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/3/13
 * Time: 4:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProcessorDefinitionHelper {

    /**
     * Is there any outputs in the given list.
     * <p/>
     * Is used for check if the route output has any real outputs (non abstracts)
     *
     * @param outputs           the outputs
     * @return <tt>true</tt> if has outputs, otherwise <tt>false</tt> is returned
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean hasOutputs(List<ProcessorDefinition<?>> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return false;
        }


        return !outputs.isEmpty();
    }

    /**
     * Is the given node parent(s) of the given type
     * @param parentType   the parent type
     * @param node         the current node
     * @param recursive    whether or not to check grand parent(s) as well
     * @return <tt>true</tt> if parent(s) is of given type, <tt>false</tt> otherwise
     */
    public static boolean isParentOfType(Class<?> parentType, ProcessorDefinition<?> node, boolean recursive) {
        if (node == null || node.getParent() == null) {
            return false;
        }

        if (parentType.isAssignableFrom(node.getParent().getClass())) {
            return true;
        } else if (recursive) {
            // recursive up the tree of parents
            return isParentOfType(parentType, node.getParent(), true);
        } else {
            // no match
            return false;
        }
    }

    /**
     * Traverses the node, including its children (recursive), and gathers all the node ids.
     *
     * @param node  the target node
     * @param set   set to store ids, if <tt>null</tt> a new set will be created
     * @param onlyCustomId  whether to only store custom assigned ids (ie. {@link org.apache.camel.model.OptionalIdentifiedDefinition#hasCustomIdAssigned()}
     * @param includeAbstract whether to include abstract nodes (ie. {@link org.apache.camel.model.ProcessorDefinition#isAbstract()}
     * @return the set with the found ids.
     */
    public static Set<String> gatherAllNodeIds(ProcessorDefinition<?> node, Set<String> set,
                                               boolean onlyCustomId, boolean includeAbstract) {
        if (node == null) {
            return set;
        }

//        // skip abstract
//        if (node.isAbstract() && !includeAbstract) {
//            return set;
//        }

        if (set == null) {
            set = new LinkedHashSet<String>();
        }

        // add ourselves
        if (node.getId() != null) {
            if (!onlyCustomId || node.hasCustomIdAssigned() && onlyCustomId) {
                set.add(node.getId());
            }
        }

        // traverse outputs and recursive children as well
        List<ProcessorDefinition<?>> children = node.getOutputs();
        if (children != null && !children.isEmpty()) {
            for (ProcessorDefinition<?> child : children) {
                // traverse children also
                gatherAllNodeIds(child, set, onlyCustomId, includeAbstract);
            }
        }

        return set;
    }


    /**
     * Gets the route definition the given node belongs to.
     *
     * @param node the node
     * @return the route, or <tt>null</tt> if not possible to find
     */
    public static FlowDefinition getFlow(ProcessorDefinition<?> node) {
        if (node == null) {
            return null;
        }

        ProcessorDefinition<?> def = node;
        // drill to the top
        while (def != null && def.getParent() != null) {
            def = def.getParent();
        }

        if (def instanceof FlowDefinition) {
            return (FlowDefinition) def;
        } else {
            // not found
            return null;
        }
    }
}
