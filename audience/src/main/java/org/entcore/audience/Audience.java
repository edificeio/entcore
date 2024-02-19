package org.entcore.audience;

import org.entcore.audience.controllers.AudienceController;
import org.entcore.audience.reaction.dao.ReactionDao;
import org.entcore.audience.reaction.dao.impl.ReactionDaoImpl;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.audience.reaction.service.impl.ReactionServiceImpl;
import org.entcore.audience.services.AudienceService;
import org.entcore.audience.services.impl.AudienceRepositoryEvents;
import org.entcore.audience.services.impl.AudienceServiceImpl;
import org.entcore.common.http.BaseServer;
import org.entcore.common.sql.ISql;
import org.entcore.common.sql.Sql;

public class Audience extends BaseServer {

  @Override
  public void start() throws Exception {
    super.start();
    final AudienceService audienceService = new AudienceServiceImpl();
    final ISql isql = Sql.getInstance();
    final ReactionDao reactionDao = new ReactionDaoImpl(isql);
    final ReactionService reactionService = new ReactionServiceImpl(reactionDao);
    addController(new AudienceController(vertx, config(), reactionService));
    setRepositoryEvents(new AudienceRepositoryEvents(audienceService));
  }
}
