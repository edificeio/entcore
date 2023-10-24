package org.entcore.audience;

import io.vertx.core.Promise;
import org.entcore.audience.controllers.AudienceController;
import org.entcore.audience.reaction.dao.ReactionDao;
import org.entcore.audience.reaction.dao.impl.ReactionDaoImpl;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.audience.reaction.service.impl.ReactionServiceImpl;
import org.entcore.audience.services.AudienceService;
import org.entcore.audience.services.impl.AudienceRepositoryEvents;
import org.entcore.audience.services.impl.AudienceServiceImpl;
import org.entcore.audience.view.dao.ViewDao;
import org.entcore.audience.view.dao.impl.ViewDaoImpl;
import org.entcore.audience.view.service.ViewService;
import org.entcore.audience.view.service.impl.ViewServiceImpl;
import org.entcore.common.http.BaseServer;
import org.entcore.common.sql.ISql;
import org.entcore.common.sql.Sql;

import java.util.Set;
import java.util.stream.Collectors;

public class Audience extends BaseServer {
  private AudienceController audienceController;

  @Override
  public void start(final Promise<Void> startPromise) throws Exception {
    super.start(startPromise);
    final ISql isql = Sql.getInstance();
    final ReactionDao reactionDao = new ReactionDaoImpl(isql);
    final ReactionService reactionService = new ReactionServiceImpl(vertx.eventBus(), reactionDao);
    final ViewDao viewDao = new ViewDaoImpl(isql);
    final ViewService viewService = new ViewServiceImpl(viewDao);
    final AudienceService audienceService = new AudienceServiceImpl(reactionService, viewService);
    final Set<String> validReactionTypes = config.getJsonObject("publicConf").getJsonArray("reaction-types").stream().map(Object::toString).collect(Collectors.toSet());
    audienceController = new AudienceController(vertx, config(), reactionService, viewService, audienceService, validReactionTypes);
    addController(audienceController);
    setRepositoryEvents(new AudienceRepositoryEvents(audienceService));
    startPromise.tryComplete();
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    super.stop(stopPromise);
    audienceController.stopResourceDeletionListener();
  }
}
