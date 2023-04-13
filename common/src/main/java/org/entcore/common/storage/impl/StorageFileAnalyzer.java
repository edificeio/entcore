package org.entcore.common.storage.impl;

import fr.wseduc.webutils.security.XSSUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.messaging.AppMessageProcessor;
import org.entcore.common.messaging.to.UploadedFileMessage;
import org.entcore.common.storage.FileAnalyzer;
import org.entcore.common.storage.Storage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Analyze files to remove XSS content.
 */
public class StorageFileAnalyzer implements AppMessageProcessor<UploadedFileMessage> {
    public static final Logger logger = LoggerFactory.getLogger(StorageFileAnalyzer.class);
    private final Vertx vertx;
    private final Storage storage;
    private final List<Pattern> handledMimeTypes;

    public StorageFileAnalyzer(final Vertx vertx, final Storage storage) {
        this.vertx = vertx;
        this.storage = storage;
        this.handledMimeTypes = new ArrayList<>();
        handledMimeTypes.add(Pattern.compile("text/.*"));
        handledMimeTypes.add(Pattern.compile("application/.*script"));
        handledMimeTypes.add(Pattern.compile("application/json"));
        handledMimeTypes.add(Pattern.compile(".*xml"));
    }

    @Override
    public Future apply(final UploadedFileMessage uploadedFileMessage) {
        final Future<FileAnalyzer.Report> report;
        if(shouldFilterFile(uploadedFileMessage)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Reading file " + uploadedFileMessage.getFilename());
            }
            report =  this.storage.readFileToMemory(uploadedFileMessage).compose(fileContent -> {
                logger.debug("File " + uploadedFileMessage.getFilename() + " read");
                return xssFilter(uploadedFileMessage, fileContent);
            }).onFailure(th -> logger.error("An error occurred while reading file " + Json.encodePrettily(uploadedFileMessage)));
        } else {
            report = Future.succeededFuture(new FileAnalyzer.Report(true, Collections.singletonList("mimeType.bypass")));
        }
        return report;
    }

    private boolean shouldFilterFile(final UploadedFileMessage uploadedFileMessage) {
        final String contentType = uploadedFileMessage.getContentType();
        return handledMimeTypes.stream().anyMatch(type -> type.matcher(contentType).matches());
    }

    private Future<FileAnalyzer.Report> xssFilter(final UploadedFileMessage uploadedFileMessage, final byte[] fileContent) {
        final Promise<FileAnalyzer.Report> report = Promise.promise();
        final String stripped = XSSUtils.stripXSS(new String(fileContent, StandardCharsets.UTF_8));
        if(stripped.length() == fileContent.length) {
            logger.debug("The file " + uploadedFileMessage.getId() + " was safe");
            report.complete(new FileAnalyzer.Report(true, Collections.singletonList("xss.filtered.ok")));
        } else {
            logger.warn("The file " + uploadedFileMessage.getId() + " has been filtered so we will reupload it");
            final List<String> actions = new ArrayList<>();
            actions.add("xss.filtered.strip");
            replaceFileWithContent(uploadedFileMessage, stripped).onComplete(replaceResult -> {
                final boolean succeeded = replaceResult.failed();
                actions.add(succeeded ? "file.replaced" : "file.replacing.error");
                report.complete(new FileAnalyzer.Report(true, actions));
            });
        }
        return report.future();
    }

    private Future<Void> replaceFileWithContent(final UploadedFileMessage uploadedFileMessage, final String stripped) {
        final Buffer src = Buffer.buffer(stripped);
        final Promise<Void> promise = Promise.promise();
        storage.writeBuffer(uploadedFileMessage.getId(), src, uploadedFileMessage.getContentType(), uploadedFileMessage.getFilename(), e -> {
            if("ok".equals(e.getString("status"))) {
                promise.complete(null);
            } else {
                promise.fail(e.getString("message"));
            }
        });
        return promise.future();
    }

    @Override
    public Class<UploadedFileMessage> getHandledMessageClass() {
        return UploadedFileMessage.class;
    }
}
