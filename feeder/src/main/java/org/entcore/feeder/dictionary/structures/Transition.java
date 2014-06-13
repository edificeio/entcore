/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.dictionary.structures;

import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.utils.ResultMessage;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class Transition {

	private static final Logger log = LoggerFactory.getLogger(Transition.class);

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
						JsonArray r = m.body().getArray("result", new JsonArray());
						groupsUsers.addAll(r.toList());
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
						tx.rollback();
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
					JsonArray r = m.body().getArray("result", new JsonArray());
					groupsUsers.addAll(r.toList());
					try {
						tx.commit(new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if ("ok".equals(event.body().getString("status"))) {
									if (handler != null) {
										handler.handle(new ResultMessage()
												.put("result", new JsonArray(groupsUsers.toArray())));
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
					tx.rollback();
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
			tx = TransactionManager.getInstance().begin("GraphDataUpdate");
		} catch (TransactionException e) {
			log.error(e.getMessage(), e);
			if (handler != null) {
				handler.handle(new ResultMessage().error(e.getMessage()));
			}
		}
		return tx;
	}

}
