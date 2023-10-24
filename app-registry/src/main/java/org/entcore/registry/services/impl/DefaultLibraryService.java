package org.entcore.registry.services.impl;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.pdf.Pdf;
import org.entcore.common.pdf.PdfFactory;
import org.entcore.common.pdf.PdfGenerator;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import org.entcore.registry.services.LibraryService;

import java.util.Optional;
import java.util.UUID;


public class DefaultLibraryService implements LibraryService {
    public enum REASON {BAD_CONFIGURATION, LIBRARY, OK};
    public enum MESSAGE {API_URL_NOT_SET, DISABLED, WRONG_TOKEN, LIBRARY_KO, CONTENT_TOO_LARGE, OK};
    private static final String RESSOURCE_ENDPOINT = "api/documents";
    private static final String CONFIG_LIBRARY_ENABLED = "library-enabled";
    private static final String CONFIG_LIBRARY_API_URL = "library-api-url";
    private static final String CONFIG_LIBRARY_TOKEN = "library-token";
    private static final Logger log = LoggerFactory.getLogger(DefaultLibraryService.class);
    private final HttpClient http;
    private final JsonObject config;
    private final PdfGenerator pdfGenerator;
    private final EventBus eb;
    private final Storage storage;

    public DefaultLibraryService(Vertx vertx, JsonObject config) throws Exception {
        this.config = config;
        this.http = vertx.createHttpClient(new HttpClientOptions()
                                .setKeepAlive(config.getBoolean("library-keepalive", true))
                                .setMaxPoolSize(config.getInteger("library-pool", 16))
                                .setConnectTimeout(config.getInteger("library-timeout", 45000)));
        this.pdfGenerator = new PdfFactory(vertx, config).getPdfGenerator();
        this.eb = vertx.eventBus();
        this.storage = new StorageFactory(vertx, config).getStorage();
    }

    private Future<Buffer> getArchive(UserInfos user, String locale, String app, String resourceId){
        Promise<JsonObject> archiveInfo = Promise.promise();
        JsonObject message = new JsonObject()
                .put("action", "start")
                .put("userId", user.getUserId())
                .put("locale", locale)
                .put("apps", new JsonArray().add(app.toLowerCase()))
                .put("resourcesIds",new JsonArray().add(resourceId))
                .put("force", true)
                .put("synchroniseReply",true);
        log.info("[getArchive] Start archinving ressource: " + resourceId);
        eb.request("entcore.export", message, new DeliveryOptions().setSendTimeout(30000l), response -> {
            if (response.succeeded()) {
                JsonObject body = (JsonObject) response.result().body();
                if ("ok".equals(body.getString("status"))){
                    log.debug("archive.export.start " + body.getString("exportPath"));
                    archiveInfo.complete(new JsonObject()
                            .put("exportId",body.getString("exportId"))
                            .put("exportPath",body.getString("exportPath")));
                    log.info("[getArchive] End archinving ressource: " + resourceId);
                } else {
                    log.error("[getArchive] Archive generation status nok for ressource: "+resourceId);
                    archiveInfo.fail("archive.export.start failed");
                }
            } else {
                log.error("[getArchive] Archive generation failed for ressource: "+resourceId);
                archiveInfo.fail(response.cause());
            }
        });
        return archiveInfo.future().compose(jo -> {
            log.debug("archive.export storage.readFile " + jo.getString("exportPath"));
            Promise<Buffer> archive = Promise.promise();
            log.info("[getArchive] Start reading archive for ressource: " + resourceId);
            this.storage.readFile(jo.getString("exportPath"), archive::complete);
            return archive.future();
        }).compose(buffer -> {
            Promise<Buffer> bufferFuture = Promise.promise();
            log.info("[getArchive] Start deleting archive for ressource: " + resourceId);
            eb.request("entcore.export", new JsonObject()
                    .put("action", "delete")
                    .put("exportId", archiveInfo.future().result().getString("exportId")), new DeliveryOptions().setSendTimeout(5000l), response -> {
                if (response.succeeded()) {
                    JsonObject body = (JsonObject) response.result().body();
                    if ("ok".equals(body.getString("status"))){
                        log.info("[getArchive] End deleting archive for ressource: " + resourceId);
                        log.debug("archive.export.delete " + archiveInfo.future().result().getString("exportPath"));
                        bufferFuture.complete(buffer);
                    } else {
                        bufferFuture.fail("archive.export.delete failed");
                        log.error("[getArchive] Failed deleting archive for ressource: " + resourceId+" - "+body.getString("message"));
                    }
                } else {
                    bufferFuture.fail(response.cause());
                    log.error("[getArchive] Failed deleting archive for ressource: " + resourceId, response.cause());
                }
            });
            return  bufferFuture.future();
        });
    }

