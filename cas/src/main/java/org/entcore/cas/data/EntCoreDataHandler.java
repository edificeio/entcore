/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.cas.data;

import java.io.IOException;
import java.util.UUID;

import fr.wseduc.cas.async.Tuple;
import fr.wseduc.cas.entities.ServiceTicket;
import fr.wseduc.cas.exceptions.ValidationException;
import org.entcore.cas.http.WrappedRequest;
import org.entcore.cas.services.RegisteredServices;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.data.DataHandler;
import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.User;
import fr.wseduc.cas.exceptions.AuthenticationException;
import fr.wseduc.cas.exceptions.Try;
import fr.wseduc.cas.http.Request;
import fr.wseduc.mongodb.MongoDb;

public class EntCoreDataHandler extends DataHandler {

	public static final String COLLECTION = "authcas";
	private final MongoDb mongoDb = MongoDb.getInstance();
	private final EventBus eb;
	private final ObjectMapper mapper = new ObjectMapper();
	private RegisteredServices services;

	private static final Logger log = LoggerFactory.getLogger(EntCoreDataHandler.class);

	public EntCoreDataHandler(Request request, EventBus eb) {
		super(request);
		this.eb = eb;
	}

	@Override
	public void validateService(String service, Handler<Boolean> handler) {
		handler.handle(services.matches(service) != null);
	}

	@Override
	public void authenticateUser(String s, String s2, AuthCas authCas,
			Handler<Try<AuthenticationException, AuthCas>> tryHandler) {
		tryHandler.handle(new Try<AuthenticationException, AuthCas>(
				new AuthenticationException("invalid.authentication.method")));
	}

	@Override
	protected void validateService(AuthCas authCas, ServiceTicket st, String service,
			Handler<Try<ValidationException, Tuple<AuthCas, User>>> handler) {
		super.validateService(authCas, st, services.formatService(service, st), handler);
	}

	@Override
	protected void getAuthByProxyGrantingTicket(String pgt, Handler<AuthCas> handler) {
		JsonObject query = new JsonObject()
				.put("serviceTickets.pgt.pgtId", pgt);
		getAuth(handler, query);
	}

	@Override
	protected void getUser(final String userId, final String service, final Handler<User> userHandler) {
		services.getUser(userId, service, userHandler);
	}

	@Override
	protected void getAuth(String ticket, final Handler<AuthCas> handler) {
		JsonObject query = new JsonObject()
				.put("serviceTickets.ticket", ticket);
		getAuth(handler, query);
	}

	private void getAuth(final Handler<AuthCas> handler, JsonObject query) {
		JsonObject keys = new JsonObject()
				.put("_id", 0)
				.put("id", 1)
				.put("serviceTickets", 1)
				.put("user", 1);
		mongoDb.findOne(COLLECTION, query, keys, new io.vertx.core.Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject res = event.body().getJsonObject("result");
				if ("ok".equals(event.body().getString("status")) && res != null) {
					handler.handle(deserialize(res));
				} else {
					handler.handle(null);
				}

			}
		});
	}

	@Override
	protected void getAuthByProxyTicket(String ticket, final Handler<AuthCas> handler) {
		JsonObject query = new JsonObject()
				.put("serviceTickets.pgt.proxyTickets.pgId", ticket);
		getAuth(handler, query);
	}

	@Override
	public void getOrCreateAuth(Request request, final Handler<AuthCas> handler) {
		UserUtils.getUserInfos(eb, ((WrappedRequest)request).getServerRequest(),
				new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos userInfos) {
				AuthCas authCas = new AuthCas();
				authCas.setId(UUID.randomUUID().toString());
				if (userInfos != null) {
					authCas.setUser(userInfos.getUserId());
				}
				handler.handle(authCas);
			}
		});
	}

	@Override
	public void persistAuth(AuthCas authCas, final Handler<Boolean> handler) {
		JsonObject query = new JsonObject().put("id", authCas.getId());
		JsonObject doc = serialize(authCas);
		if (doc == null) {
			handler.handle(false);
			return;
		}
		doc.put("updatedAt", MongoDb.now());
		mongoDb.update(COLLECTION, query, doc, true, false, new io.vertx.core.Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle("ok".equals(event.body().getString("status")));
			}
		});
	}

	@Override
	public void getAndDestroyAuth(Request request, final Handler<AuthCas> handler) {
		UserUtils.getUserInfos(eb, ((WrappedRequest)request).getServerRequest(),
				new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					final JsonObject query = new JsonObject().put("user", user.getUserId());
					getAuth(new Handler<AuthCas>() {
						@Override
						public void handle(AuthCas authCas) {
							mongoDb.delete(COLLECTION, query);
							handler.handle(authCas);
						}
					}, query);
				} else {
					handler.handle(null);
				}
			}
		});
	}

	@Override
	public void getAndDestroyAuth(String user, final Handler<AuthCas> handler) {
		final JsonObject query = new JsonObject().put("user", user);
		getAuth(new Handler<AuthCas>() {
			@Override
			public void handle(AuthCas authCas) {
				mongoDb.delete(COLLECTION, query);
				handler.handle(authCas);
			}
		}, query);
	}

	private JsonObject serialize(AuthCas authCas) {
		if (authCas == null) {
			return null;
		}
		try {
			return new JsonObject(mapper.writeValueAsString(authCas));
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	private AuthCas deserialize(JsonObject res) {
		if (res == null) {
			return null;
		}
		res.remove("updatedAt");
		try {
			return mapper.readValue(res.encode(), AuthCas.class);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	public void setServices(RegisteredServices services) {
		this.services = services;
	}

}
