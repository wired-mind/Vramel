Getting Started
===============
For now: 
    
    git clone git@github.com:wired-mind/vert-x-transports.git
    cd vert-x-transports
    gradle install
    
    git clone git@github.com:wired-mind/Vramel.git
    cd Vramel
    gradle install
    compile("com.nxttxn:vramel-core:1.0-SNAPSHOT")

Vramel
======

Vramel started as a Java DSL inspired by Camel, but using the Vertx threading model. However, as it progressed it became much easier to mimic the Camel API as closely as possible allowing many camel components to be ported with little effort.

* Camel (http://camel.apache.org) is a framework for building EIP solutions.
* Vertx (http://vertx.io) is a newer Java framework inspired by Node.js. It is a highly concurrent, yet simple programming framework.

Most of Camel is built on two core concepts: Pipeline and Multicast. Camel uses thread pools to make this happen. Vramel has ground up implementations for these core components (and others) that take complete advantage of the Vertx framework.

Vramel is not yet a complete port of Camel. Below is a list of components available in Vramel:

Patterns
==========

* Pipeline
* Multicast
* Enrich
* Choice
* Dynamic Router
* Filter
* Log
* Routing Slip
* Splitter

Components
==========

* Direct
* GSON
* Vertx (event bus)
* VertxQueue
* REST
* Axis2
* BeanIO
* Bean
* Timer
* Jibx
* XMLBeans
* XMLSecurity
* JPOS
* Properties

Status
======
Most, but not all of the core camel code is ported. One major part that needs work is toF(), fromF() api's. Originally we had these customized to pass config objects that were specific to our project. 
However, now we have ported Properties and Vramel "can" support the same URL based configuration that Camel uses. Most of the components though are not yet compatible with this. Therefore, the extra "config"
parameter in toF and fromF are legacy and will eventually go away.

Most of the Simple and Bean expression support is ported. Your mileage may vary though.

Vertx Integration (mod-vramel)
=================
This project includes a base class for use in Vertx projects that will scan for Vramel flows and start them inside a Vramel Context automatically.

Create a Verticle that extends VramelBusMod and pass a config setting called "packageName". Any classes in this package that extend FlowBuilder will be created and the flows added to a VramelContext. 

DSL
===
Right now Vramel only uses the Java DSL. In theory this could be extended to other implementations like Camel.

Examples
=======
    fromF("rest:POST:/v1/alerts")
       .log(LoggingLevel.DEBUG, MyClass.class.getName(), "[API] [Raw json] : ${body}")
       .choice()
           .when(new XHubSignaturePredicate("secret"))
               .process(new EventPreProcessor())
               .unmarshal().json(AlertPojo.class)
               .process(new AlertRouter())
               .marshal().json()
               .routingSlip(header("slip"))
           .end()
           .otherwise()
               .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
               .setHeader(Exchange.HTTP_STATUS_MESSAGE, constant("Invalid XHubSignature"))
               .transform(constant(new JsonObject().putString("error", "Invalid XHubSignature").toString()))
           .end();

    fromF("vertx:%s", "special_processing")
        .unmarshal().json(AlertPojo.class)
        .setHeader(AlertPojo.ORIGINAL, body())
        .enrich("vertx:alertMetrics", new KeyedBodyAggregationStrategy())
        .choice()
            .when(simple("${body.thresholdExceeded}"))
                .log("[Alerting] [Threshold Exceeded]")
                .marshal().json()
                .setHeader("needsUpdate", constant(true))
                .toF("vertxQueue:%s", "persistenceQueue")
            .otherwise()
                .log("[Alerting] [Threshold Not Exceeded]")
                .marshal().json().toF("vertxQueue:%s", "persistenceQueue")
        .end();
        
    fromF("vertx:%s", "undelivered
        .process(new UndeliveredQuery())
        .toF("rest:GET:/v1/events")
        .unmarshal().json(Results.class);