package org.entcore.common.events.video;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class VideoEventsLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoEventsLogger.class);

    public void info(JsonObject data) {
        List<String> values = new ArrayList();
        data.forEach(pair -> {
            Object value = pair.getValue();
            if (value != null) {
                values.add(value.toString());
            } else {
                values.add("");
            }
        });
        LOGGER.info("," + String.join(",", values));
    }
}
