package org.entcore.audience;

import org.entcore.audience.controllers.AudienceController;
import org.entcore.common.http.BaseServer;

public class Audience extends BaseServer {

  @Override
  public void start() throws Exception {
    super.start();
    addController(new AudienceController(vertx, config()));
  }
}
