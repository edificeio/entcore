/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.module;

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
				.putString("date", Calendar.getInstance().getTime().toString())
				.putString("message", record.getMessage());
		return formatted.toString();
	}
	
}
