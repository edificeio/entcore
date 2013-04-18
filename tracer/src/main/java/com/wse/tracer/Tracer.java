package com.wse.tracer;

import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class Tracer extends BusModBase implements Handler<Message<JsonObject>> {

	private Logger tracer;
	private org.vertx.java.core.logging.Logger vertxLogger;

	@Override
	public void start() {
		super.start();
		vertxLogger = container.logger();

		tracer = java.util.logging.Logger.getLogger(config.getString("logger-name"));
		vertx.eventBus().registerHandler(config.getString("address"), this);

		FileHandler allLogs = createFileHandler("all");
		tracer.addHandler(allLogs);

		vertxLogger.info("BusModBase Trace starts on address: " + config.getString("address"));
	}

	@Override
	public void handle(Message<JsonObject> m) {
		String appName = m.body().getString("app");
		if (handlerExists(appName)){
			tracer.log(Level.parse(m.body().getString("level")),m.body().getString("message"),(Object)(appName));
		} else {
			tracer.addHandler(createFileHandler(appName));
			tracer.log(Level.parse(m.body().getString("level")),m.body().getString("message"),(Object)(appName));
		}
	}

	private boolean handlerExists(String appName){
		boolean exists = false;
		for (java.util.logging.Handler h : tracer.getHandlers()) {
			if (h.getFilter() != null){
				ApplicationLogFilter f = (ApplicationLogFilter) h.getFilter();
				if (f.toString().equals(appName)){
					exists = true;
				}
			}
		}
		return exists;
	}

	public FileHandler createFileHandler(String name){
		FileHandler fh = null;
		try {
			fh = new FileHandler(config.getString("log-path") + name + ".trace", true);
		} catch (Exception ex) {
			vertxLogger.info(ex);
		}
		Formatter formatter = new JsonFormatter();
		fh.setFormatter(formatter);
		if (!name.equals("all")){
			fh.setFilter(new ApplicationLogFilter(name));
		}
		return fh;
	}
}
