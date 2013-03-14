/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core;

import edu.one.core.infra.Controller;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

/**
 *
 * @author rafik
 */
public class History extends Controller implements Handler<Message<String>> {

	@Override
	public void handle(Message<String> event) {
		log.info(event.body);
	}

}
