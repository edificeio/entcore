package org.entcore.audience;

import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import org.entcore.audience.controllers.AudienceController;
import org.entcore.audience.reaction.dao.ReactionDao;
import org.entcore.audience.reaction.dao.impl.ReactionDaoImpl;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.audience.reaction.service.impl.ReactionServiceImpl;
import org.entcore.audience.services.AudienceService;
import org.entcore.audience.services.impl.AudienceRepositoryEvents;
import org.entcore.audience.services.impl.AudienceServiceImpl;
import org.entcore.audience.view.dao.ViewDao;
import org.entcore.audience.view.dao.ViewDaoImpl;
import org.entcore.audience.view.service.ViewService;
import org.entcore.audience.view.service.impl.ViewServiceImpl;
import org.entcore.common.http.BaseServer;
import org.entcore.common.sql.ISql;
import org.entcore.common.sql.Sql;

public class Audience extends BaseServer {

  private MessageConsumer<Object> resourceDeletionListener;

  @Override
  public void start() throws Exception {
    super.start();
    final ISql isql = Sql.getInstance();
    final ReactionDao reactionDao = new ReactionDaoImpl(isql);
    final ReactionService reactionService = new ReactionServiceImpl(vertx.eventBus(), reactionDao);
    final ViewDao viewDao = new ViewDaoImpl(isql);
    final ViewService viewService = new ViewServiceImpl(viewDao);
    final AudienceService audienceService = new AudienceServiceImpl(reactionService, viewService);
    final AudienceController audienceController = new AudienceController(vertx, config(), reactionService, viewService, audienceService);
    addController(audienceController);
    setRepositoryEvents(new AudienceRepositoryEvents(audienceService));
    resourceDeletionListener = audienceController.listenForResourceDeletionNotification();
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    super.stop(stopPromise);
    if (resourceDeletionListener != null) {
      resourceDeletionListener.unregister();
    }
  }
}
