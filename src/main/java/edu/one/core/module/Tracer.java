package edu.one.core.module;

import java.io.IOException;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author rafik
 */
public class Tracer extends BusModBase implements Handler<Message<JsonObject>> {

	@Override
	public void start() {
		super.start();
		JsonObject config = container.getConfig();
		
		vertx.eventBus().registerHandler(config.getString("log-address"), this);
		Logger tracer = java.util.logging.Logger.getLogger(config.getString("logger"));
		Formatter formatter = new JsonFormatter();
		FileHandler handler = null;
			try {
				handler = new FileHandler(config.getString("log-path"), true);
			} catch (IOException | SecurityException ex) {
				//Logger.getLogger(Tracer.class.getName()).log(Level.SEVERE, null, ex);
			}
		handler.setFormatter(formatter);
		tracer.addHandler(handler);
	}

	@Override
	public void stop() throws Exception {
		super.stop();
	}

	@Override
	public void handle(Message<JsonObject> m) {
		Logger.getLogger(config.getString("logger")).log(Level.OFF,m.body.getString("message"));
	}
}