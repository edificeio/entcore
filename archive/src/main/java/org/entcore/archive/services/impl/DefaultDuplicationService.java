package org.entcore.archive.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;

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

import org.entcore.common.storage.Storage;
import org.entcore.archive.services.DuplicationService;
import org.entcore.archive.services.ExportService;
import org.entcore.archive.services.ImportService;
import org.entcore.common.user.UserInfos;

import java.util.Map;

public class DefaultDuplicationService implements DuplicationService
{
    private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);

    private final EventBus eb;
    private final FileSystem fs;

    private final ExportService exportService;
    private final ImportService importService;

    public DefaultDuplicationService(Vertx vertx, Storage storage, String importPath)
    {
      this.eb = vertx.eventBus();
      this.fs = vertx.fileSystem();

      String tmpDir = System.getProperty("java.io.tmpdir");
      this.exportService = new FileSystemExportService(vertx, vertx.fileSystem(), vertx.eventBus(), tmpDir, "duplicate:export", null, storage, null, null);
      this.importService = new DefaultImportService(vertx, storage, importPath, "duplicate:import");
    }

    @Override
    public void duplicateSingleResource(UserInfos user, HttpServerRequest request, JsonArray apps, JsonArray resourcesIds,
      JsonObject config, Handler<Either<String, String>> handler)
    {
      String locale = I18n.acceptLanguage(request);

      exportService.export(user, locale, apps, resourcesIds, request, new Handler<Either<String, String>>()
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
                            String importPath = importData.getString("path");
                            JsonObject importApps = importData.getJsonObject("apps");

                            importService.launchImport(user.getUserId(), user.getLogin(), user.getUsername(), importId, importPath,
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
