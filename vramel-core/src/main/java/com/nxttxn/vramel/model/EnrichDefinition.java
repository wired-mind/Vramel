package com.nxttxn.vramel.model;

import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.processor.Enricher;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.spi.FlowContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 12:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class EnrichDefinition extends NoOutputDefinition<EnrichDefinition> {
    private final AggregationStrategy aggregationStrategy;
    private final String resourceUri;

    public EnrichDefinition(AggregationStrategy aggregationStrategy, String resourceUri) {
        super();
        this.aggregationStrategy = aggregationStrategy;
        this.resourceUri = resourceUri;
    }

    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        checkNotNull(aggregationStrategy);
        // lookup endpoint
        Endpoint endpoint = flowContext.resolveEndpoint(resourceUri);

        Enricher enricher = new Enricher(aggregationStrategy, endpoint.createProducer());

        return enricher;
    }
}
