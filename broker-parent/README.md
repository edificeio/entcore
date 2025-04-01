# Broker module

## Presentation

This module is a multimodule project that provides a broker for the ENT outside of Vert.x's event bus.

## Modules

### broker-api
This module contains the DTOs used by the broker's listeners.
Every object that is handled by one of the listeners must be typed and 
this type should be in this module.

This module should be added as dependency in the other modules that need to communicate with the broker.

### broker-nats

This module contains the necessary code to interact with NATS.

It also provides the annotation @NATSListener which should mark all the methods that handles NATS events.

NB : @NATSListener an NATSListenerProcessor are duplicates (yet slightly different) version of what is in the project
[contracts-parent](https://github.com/edificeio/edifice-quarkus-contracts-parent). They had to be duplicated to be able
to be used by ENT's Java 8 code.

### broker-service

This module actually starts the broker (in the case of NATS it will connect to the cluster) and registers the listeners.

To add a listener :
1. Add it to the package `org.entcore.broker.listener` (or in a subpackage)
2. Add the @NATSListener annotation to the methods that should handle NATS events
3. Add the request type and response POJO to the `broker-api` module

#### Installation

```shell
./build.sh init
```



