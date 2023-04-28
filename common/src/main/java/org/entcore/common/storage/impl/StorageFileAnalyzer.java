package org.entcore.common.storage.impl;

import fr.wseduc.webutils.security.XSSUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import org.entcore.common.messaging.AppMessageProcessor;
import org.entcore.common.messaging.to.UploadedFileMessage;
import org.entcore.common.storage.FileAnalyzer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageException;
import org.entcore.common.storage.StorageFileAnalyzingException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analyze files to remove XSS content.
 */
public class StorageFileAnalyzer implements AppMessageProcessor<UploadedFileMessage> {
    public static final Logger logger = LoggerFactory.getLogger(StorageFileAnalyzer.class);
    private final Vertx vertx;
    private final Storage storage;
    private final Configuration configuration;

    public StorageFileAnalyzer(final Vertx vertx,
                               final Storage storage) {
        this(vertx, storage, new Configuration());
    }

    public StorageFileAnalyzer(final Vertx vertx,
                               final Storage storage,
                               final Configuration configuration) {
        this.vertx = vertx;
        this.storage = storage;
        this.configuration = configuration;
        if(this.configuration.handledMimeTypes.isEmpty()) {
            throw new StorageFileAnalyzingException("storage.file.analyzer.no.mime.types");
        }
    }

    @Override
    public Future apply(final UploadedFileMessage uploadedFileMessage) {
        final Future<FileAnalyzer.Report> report;
        if(shouldFilterFile(uploadedFileMessage)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Reading file " + uploadedFileMessage.getFilename());
            }
            report =  this.storage.readFileToMemory(uploadedFileMessage)
            .compose(fileContent -> {
                logger.debug("File " + uploadedFileMessage.getId() + " read");
                final Future analyze;
                if(configuration.maxSize > 0 && fileContent.length > configuration.maxSize) {
                    logger.warn("File " + uploadedFileMessage.getId() + " is too large to be analyzed");
                    analyze = Future.succeededFuture();
                } else {
                    analyze = xssFilter(uploadedFileMessage, fileContent);
                }
                return analyze;
            })
            .onFailure(th -> logger.error("An error occurred while reading file " + Json.encodePrettily(uploadedFileMessage)));
        } else {
            report = Future.succeededFuture(new FileAnalyzer.Report(true, Collections.singletonList("mimeType.bypass")));
        }
        return report;
    }

    private boolean shouldFilterFile(final UploadedFileMessage uploadedFileMessage) {
        final String contentType = uploadedFileMessage.getContentType();
        return configuration.handledMimeTypes.stream().anyMatch(type -> type.matcher(contentType).matches());
    }

    private Future<FileAnalyzer.Report> xssFilter(final UploadedFileMessage uploadedFileMessage, final byte[] fileContent) {
        final Promise<FileAnalyzer.Report> report = Promise.promise();
        final String stripped = XSSUtils.stripXSS(new String(fileContent, StandardCharsets.UTF_8));
        if(stripped.getBytes(StandardCharsets.UTF_8).length == fileContent.length) {
            logger.debug("The file " + uploadedFileMessage.getId() + " was safe");
            report.complete(new FileAnalyzer.Report(true, Collections.singletonList("xss.filtered.ok")));
        } else {
            logger.warn("The file " + uploadedFileMessage.getId() + " has been filtered so we will reupload it");
            final List<String> actions = new ArrayList<>();
            actions.add("xss.filtered.strip");
            replaceFileWithContent(uploadedFileMessage, configuration.replacementContent).onComplete(replaceResult -> {
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

    public static class Configuration {
        private final List<Pattern> handledMimeTypes;
        private final long maxSize;
        private final String replacementContent;
        public static final String DEFAULT_CONTENT = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" >\n" +
                "<text text-anchor=\"middle\" font-size=\"10px\" x=\"50%\" y=\"50%\">Contenu remplacé</text>\n" +
                "</svg>";

        public Configuration() {
            this(emptyList(), -1, DEFAULT_CONTENT);
        }
        public Configuration(final List<String> handledMimeTypes, final long maxSize, final String replacementContent) {
            this.maxSize = maxSize;
            this.replacementContent = replacementContent;
            if(handledMimeTypes == null) {
                this.handledMimeTypes = emptyList();
            } else {
                this.handledMimeTypes = handledMimeTypes.stream()
                        .map(type -> Pattern.compile(type))
                        .collect(Collectors.toList());
            }
        }
    }
}
