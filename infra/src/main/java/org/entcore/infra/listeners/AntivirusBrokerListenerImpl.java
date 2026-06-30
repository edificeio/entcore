package org.entcore.infra.listeners;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.dto.antivirus.ScanAntivirusRequestDTO;
import org.entcore.broker.api.dto.antivirus.ScanAntivirusResponseDTO;
import org.entcore.broker.proxy.AntivirusBrokerListener;
import org.entcore.common.storage.AntivirusClient;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.broker.api.dto.antivirus.ScanAntivirusResponseDTO.Status.*;

public class AntivirusBrokerListenerImpl implements AntivirusBrokerListener {
  private static final Logger log = LoggerFactory.getLogger(AntivirusBrokerListenerImpl.class);
  private final AntivirusClient antivirusClient;

  public AntivirusBrokerListenerImpl(AntivirusClient client){
    this.antivirusClient = client;
  }

  @Override
  public Future<ScanAntivirusResponseDTO> scan(ScanAntivirusRequestDTO req) {
    if (req == null || req.getIds() == null || req.getIds().isEmpty()) {
      return Future.succeededFuture(
        new ScanAntivirusResponseDTO(REJECTED, "missing ids", 0, new ArrayList<String>()));
    }
    String bucket = req.getBucket();
    List<String> ids = req.getIds();
    ScanAntivirusRequestDTO.Mode mode = req.getMode();

    log.info(String.format("Antivirus scan requested: bucket=%s count=%d ids=%s", bucket, ids.size(), ids));

    final ArrayList<Future<String>> futures = new ArrayList<>();
    for (final String id : ids) {
      final Promise<String> p = Promise.promise();
      futures.add(p.future());
      switch (mode) {
        case S3:
          antivirusClient.scanS3(id, bucket, ar -> {
            if(ar.succeeded()) p.complete(id);
            else p.fail(ar.cause());
          });
          break;
        case LEGACY:
          antivirusClient.scan(id, ar -> {
            if(ar.succeeded()) p.complete(id);
            else p.fail(ar.cause());
          });
          break;
        default:
          p.fail(new IllegalArgumentException("Unsupported mode: " + mode));
      }
    }
    return Future.join(futures).map(compositeFuture -> {
      ScanAntivirusResponseDTO.Status status = ACCEPTED;
      String message = "All scans were successful";
      int successCount = 0;
      ArrayList<String> failedIds = new ArrayList<>(ids);
      for(int i = 0; i < compositeFuture.size(); i++){
        if(compositeFuture.succeeded(i)){
          failedIds.remove(compositeFuture.resultAt(i));
          successCount++;
        } else {
          Throwable err = compositeFuture.cause(i);
          log.error("One file failed the antivirus scan (non-blocking error)", err);
          message = err.getMessage();
          status = PARTIAL;
        }
      }
      if(successCount == 0) status = REJECTED;
      log.info(
        String.format("Antivirus scan finished: succeeded=%d failed=%d status=%s", successCount, failedIds.size(), status)
      );
      return new ScanAntivirusResponseDTO(status, message, successCount, failedIds);
    });
  }
}
