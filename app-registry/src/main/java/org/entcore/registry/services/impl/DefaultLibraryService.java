package org.entcore.registry.services.impl;

import fr.wseduc.webutils.data.FileResolver;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.pdf.Pdf;
import org.entcore.common.pdf.PdfFactory;
import org.entcore.common.pdf.PdfGenerator;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import org.entcore.registry.services.LibraryService;

import java.util.Optional;
import java.util.UUID;


enum REASON {BAD_CONFIGURATION, LIBRARY, OK}

enum MESSAGE {API_URL_NOT_SET, DISABLED, WRONG_TOKEN, LIBRARY_KO, OK}

public class DefaultLibraryService implements LibraryService {
    private static final String RESSOURCE_ENDPOINT = "api/documents";
    private static final String CONFIG_LIBRARY_ENABLED = "library-enabled";
    private static final String CONFIG_LIBRARY_API_URL = "library-api-url";
    private static final String CONFIG_LIBRARY_TOKEN = "library-token";
    private static final Logger log = LoggerFactory.getLogger(DefaultLibraryService.class);
    private final HttpClient http;
    private final JsonObject config;
    private final PdfGenerator pdfGenerator;
    private final FileSystem fileSystem;

    public DefaultLibraryService(Vertx vertx, JsonObject config) throws Exception {
        this.config = config;
        this.fileSystem = vertx.fileSystem();
        this.http = vertx.createHttpClient();
        this.pdfGenerator = new PdfFactory(vertx, config).getPdfGenerator();
    }

    private Future<Buffer> getArchive() {
        //TODO call real bus method to generate archive
        Future<Buffer> archive = Future.future();
        this.fileSystem.readFile(FileResolver.absolutePath("document.zip"), archive.completer());
        return archive;
    }

    @Override
    public Future<JsonObject> publish(UserInfos user, MultiMap form, Buffer cover) {
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
        Future<Buffer> archive = getArchive();
        return archive.compose(resArchive -> generatePdf(user, form)).compose(resPdf -> {
            final Buffer exportPdf = resPdf.getContent();
            final Future<JsonObject> future = Future.future();
            final String boundary = UUID.randomUUID().toString();
            final Buffer multipartBody = createMultipartBody(form, cover, exportPdf, archive.result(), boundary);
            HttpClientRequest request = http.postAbs(libraryApiUrl.concat(RESSOURCE_ENDPOINT));
            request.handler(response -> {
                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    response.bodyHandler(body -> {
                        future.complete(generateJsonResponse(true, REASON.OK, MESSAGE.OK).put("details", new JsonObject(body)));
                    });
                    response.exceptionHandler(e -> {
                        future.fail(e);
                        log.error("Ressource publication succeed but get exception : " + response.statusCode(), e);
                    });
                } else {
                    if (response.statusCode() == 401) {
                        future.complete(generateJsonResponse(false, REASON.LIBRARY, MESSAGE.WRONG_TOKEN));
                    } else {
                        future.complete(generateJsonResponse(false, REASON.LIBRARY, MESSAGE.LIBRARY_KO));
                    }
                    response.bodyHandler(body -> {
                        log.error("Ressource publication failed : " + response.statusCode() + " - " + body);
                    });
                }
            });
            request.setChunked(true);
            request.putHeader("Authorization", "Bearer ".concat(libraryToken));
            request.putHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
            request.putHeader("Content-Length", String.valueOf(multipartBody.length()));
            request.end(multipartBody);
            return future;
        });
    }


    private JsonObject generateJsonResponse(boolean success, REASON reason, MESSAGE message) {
        return new JsonObject()
                .put("success", success)
                .put("reason", reason.name())
                .put("message", message.name());
    }

    private Future<Pdf> generatePdf(UserInfos user, MultiMap form) {
        final String pdfUri = form.get("pdfUri");
        form.remove("pdfUri");
        Future<Pdf> future = Future.future();
        if (StringUtils.isEmpty(pdfUri)) {
            future.fail("Pdf URI should not be empty");
        } else {
            this.pdfGenerator.generatePdfFromUrl(user, "pdfExport.pdf", pdfUri, future.completer());
        }
        return future;
    }

    private Buffer createMultipartBody(MultiMap form, Buffer cover, Buffer exportPdf, Buffer archive, String boundary) {
        final Buffer buffer = Buffer.buffer();
        final Optional<String> coverName = Optional.ofNullable(form.get("coverName"));
        final Optional<String> coverType = Optional.ofNullable(form.get("coverType"));
        form.remove("coverName").remove("coverType");
        form.forEach(entry -> {
            final String attribute = entry.getKey();
            final String value = entry.getValue();
            buffer.appendString("--" + boundary + "\r\n");
            buffer.appendString(String.format("Content-Disposition: form-data; name=\"%s\"", attribute));
            buffer.appendString("\r\n\r\n");
            buffer.appendString(value + "\r\n");
        });
        //TODO dynamic name/filetype
        buffer.appendString("--" + boundary + "\r\n");
        buffer.appendString(String.format("Content-Disposition: form-data; name=\"cover\"; filename=\"%s\"\r\n", coverName.orElse("cover.png")));
        buffer.appendString(String.format("Content-Type: %s\r\n", coverType.orElse("image/png")));
        buffer.appendString("\r\n");
        buffer.appendBuffer(cover);
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
