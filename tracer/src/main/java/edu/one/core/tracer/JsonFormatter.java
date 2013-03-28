package edu.one.core.tracer;

import java.util.Calendar;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author dwak
 */
public class JsonFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		JsonObject formatted = new JsonObject()
				.putString("level", record.getLevel().toString())
				.putString("date", Calendar.getInstance().getTime().toString())
				.putString("app", record.getParameters()[0].toString())
				.putString("message", record.getMessage());
		return formatted.toString() + ",\n";
	}

}
