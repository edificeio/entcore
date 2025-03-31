package org.entcore.broker.listener;

import io.vertx.core.Vertx;
import org.entcore.broker.api.DummyResponse;
import org.entcore.broker.api.ListenAndAnswerDTO;
import org.entcore.broker.api.ListenOnlyDTO;
import org.entcore.broker.nats.NATSListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyListener {

  private final Logger log = LoggerFactory.getLogger(DummyListener.class);

  public DummyListener(final Vertx vertx) {
    this.log.info("DummyListener initialized");
  }

  @NATSListener(subject = "ent.test.listen", queue = "ent")
  public void listenOnlyExample(ListenOnlyDTO request) {
    log.info("DummyListener listenOnlyExemple called with request: {}", request);
  }

  @NATSListener(subject = "ent.test.listen.reply", queue = "ent")
  public DummyResponse listenOnlyExample(ListenAndAnswerDTO request) {
    log.info("DummyListener listenOnlyExemple called with request: {}", request);
    return new DummyResponse(request.getUserId(), request.getJobId(), true);
  }
}
