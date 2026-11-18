package org.entcore.workspace.service.impl;

import fr.wseduc.webutils.collections.SharedDataHelper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.impl.S3Storage;
import org.entcore.common.utils.StringUtils;
import org.entcore.workspace.service.TransferService;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.regex.Pattern;

public class DefaultTransferService implements TransferService {

    private static final Logger log = LoggerFactory.getLogger(DefaultTransferService.class);

    /** Module config key holding the transfer storage settings ({@code prefix-s3}). */
    private static final String TRANSFER_CONFIG_KEY = "transfert-storage";
    /** INE are alphanumeric; anything else could alter the S3 key. */
    private static final Pattern INE_PATTERN = Pattern.compile("^[0-9A-Za-z]{1,20}$");
    private static final String ZIP_CONTENT_TYPE = "application/zip";

    private final Future<S3Storage> storage;
    private final String prefix;

    public DefaultTransferService(Vertx vertx, JsonObject config) {
        final JsonObject transferConfig = config.getJsonObject(TRANSFER_CONFIG_KEY, new JsonObject());
        this.prefix = transferConfig.getString("prefix-s3");
        if (StringUtils.isEmpty(prefix)) {
            log.warn("Missing " + TRANSFER_CONFIG_KEY + ".prefix-s3 config, transfer S3 storage disabled");
            this.storage = Future.failedFuture(new IllegalStateException("Missing " + TRANSFER_CONFIG_KEY + ".prefix-s3 config"));
        } else {
            this.storage = loadEntS3Config(config).compose(s3 -> buildStorage(vertx, s3));
            this.storage.onFailure(e -> log.error("Transfer S3 storage initialization error", e));
        }
    }

    @Override
    public Future<Buffer> getArchive(String ine) {
        return resolveKey(ine).compose(key -> storage.compose(s3 -> {
            final Promise<Buffer> promise = Promise.promise();
            s3.readFile(key, buffer -> {
                if (buffer == null) {
                    promise.fail(new FileNotFoundException("Archive not found : " + key));
                } else {
                    promise.complete(buffer);
                }
            });
            return promise.future();
        }));
    }

    /** Uses sendFile rather than readStreamFile: S3Client.readFileStream drains the body before handing the stream over. */
    @Override
    public Future<Void> sendArchive(String ine, HttpServerRequest request) {
        return resolveKey(ine).compose(key -> storage.compose(s3 -> {
            final Promise<Void> promise = Promise.promise();
            final JsonObject metadata = new JsonObject().put("content-type", ZIP_CONTENT_TYPE);
            s3.sendFile(key, ine + ".zip", request, false, metadata, ar -> promise.complete());
            return promise.future();
        }));
    }

    private Future<String> resolveKey(String ine) {
        if (ine == null || !INE_PATTERN.matcher(ine).matches()) {
            return Future.failedFuture(new IllegalArgumentException("Invalid ine"));
        }
        return Future.succeededFuture(prefix + "/" + ine);
    }

    /** Same resolution as StorageFactory: module "s3" config first, then the ENT shared "server.s3". */
    private static Future<JsonObject> loadEntS3Config(JsonObject config) {
        if (config.getJsonObject("s3") != null) {
            return Future.succeededFuture(config.getJsonObject("s3"));
        }
        return SharedDataHelper.getInstance().<String, String>getLocal("server", "s3").compose(s3 -> s3 == null
                ? Future.failedFuture(new IllegalStateException("Missing ENT s3 storage config"))
                : Future.succeededFuture(new JsonObject(s3)));
    }

    /** flat=true so the key is used as is instead of being split into uuid-based directories. */
    private static Future<S3Storage> buildStorage(Vertx vertx, JsonObject s3) {
        try {
            return Future.succeededFuture(new S3Storage(
                    vertx, new URI(s3.getString("uri")),
                    s3.getString("accessKey"), s3.getString("secretKey"),
                    s3.getString("region"), s3.getString("bucket"),
                    s3.getString("ssec"),
                    s3.getBoolean("keepAlive", false),
                    s3.getInteger("timeout", 10000),
                    s3.getInteger("threshold", 100),
                    s3.getLong("openDelay", 10000L),
                    s3.getInteger("poolSize", 16),
                    true
            ));
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }
}
