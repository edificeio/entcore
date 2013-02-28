package edu.one.core;

import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class Admin extends Verticle {

	private Logger log;

	
	@Override
	public void start() throws Exception {
		log = container.getLogger();
		log.info(container.getConfig().getString("test"));
	}

}