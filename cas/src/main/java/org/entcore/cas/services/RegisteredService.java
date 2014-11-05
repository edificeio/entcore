package org.entcore.cas.services;

import java.util.Map;

import org.vertx.java.core.eventbus.EventBus;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.User;

public interface RegisteredService {

	public void configure(EventBus eb, Map<String, Object> configuration);
	public boolean matches(String serviceUri);
	public void getUser(String userId, Handler<User> userHandler);
}