    @Override
    public Future<JsonObject> publish(UserInfos user, String locale, MultiMap form, Buffer cover, Buffer teacherAvatar) {
        log.info("[publish] Start publishing ressource: "+form.get("resourceId"));
        final boolean isLibraryEnabled = config.getBoolean(CONFIG_LIBRARY_ENABLED, false);
        final String libraryApiUrl = config.getString(CONFIG_LIBRARY_API_URL);
        final String libraryToken = config.getString(CONFIG_LIBRARY_TOKEN);

        if (!isLibraryEnabled) {
            return Future.succeededFuture(generateJsonResponse(false, REASON.BAD_CONFIGURATION, MESSAGE.DISABLED));
        }
        if (!(libraryApiUrl != null && libraryApiUrl.length() > 0)) {
            return Future.succeededFuture(generateJsonResponse(false, REASON.BAD_CONFIGURATION, MESSAGE.API_URL_NOT_SET));
        }
        if (!(libraryToken != null && libraryToken.length() > 0)) {
            return Future.succeededFuture(generateJsonResponse(false, REASON.BAD_CONFIGURATION, MESSAGE.WRONG_TOKEN));
        }
        Future<Buffer> archive = getArchive(user, locale, form.get("application"), form.get("resourceId"));
        return archive.compose(resArchive -> generatePdf(user, form)).compose(resPdf -> {
            final Promise<JsonObject> future = Promise.promise();
            try{
                log.info("[publish] Post data to library for ressource: " + form.get("resourceId"));
                final Buffer exportPdf = resPdf.getContent();
                final String boundary = UUID.randomUUID().toString();
                final Buffer multipartBody = createMultipartBody(form, cover, teacherAvatar, exportPdf, archive.result(), boundary);
                http.request(new RequestOptions()
                    .setAbsoluteURI(libraryApiUrl.concat(RESSOURCE_ENDPOINT))
                    .setMethod(HttpMethod.POST))
                .map(req -> req.setChunked(true)
                    .putHeader("Authorization", "Bearer ".concat(libraryToken))
                    .putHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .putHeader("Content-Length", String.valueOf(multipartBody.length())))
                .flatMap(req -> req.send(multipartBody))
                .onSuccess(response -> {
                    log.info("[publish] Post data finished for ressource: " + form.get("resourceId")+ " - with code: "+response.statusCode());
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        response.bodyHandler(body -> {
                            future.complete(generateJsonResponse(true, REASON.OK, MESSAGE.OK).put("details", new JsonObject(body)));
                        });
                        response.exceptionHandler(e -> {
                            future.fail(e);
                            log.error("[publish] Ressource publication succeed but get exception : " + response.statusCode(), e);
                        });
                    } else {
                        if (response.statusCode() == 401) {
                            future.complete(generateJsonResponse(false, REASON.LIBRARY, MESSAGE.WRONG_TOKEN));
                        } else if (response.statusCode() == 413) {
                            future.complete(generateJsonResponse(false, REASON.LIBRARY, MESSAGE.CONTENT_TOO_LARGE));
                        } else {
                            future.complete(generateJsonResponse(false, REASON.LIBRARY, MESSAGE.LIBRARY_KO));
                        }
                        response.bodyHandler(body -> {
                            log.error("[publish] Ressource publication failed : " + response.statusCode() + " - " + body);
                        });
                    }
                })
                .onFailure(th-> log.error("[publish] Library publish failed handler for: "+form.get("resourceId"), th));
            }catch(Exception e){
                log.error("[publish] An error occured while publishing", e);
                future.fail(e);
            }
            return future.future();
        });
    }


    private JsonObject generateJsonResponse(boolean success, REASON reason, MESSAGE message) {
        return new JsonObject()
                .put("success", success)
                .put("reason", reason.name())
                .put("message", message.name());
    }

    private Future<Pdf> generatePdf(UserInfos user, MultiMap form) {
        log.info("[generatePdf] Start generating pdf for ressource: " + form.get("resourceId"));
        final String pdfUri = form.get("pdfUri");
        form.remove("pdfUri");
        Promise<Pdf> future = Promise.promise();
        if (StringUtils.isEmpty(pdfUri)) {
            future.fail("Pdf URI should not be empty");
            log.error("[generatePdf] Pdf generation failed for URI: " +pdfUri);
        } else {
            this.pdfGenerator.generatePdfFromUrl(user, "pdfExport.pdf", pdfUri, future);
        }
        return future.future().onComplete(res->{
            if(res.failed()){
                log.error("[generatePdf] Pdf generation failed for resource: " +form.get("resourceId"), res.cause());
            }else{
                log.info("[generatePdf] Pdf generation succeed for resource: " +form.get("resourceId"));
            }
        });
    }

    private Buffer createMultipartBody(MultiMap form, Buffer cover, Buffer teacherAvatar, Buffer exportPdf, Buffer archive, String boundary) {
        final Buffer buffer = Buffer.buffer();

        final Optional<String> coverName = Optional.ofNullable(form.get("coverName"));
        final Optional<String> coverType = Optional.ofNullable(form.get("coverType"));
        form.remove("coverName").remove("coverType");

        final Optional<String> teacherAvatarName = Optional.ofNullable(form.get("teacherAvatarName"));
        final Optional<String> teacherAvatarType = Optional.ofNullable(form.get("teacherAvatarType"));
        form.remove("teacherAvatarName").remove("teacherAvatarType");

        form.forEach(entry -> {
            final String attribute = entry.getKey();
            final String value = entry.getValue();
            buffer.appendString("--" + boundary + "\r\n");
            buffer.appendString(String.format("Content-Disposition: form-data; name=\"%s\"", attribute));
            buffer.appendString("\r\n\r\n");
            buffer.appendString(value + "\r\n");
        });

        // cover
        buffer.appendString("--" + boundary + "\r\n");
        buffer.appendString(String.format("Content-Disposition: form-data; name=\"cover\"; filename=\"%s\"\r\n", coverName.orElse("cover.png")));
        buffer.appendString(String.format("Content-Type: %s\r\n", coverType.orElse("image/png")));
        buffer.appendString("\r\n");
        buffer.appendBuffer(cover);
        buffer.appendString("\r\n");
        // teacherAvatar
        buffer.appendString("--" + boundary + "\r\n");
        buffer.appendString(String.format("Content-Disposition: form-data; name=\"teacherAvatar\"; filename=\"%s\"\r\n", teacherAvatarName.orElse("teacherAvatar.png")));
        buffer.appendString(String.format("Content-Type: %s\r\n", teacherAvatarType.orElse("image/png")));
        buffer.appendString("\r\n");
        buffer.appendBuffer(teacherAvatar);
        buffer.appendString("\r\n");
        //TODO put real archive (archive is mandatory in BPR side) and remove documents.zip in ressources
        buffer.appendString("--" + boundary + "\r\n");
        buffer.appendString("Content-Disposition: form-data; name=\"archive\"; filename=\"archive.zip\"\r\n");
        buffer.appendString("Content-Type: application/zip\r\n");
        buffer.appendString("\r\n");
        buffer.appendBuffer(archive);
        buffer.appendString("\r\n");
        //pdf
        buffer.appendString("--" + boundary + "\r\n");
        buffer.appendString("Content-Disposition: form-data; name=\"pdfExport\"; filename=\"pdfExport.pdf\"\r\n");
        buffer.appendString("Content-Type: application/pdf\r\n");
        buffer.appendString("\r\n");
        buffer.appendBuffer(exportPdf);
        buffer.appendString("\r\n");
        buffer.appendString("--" + boundary + "--\r\n");
        return buffer;
    }
}
