/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 *
 * @author rafik
 */
public class History extends Verticle implements Handler<Message<String>> {
	private Logger log;

	@Override
	public void start() throws Exception {
		log = container.getLogger();
		log.info(container.getConfig().getString("test"));
	}

	@Override
	public void handle(Message<String> event) {
		log.info(event.body);
	}

}
