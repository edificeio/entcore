package org.entcore.common.editor;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.IContentTransformerFactory;
import org.entcore.common.events.EventStore;

/**
 * Stores the multimedia composition of transformed content.
 */
public class EventStoredContentTransformerClientFactory implements IContentTransformerFactory {
  private final EventStore eventStore;

  private final IContentTransformerFactory innerFactory;
  private final String module;

  public EventStoredContentTransformerClientFactory(final String module, EventStore eventStore, IContentTransformerFactory innerFactory) {
    this.module = module;
    this.eventStore = eventStore;
    this.innerFactory = innerFactory;
  }

  @Override
  public IContentTransformerClient create() {
    final IContentTransformerClient innerClient = innerFactory.create();
    return new EventRecorderContentTransformerClient(innerClient, eventStore, module);
  }
}
