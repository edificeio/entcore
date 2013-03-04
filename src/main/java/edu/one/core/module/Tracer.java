package edu.one.core.module;

import java.io.IOException;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
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
		vertx.eventBus().registerHandler("one.address", this);
		Logger tracer = java.util.logging.Logger.getLogger("one.tracer");
		Formatter formatter = new SimpleFormatter();
		FileHandler handler = null;
			try {
				handler = new FileHandler("./data/dev/trace", true);
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
		Logger.getLogger("one.tracer").log(Level.OFF,jsonFormatter(m.body).toString());
	}

	public JsonObject jsonFormatter(JsonObject message){
		JsonObject log = new JsonObject()
				.putString("app", message.getField("app").toString())
				.putString("date", Calendar.getInstance().getTime().toString())
				.putString("message", message.getField("action").toString());
		return log;
	}

}