package org.entcore.archive.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.bus.BusAddress;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;

import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.entcore.common.storage.Storage;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.buffer.Buffer;

import org.entcore.archive.services.DuplicationService;
import org.entcore.archive.services.impl.DefaultDuplicationService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

public class DuplicationController extends BaseController
{
  private EventBus            eb;
  private DuplicationService  dupService;

  public DuplicationController(Vertx vertx, Storage storage, String importPath)
  {
    this.eb = vertx.eventBus();
    this.dupService = new DefaultDuplicationService(vertx, storage, importPath);
  }

  @Post("/duplicate")
  public void duplicate(final HttpServerRequest request)
  {
    request.bodyHandler(new Handler<Buffer>()
    {
      @Override
      public void handle(Buffer buff)
      {
        JsonObject body = new JsonObject();
        try
        {
          body = buff.toJsonObject();
        }
        catch(Exception e)
        {
          log.error(e, e.getMessage());
          badRequest(request);
        }

        final String application = body.getString("application");
        final String resourceId = body.getString("resourceId");

        if (application == null || resourceId == null)
          badRequest(request);
        else
        {
          UserUtils.getUserInfos(eb, request, user ->
          {
            dupService.duplicateSingleResource(user, request, new JsonArray().add(application), new JsonArray().add(resourceId), config,
              new Handler<Either<String, String>>()
              {
                @Override
                public void handle(Either<String, String> event)
                {
                  if (event.isRight())
                  {
                    renderJson(request,
                        new JsonObject().put("message", "duplicate.in.progress")
                            .put("duplicateId", event.right().getValue()));
                  }
                  else
                  {
                    badRequest(request, event.left().getValue());
                  }
                }
              });
          });
        }
      }
    });
  }

  @BusAddress("entcore.duplicate")
  public void export(Message<JsonObject> message)
  {
    String action = message.body().getString("action", "");
    switch (action)
    {
      case "exported" :
        this.dupService.exported(
            message.body().getString("exportId"),
            message.body().getString("status"),
            message.body().getString("locale", "fr"),
            message.body().getString("host", config.getString("host", ""))
        );
        break;
        case "imported" :
            this.dupService.imported(
              message.body().getString("importId"),
              message.body().getString("app"),
              message.body().getJsonObject("rapport")
            );
            break;
      default: log.error("Duplication : invalid action " + action);
    }
  }
}
