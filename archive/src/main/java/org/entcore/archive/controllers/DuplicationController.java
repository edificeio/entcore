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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

import org.entcore.archive.services.DuplicationService;
import org.entcore.archive.services.impl.DefaultDuplicationService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

public class DuplicationController extends BaseController
{
  private EventBus            eb;
  private DuplicationService  dupService;
  private Storage             storage;
  private String              importPath;
  private PrivateKey          signKey;
  private PublicKey           verifyKey;
  private boolean             forceEncryption;

  public DuplicationController(Vertx vertx, Storage storage, String importPath, PrivateKey signKey, PublicKey verifyKey, boolean forceEncryption)
  {
    this.eb = vertx.eventBus();
    this.storage = storage;
    this.importPath = importPath;
    this.signKey = signKey;
    this.verifyKey = verifyKey;
    this.forceEncryption = forceEncryption;
  }

	@Override
	public void init(Vertx vertx, final JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions)
	{
    super.init(vertx, config, rm, securedActions);

    this.dupService = new DefaultDuplicationService(vertx, config, storage, importPath, signKey, verifyKey, forceEncryption);
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

        String application = null;
        String resourceId = null;

        try
        {
          body = buff.toJsonObject();
          application = body.getString("application");
          resourceId = body.getString("resourceId");
        }
        catch(Exception e)
        {
          log.error(e, e.getMessage());
          badRequest(request);
          return;
        }

        if (application == null || resourceId == null)
          badRequest(request);
        else
        {
          final String fapplication = application;
          final String fresourceId = resourceId;
          UserUtils.getUserInfos(eb, request, user ->
          {
            dupService.duplicateSingleResource(user, request, new JsonArray().add(fapplication), new JsonArray().add(fresourceId), config,
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
