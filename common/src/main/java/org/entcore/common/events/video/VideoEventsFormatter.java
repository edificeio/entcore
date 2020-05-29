package org.entcore.common.events.video;

import io.vertx.core.impl.Utils;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class VideoEventsFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        StringBuilder builder = new StringBuilder();
        builder.append(dateTimeFormatter.format(new Date(record.getMillis()).toInstant()));
        builder.append(",");
        builder.append(record.getMessage());
        builder.append(Utils.LINE_SEPARATOR);
        return builder.toString();
    }
}