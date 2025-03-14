package org.entcore.archive.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.entcore.common.explorer.ExplorerMessage;
import org.entcore.common.explorer.ExplorerPluginFactory;
import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.explorer.IdAndVersion;
import org.entcore.common.explorer.impl.ExplorerResourceDetails;
import org.entcore.common.explorer.impl.ExplorerResourceDetailsQuery;
import org.entcore.common.storage.Storage;
import org.entcore.archive.services.DuplicationService;
import org.entcore.archive.services.ExportService;
import org.entcore.archive.services.ImportService;
import org.entcore.common.user.UserInfos;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.Optional;

import static io.vertx.core.json.JsonObject.mapFrom;
import static java.lang.System.currentTimeMillis;

public class DefaultDuplicationService implements DuplicationService
{
    private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);

    private final EventBus eb;
    private final FileSystem fs;

    private final ExportService exportService;
    private final ImportService importService;
    private final IExplorerPluginCommunication explorerPluginCommunication;

    public DefaultDuplicationService(Vertx vertx, JsonObject config, Storage storage, String importPath, PrivateKey signKey, PublicKey verifyKey, boolean forceEncryption)
    {
      this.eb = vertx.eventBus();
      this.fs = vertx.fileSystem();

      String tmpDir = System.getProperty("java.io.tmpdir");
      this.exportService = new FileSystemExportService(vertx, vertx.fileSystem(), vertx.eventBus(), tmpDir, "duplicate:export", null,
              storage, null, null, signKey, forceEncryption);
      this.importService = new DefaultImportService(vertx, config, storage, importPath, "duplicate:import", verifyKey, forceEncryption);
      try {
        this.explorerPluginCommunication = ExplorerPluginFactory.getCommunication();
      } catch (Exception e) {
        throw new IllegalStateException("explorer plugin communication could not be started", e);
      }
    }

    @Override
    public void duplicateSingleResource(UserInfos user, HttpServerRequest request, JsonArray apps, JsonArray resourcesIds,
      JsonObject config, Handler<Either<String, String>> handler)
    {
      String locale = I18n.acceptLanguage(request);

      exportService.export(user, locale, apps, resourcesIds, true, true, request, new Handler<Either<String, String>>()
      {
        @Override
        public void handle(Either<String, String> result)
        {
          if(result.isRight() == false)
            handler.handle(result);
          else
          {
            String exportId = result.right().getValue();
            final String address = exportService.getExportBusAddress(exportId);

            final MessageConsumer<JsonObject> consumer = eb.consumer(address);
            consumer.handler(new Handler<Message<JsonObject>>()
            {
              @Override
              public void handle(Message<JsonObject> event)
              {
                event.reply(new JsonObject().put("status", "ok").put("sendNotifications", false));
                consumer.unregister();

                importService.copyArchive(exportId, new Handler<Either<String, String>>()
                {
                  @Override
                  public void handle(Either<String, String> res)
                  {
                    exportService.deleteExport(exportId);
                    if(res.isRight() == true)
                    {
                      String importId = res.right().getValue();
                      importService.analyzeArchive(user, importId, locale, config, new Handler<Either<String, JsonObject>>()
                      {
                        @Override
                        public void handle(Either<String, JsonObject> res)
                        {
                          if(res.isRight() == true)
                          {
                            JsonObject importData = res.right().getValue();

                            String importId = importData.getString("importId");
                            JsonObject importApps = importData.getJsonObject("apps");

                            importService.launchImport(user.getUserId(), user.getLogin(), user.getUsername(), importId,
                              locale, request.headers().get("Host"), importApps);

                            final String address = importService.getImportBusAddress(exportId);

                            final MessageConsumer<JsonObject> consumer = eb.consumer(address);
                            consumer.handler(new Handler<Message<JsonObject>>()
                            {
                              @Override
                              public void handle(Message<JsonObject> event)
                              {
                                JsonObject rapport = event.body().getJsonObject("result");
                                String duplicatedId = "";

                                String mainResourceName = "";
                                JsonObject idsMap = new JsonObject();

                                for(Map.Entry<String, Object> appResult : rapport.getMap().entrySet())
                                {
                                  mainResourceName = ((JsonObject)appResult.getValue()).getString("mainResourceName");
                                  idsMap = ((JsonObject)appResult.getValue()).getJsonObject("resourcesIdsMap");
                                }

                                // Should be Map<String, String> but casting Object to String doesn't work...
                                Map<String, Object> mainIdsMap = idsMap.getJsonObject(mainResourceName).getMap();

                                for(Map.Entry<String, Object> entry : mainIdsMap.entrySet())
                                {
                                  duplicatedId = entry.getValue().toString();
                                  break;
                                }

                                event.reply(new JsonObject().put("status", "ok"));
                                consumer.unregister();

                                // The import service automatically deletes the archive
                                //importService.deleteArchive(importId);
                                handler.handle(new Either.Right<>(duplicatedId));
                                moveDuplicatedResourceToOriginalResourceFolder(resourcesIds, apps, duplicatedId, user);
                              }
                            });
                          }
                          else
                            handler.handle(new Either.Left<>(res.left().getValue()));
                        }
                      });
                    }
                    else
                      handler.handle(res);
                  }
                });
              }
            });
          }
        }
      });
    }

  private void moveDuplicatedResourceToOriginalResourceFolder(final JsonArray resourcesIds, final JsonArray apps,
                                                              final String duplicatedId, final UserInfos userInfos) {
    final String resourceId = resourcesIds.getString(0);
    final String app = apps.getString(0);
    eb.request("explorer.resources.details", mapFrom(new ExplorerResourceDetailsQuery(resourceId, app, userInfos.getUserId())), e -> {
      if(e.succeeded()) {
        final ExplorerResourceDetails details = ((JsonObject) e.result().body()).mapTo(ExplorerResourceDetails.class);
        if(details.getParentId() == null) {
          log.debug("Resource {0}@{1} is already in root folder", duplicatedId, app);
        } else {
          log.debug("Sending a message to move duplicated resource {0}@{1} to right folder", duplicatedId, app);
          final ExplorerMessage message = ExplorerMessage.move(
              new IdAndVersion(duplicatedId, currentTimeMillis()), details.getParentId(),
              app, details.getResourceType(), details.getEntityType(),
            userInfos);
          explorerPluginCommunication.pushMessage(message)
            .onSuccess(ok -> log.debug("Successfully sent a message to move the newly duplicated resource"))
            .onFailure(th -> log.warn("An error occurred while sending a message to move the newly duplicated resource {0}@{1} to right folder", duplicatedId, app, th));
        }
      } else {
        log.error("An error occurred while fetching resource {0}@{1} details", resourceId, app, e.cause());
      }
    });
  }

  @Override
  public void exported(final String exportId, String status, final String locale, final String host)
  {
    this.exportService.exported(exportId, status, locale, host);
  }

  @Override
  public void imported(String importId, String app, JsonObject importRapport)
  {
    this.importService.imported(importId, app, importRapport);
  }
}
