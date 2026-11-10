package org.entcore.infra.listeners;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.dto.antivirus.ScanAntivirusRequestDTO;
import org.entcore.broker.api.dto.antivirus.ScanAntivirusResponseDTO;
import org.entcore.broker.proxy.AntivirusBrokerListener;
import org.entcore.infra.services.AntivirusService;

import java.util.ArrayList;
import java.util.List;

public class AntivirusBrokerListenerImpl implements AntivirusBrokerListener {
    private static final Logger log = LoggerFactory.getLogger(AntivirusBrokerListenerImpl.class);
    private final AntivirusService antivirusService;

    public AntivirusBrokerListenerImpl(AntivirusService service) {
        this.antivirusService = service;
    }

    @Override
    public Future<ScanAntivirusResponseDTO> scan(ScanAntivirusRequestDTO req) {
        if (req == null || req.getIds() == null || req.getIds().isEmpty()) {
            return Future.succeededFuture(
                new ScanAntivirusResponseDTO(
                    "Error: missing ids",
                    0,
                    new ArrayList<>(),
                    0)
            );
        }
        List<String> ids = req.getIds();
        log.info(String.format("Antivirus scan requested: count=%d ids=%s", ids.size(), ids));

        // In our case we will only have one or zero file in each future, but the scan function will return a list anyway.
        final List<Future<List<String>>> futures = new ArrayList<>();
        for (final String id : ids) {
            futures.add(antivirusService.scanWithResult(id));
        }
        return Future.join(futures).map(compositeFuture -> {
            List<String> errors = new ArrayList<>();
            int successCount = 0;
            List<String> rejectedIds = new ArrayList<>();
            for (int i = 0; i < compositeFuture.size(); i++) {
                if (compositeFuture.succeeded(i)) {
                    List<String> res = compositeFuture.resultAt(i);
                    if (!res.isEmpty()) {
                        // There is one infected file max.
                        rejectedIds.add(res.get(0));
                    } else {
                        successCount++;
                    }
                } else {
                    Throwable err = compositeFuture.cause(i);
                    log.error("Antivirus scan failed (non-blocking error)", err);
                    errors.add(err.getMessage());
                }
            }
            final Integer nErrors = errors.size();
            String message = String.format(
                "Successful scans: %d. Rejected scans: %d. Errors: %d.",
                successCount,
                rejectedIds.size(),
                nErrors
            );
            for (String errorMessage : errors) {
                message += "\nError: " + errorMessage;
            }
            log.info(message);
            return new ScanAntivirusResponseDTO(message, successCount, rejectedIds, errors.size());
        });
    }
}
