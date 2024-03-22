package org.entcore.common.editor;

import fr.wseduc.transformer.ContentTransformerFactoryProvider;
import fr.wseduc.transformer.IContentTransformerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;

public class EventStoredContentTransformerFactoryProvider {

  private EventStoredContentTransformerFactoryProvider() {};

  /**
   * Initialization method, must be called before getting a factory
   * @param vertx vertx instance
   */
  public static void init(final Vertx vertx) {
    ContentTransformerFactoryProvider.init(vertx);
  }

  /**
   * Gets a factory according to the context specified in the configuration
   * @param contentTransformerConfig the content transformer client configuration
   * @return the content transformer client factory
   */
  public static IContentTransformerFactory getFactory(final String appName, JsonObject contentTransformerConfig) {
    IContentTransformerFactory factory = ContentTransformerFactoryProvider.getFactory(appName, contentTransformerConfig);
    if (contentTransformerConfig != null && contentTransformerConfig.getBoolean("store-event", true)) {
      final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(appName);
      factory = new EventStoredContentTransformerClientFactory(appName, eventStore, factory);
    }
    return factory;
  }
}
