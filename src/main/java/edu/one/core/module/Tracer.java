package edu.one.core.module;

import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
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
		
		//TODO : get app list form appregistry and create handlers for all
		FileHandler directory = setFileHandler("directory");
		tracer.addHandler(directory);
		
		FileHandler history = setFileHandler("history");
		tracer.addHandler(history);

		FileHandler sync = setFileHandler("sync");	
		tracer.addHandler(sync);
		
	}

	@Override
	public void stop() throws Exception {
		super.stop();
	}

	@Override
	public void handle(Message<JsonObject> m) {
		Logger.getLogger(config.getString("logger")).log(Level.OFF,m.body.getString("message"),(Object)(m.body.getString("appli")));
	}
	
	
	public FileHandler setFileHandler(String name){
		FileHandler fh = null;
		try {
			fh = new FileHandler(config.getString("log-path") + name + ".trace", true);
		} catch (Exception ex) {
			Logger.getLogger(Tracer.class.getName()).log(Level.SEVERE, null, ex);
		}
		Formatter formatter = new JsonFormatter();
		fh.setFormatter(formatter);
		fh.setFilter(new ApplicationLogFilter(name));
		return fh;
	}
}