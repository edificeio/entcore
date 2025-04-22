package org.entcore.broker.listener;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.DummyResponseDTO;
import org.entcore.broker.api.dto.ListenAndAnswerDTO;
import org.entcore.broker.api.dto.ListenOnlyDTO;

public class DummyListener {

  private final Logger log = LoggerFactory.getLogger(DummyListener.class);

  private final Vertx vertx;

  public DummyListener(final Vertx vertx) {
    this.log.info("DummyListener initialized");
    this.vertx = vertx;
  }

  @BrokerListener(subject = "ent.test.listen")
  public void listenOnlyExample(ListenOnlyDTO request) {
    log.info("DummyListener listenOnlyExemple called with request: " + request);
  }

  @BrokerListener(subject = "ent.test.listen.reply")
  public Future<DummyResponseDTO> listenOnlyExample(ListenAndAnswerDTO request) {
    log.info("DummyListener listenOnlyExemple called with request: " + request);
    final Promise<DummyResponseDTO> promise = Promise.promise();
    vertx.setTimer(1000l, e -> {
      promise.tryComplete(new DummyResponseDTO(request.getUserId(), request.getJobId(), true));
    });
    return promise.future();
  }
}
