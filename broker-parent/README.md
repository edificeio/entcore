# Broker module

## Presentation

This module is a multimodule project that provides a broker for the ENT outside of Vert.x's event bus.

## Modules

### broker-api

This module contains :
- the DTOs used by the broker's listeners
- the annotations used to mark the listeners `@BrokerListener`

Every object that is handled by one of the listeners must be typed and this type should be in this module.

This module should be added as dependency in the other modules that need to communicate with the broker.

### broker-proxy

This module contains only interfaces that describe the events that should go directly from the broker to the event bus.

Example :

```java
public interface DirectoryBrokerListener {

  @BrokerListener(subject = "directory.shares.create", proxy = true)
  CreateSharesResponseDTO createShares(CreateSharesRequestDTO request);

  @BrokerListener(subject = "directory.shares.delete", proxy = true)
  DeleteSharesResponseDTO deleteShares(DeleteSharesRequestDTO request);
}
```

This will automatically generate the code that will :
1. listen to the event `directory.shares.create` on the broker
2. forward all events to the event bus with the same subject (in this case `directory.shares.create` and `directory.shares.delete`)
3. forward the response to the broker

Typically, an ENT module that want to implement such a listener will do the following :

```java

import io.vertx.core.Vertx;

public final DirectoryBrokerListenerImpl implements DirectoryBrokerListener {
  private final Vertx vertx;
  public DirectoryBrokerListenerImpl(final Vertx vertx){
    this.vertx = vertx;
  }

  @Override
  public CreateSharesResponseDTO createShares(CreateSharesRequestDTO request) {
    // Do stuff here
  }

  @Override
  public DeleteSharesResponseDTO deleteShares(DeleteSharesRequestDTO request) {
    // Do stuff here
  }
}

// In the main verticle
@Override
public void start(final Promise<Void> startPromise) throws Exception {
  super.start(startPromise);
  BrokerProxyUtils.addBrokerProxy(new DirectoryBrokerListener(vertx));
}
```

### broker-nats

This module contains the necessary code to interact with NATS.

It provides the annotation processor `BrokerListenerProcessor`.
It should be added like that to the `pom.xml` of modules that contains @BrokerListener annotated methods :

```xml

<dependencies>
    ...
    <!-- To get the annotation -->
    <dependency>
        <groupId>org.entcore</groupId>
        <artifactId>broker-api</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
...
<build>
    <plugins>
        <!-- To process the annotation -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <executions>
                <execution>
                    <id>default-compile</id>
                    <configuration>
                        <annotationProcessors>
                            <annotationProcessor>org.entcore.broker.nats.BrokerListenerProcessor</annotationProcessor>
                            <annotationProcessor>fr.wseduc.processor.ControllerAnnotationProcessor</annotationProcessor>
                        </annotationProcessors>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
### broker

This module actually starts the broker (in the case of NATS it will connect to the cluster) and registers the listeners.

To add a listener :
1. Add it to the package `org.entcore.broker.listener` (or in a subpackage)
2. Add the `@BrokerListener` annotation to the methods that should handle NATS events
3. Add the request type and response POJO to the `broker-api` module

#### Installation

```shell
./build.sh init
```



