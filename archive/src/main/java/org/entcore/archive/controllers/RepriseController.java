package org.entcore.archive.controllers;

import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import org.entcore.archive.services.RepriseService;

public class RepriseController extends BaseController
{
  private RepriseService  repriseService;

  public RepriseController(RepriseService repriseService)
  {
      this.repriseService = repriseService;
  }

  @BusAddress("entcore.reprise")
  public void export(Message<JsonObject> message)
  {
    String action = message.body().getString("action", "");
    switch (action)
    {
        case "imported" :
            this.repriseService.imported(
              message.body().getString("importId"),
              message.body().getString("app"),
              message.body().getJsonObject("rapport")
            );
            break;
      default: log.error("Reprise : invalid action " + action);
    }
  }
}
