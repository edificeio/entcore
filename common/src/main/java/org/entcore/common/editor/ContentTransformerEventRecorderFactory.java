package org.entcore.common.editor;

import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;

public class ContentTransformerEventRecorderFactory {
  private final String module;
  private final boolean storeEvent;

  /**
   * @param module Short name of the module that will require the recorder (e.g. blog, wiki)
   * @param contentTransformerConfig configuration of the content transformer module
   */
  public ContentTransformerEventRecorderFactory(final String module, JsonObject contentTransformerConfig) {
    this.module = module;
    this.storeEvent = contentTransformerConfig != null &&
      contentTransformerConfig.getBoolean("store-event", true);
  }

  /**
   * @return a new recorder based on the configuration of the service
   */
  public IContentTransformerEventRecorder create() {
    final IContentTransformerEventRecorder recorder;
    if (storeEvent) {
      final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(module);
      recorder = new ContentTransformerEventRecorder(eventStore, module);
    } else {
      recorder = IContentTransformerEventRecorder.noop;
    }
    return recorder;
  }
}
