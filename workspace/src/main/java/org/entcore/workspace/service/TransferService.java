package org.entcore.workspace.service;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

public interface TransferService {

    /**
     * Read the zip archive stored on S3 under the key {@code <prefix>/<ine>}.
     *
     * @param ine student national identifier
     * @return the zip content, failed with {@link java.io.FileNotFoundException} if the archive does not exist
     */
    Future<Buffer> getArchive(String ine);

    /**
     * Stream the zip archive {@code <prefix>/<ine>} as an attachment into the request response.
     * Fails without touching the response if the ine or the configuration is invalid; S3 errors (404...) are forwarded as is.
     *
     * @param ine student national identifier
     * @param request request whose response receives the archive
     */
    Future<Void> sendArchive(String ine, HttpServerRequest request);
}
