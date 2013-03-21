package edu.one.core.tracer;

import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class Tracer extends BusModBase implements Handler<Message<JsonObject>> {
	
	private JsonObject config;
	private Logger tracer;
	private org.vertx.java.core.logging.Logger vertxLogger;
	
	@Override
	public void start() {
		super.start();
		vertxLogger = container.getLogger();
		config = container.getConfig();

		tracer = java.util.logging.Logger.getLogger(config.getString("logger-name"));
		vertx.eventBus().registerHandler(config.getString("address"), this);

		//TODO : get app list form appregistry and create handlers for all
		//TODO (rdje) Apps have to know Tracer. But Tracer must be App agnostic. 
		//            File Handler must be create juste before the first trace of the App
		FileHandler allLogs = createFileHandler("all");
		tracer.addHandler(allLogs);
		
		FileHandler directory = createFileHandler("directory");
		tracer.addHandler(directory);
		
		FileHandler history = createFileHandler("history");
		tracer.addHandler(history);

		FileHandler sync = createFileHandler("sync");	
		tracer.addHandler(sync);

		vertxLogger.info("BusModBase Trace starts on address: " + config.getString("address"));
	}

	@Override
	public void stop() throws Exception {
		super.stop();
	}

	@Override
	public void handle(Message<JsonObject> m) {
		tracer.log(Level.OFF,m.body.getString("message"),(Object)(m.body.getString("app")));
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
