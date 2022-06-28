package org.entcore.common.storage;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface FallbackStorage {

    void downloadFile(String file, String destination, Handler<AsyncResult<String>> handler);

    void downloadFileIfNotExists(String file, String destination, Handler<AsyncResult<String>> handler);

}
