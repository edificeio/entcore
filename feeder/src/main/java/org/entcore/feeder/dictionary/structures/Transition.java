/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.feeder.dictionary.structures;

import org.entcore.feeder.Feeder;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.utils.ResultMessage;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Transition {

	private static final Logger log = LoggerFactory.getLogger(Transition.class);
	private static final String GRAPH_DATA_UPDATE = "GraphDataUpdate";

	public void launch(final String structureExternalId, final Handler<Message<JsonObject>> handler) {
		if (GraphData.isReady()) {
			GraphData.loadData(TransactionManager.getInstance().getNeo4j(), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> res) {
					if (!"ok".equals(res.body().getString("status"))) {
						log.error(res.body().getString("message"));
						if (handler != null) {
							handler.handle(res);
						}
					} else if (structureExternalId != null && !structureExternalId.trim().isEmpty()) {
						transitionStructure(structureExternalId, handler);
					} else {
						transitionStructures(handler);
					}
				}
			});
		} else {
			if (handler != null) {
				handler.handle(new ResultMessage().error("Concurrent graph migration"));
			}
		}
	}

	private void transitionStructure(final String structureExternalId,
			final Handler<Message<JsonObject>> handler) {
		final TransactionHelper tx = getTransaction(handler);
		if (tx == null) {
			return;
		}
		Structure structure = GraphData.getStructures().get(structureExternalId);
		if (structure == null) {
			log.error("Missing structure with externalId : " + structureExternalId);
			if (handler != null) {
				handler.handle(new ResultMessage().error(
						"Missing structure with externalId : " + structureExternalId));
			}
			return;
		}
		structure.transition(commitHandler(handler, tx, new HashSet<>()));
	}

	private void transitionStructures(final Handler<Message<JsonObject>> handler) {
		final TransactionHelper tx = getTransaction(handler);
		if (tx == null) {
			return;
		}
		final Set<Object> groupsUsers = new HashSet<>();
		Set<String> s = GraphData.getStructures().keySet();
		final String [] structuresExternalId = s.toArray(new String[s.size()]);
		final Handler[] handlers = new Handler[structuresExternalId.length + 1];
		handlers[handlers.length -1] = commitHandler(handler, tx, groupsUsers);

		for (int i = structuresExternalId.length - 1; i >= 0; i--) {
			final int j = i;
			handlers[i] = new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					if ("ok".equals(m.body().getString("status"))) {
						JsonArray r = m.body().getJsonArray("result", new JsonArray());
						groupsUsers.addAll(r.getList());
						Structure s = GraphData.getStructures().get(structuresExternalId[j]);
						if (s == null) {
							log.error("Missing structure with externalId : " +
									structuresExternalId[j]);
							if (handler != null) {
								handler.handle(new ResultMessage().error(
										"Missing structure with externalId : " +
												structuresExternalId[j]));
							}
							return;
						}
						s.transition(handlers[j+1]);
					} else {
						TransactionManager.getInstance().rollback(GRAPH_DATA_UPDATE);
						log.error("Transition error");
						log.error(m.body().encode());
						if (handler != null) {
							handler.handle(m);
						}
					}
				}
			};
		}
		handlers[0].handle(new ResultMessage().put("result", new JsonArray()));
	}

	private Handler<Message<JsonObject>> commitHandler(final Handler<Message<JsonObject>> handler,
			final TransactionHelper tx, final Set<Object> groupsUsers) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				if ("ok".equals(m.body().getString("status"))) {
					JsonArray r = m.body().getJsonArray("result", new JsonArray());
					groupsUsers.addAll(r.getList());
					try {
						TransactionManager.getInstance().persist(GRAPH_DATA_UPDATE, false,
								new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if ("ok".equals(event.body().getString("status"))) {
									if (handler != null) {
										handler.handle(new ResultMessage()
												.put("result", new JsonArray(new ArrayList<>(groupsUsers))));
									}
								} else {
									log.error("Transition commit error");
									log.error(event.body().encode());
									if (handler != null) {
										handler.handle(event);
									}
								}
							}
						});
					} catch (Exception e) {
						log.error("Transition commit error");
						log.error(e.getMessage(), e);
						if (handler != null) {
							handler.handle(new ResultMessage().error(e.getMessage()));
						}
					}
				} else {
					TransactionManager.getInstance().rollback(GRAPH_DATA_UPDATE);
					log.error("Transition error");
					log.error(m.body().encode());
					if (handler != null) {
						handler.handle(m);
					}
				}
			}
		};
	}

	private TransactionHelper getTransaction(Handler<Message<JsonObject>> handler) {
		TransactionHelper tx = null;
		try {
			tx = TransactionManager.getInstance().begin(GRAPH_DATA_UPDATE);
		} catch (TransactionException e) {
			log.error(e.getMessage(), e);
			if (handler != null) {
				handler.handle(new ResultMessage().error(e.getMessage()));
			}
		}
		return tx;
	}


	public static void publishDeleteGroups(EventBus eb, Logger logger, JsonArray groups) {
		logger.info("Delete groups : " + groups.encode());
		eb.publish(Feeder.USER_REPOSITORY, new JsonObject()
				.put("action", "delete-groups")
				.put("old-groups", groups));
	}

}
