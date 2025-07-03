package org.entcore.directory.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.entcore.broker.api.dto.loadtest.LoadTestRequestDTO;
import org.entcore.broker.api.dto.loadtest.LoadTestResponseDTO;
import org.entcore.broker.proxy.LoadTestProxy;

import java.util.Arrays;

public class LoadTestProxyImpl implements LoadTestProxy {

  private final Vertx vertx;

  public LoadTestProxyImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Future<LoadTestResponseDTO> loadTest(LoadTestRequestDTO request) {
    final Promise<LoadTestResponseDTO> promise = Promise.promise();
    final long delay = request.getDelay();
    if(delay > 0) {
      final long startTime = System.currentTimeMillis();
      vertx.setTimer(delay, id -> {
        final long startProcessingTime = System.currentTimeMillis();
        // Generate a payload of the requested size
        final char[] payloadChars = new char[(int) request.getResponseSize()];
        Arrays.fill(payloadChars, 'x');
        final LoadTestResponseDTO response = new LoadTestResponseDTO(new String(payloadChars), startTime, startProcessingTime, System.currentTimeMillis() - startTime);
        promise.complete(response);
      });
    } else {
      // We will never reply
    }
    return promise.future();
  }
}
