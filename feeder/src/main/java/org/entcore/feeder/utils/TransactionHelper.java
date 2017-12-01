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

package org.entcore.feeder.utils;

import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionHelper {

	private static final Logger log = LoggerFactory.getLogger(TransactionHelper.class);
	private final Neo4j neo4j;
	private volatile JsonArray statements;
	private AtomicInteger remainingStatementNumber;
	private final int statementNumber;
	private Integer transactionId;
	private Timer resetTimeOutTimer;
	private Message<JsonObject> error;
	private boolean waitingQuery = false;
	private boolean commit = false;
	private Handler<Message<JsonObject>> commitHandler;
	private boolean flush = false;
	private Handler<Message<JsonObject>> flushHandler;
	private boolean autoSend = true;

	class ResetTransactionTimer extends TimerTask {

		@Override
		public void run() {
			Integer tId = getTransactionId();
			if (tId != null) {
				neo4j.resetTransactionTimeout(tId, null);
			} else {
				cancel();
			}
		}

	}

	public TransactionHelper(Neo4j neo4j) {
		this(neo4j, 1000);
	}

	public TransactionHelper(Neo4j neo4j, int statementNumber) {
		this.neo4j = neo4j;
		this.remainingStatementNumber = new AtomicInteger(statementNumber);
		this.statementNumber = statementNumber;
		this.statements = new JsonArray();
		send(new JsonArray());
	}

	public void add(String query, JsonObject params) {
		if (autoSend && !waitingQuery && transactionId != null &&
				remainingStatementNumber.getAndDecrement() == 0) {
			final JsonArray s = statements;
			statements = new JsonArray();
			send(s);
			remainingStatementNumber = new AtomicInteger(statementNumber);
		}
		if (query != null && !query.trim().isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("query : " + query + " - params : " + (params != null ? params.encode() : "{}"));
			}
			JsonObject statement = new JsonObject().put("statement", query);
			if (params != null) {
				statement.put("parameters", params);
			}
			statements.add(statement);
		}
	}

	private void send(JsonArray s) {
		send(s, null);
	}

	private void send(JsonArray s, final Handler<Message<JsonObject>> handler) {
		if (error != null) {
			throw new IllegalStateException(error.body().getString("message"));
		}
		waitingQuery = true;
		neo4j.executeTransaction(s, transactionId, false, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				if (handler != null) {
					handler.handle(message);
				}
				if ("ok".equals(message.body().getString("status"))) {
					Integer tId = message.body().getInteger("transactionId");
					if (transactionId == null && tId != null) {
						transactionId = tId;
						resetTimeOutTimer = new Timer();
						//resetTimeOutTimer.schedule(new ResetTransactionTimer(), 0, 55000); // TODO use transaction expires
					}
				} else {
					error = message;
					log.error(message.body().encode());
				}
				waitingQuery = false;
				if (commit) {
					commit(commitHandler, false);
				} else if (flush) {
					flush(flushHandler, false);
				}
			}
		});
	}

	public void commit(Handler<Message<JsonObject>> handler) {
		commit(handler, true);
	}

	public void commit(Handler<Message<JsonObject>> handler, boolean th) {
		if (error != null) {
			if (!th && handler != null) {
				handler.handle(error);
			} else {
				throw new IllegalStateException(error.body().getString("message"));
			}
		}
		if (waitingQuery) {
			commit = true;
			commitHandler = handler;
			return;
		}
		if (transactionId != null || statements.size() > 0) {
			neo4j.executeTransaction(statements, transactionId, true, handler);
			if (transactionId != null) {
				resetTimeOutTimer.cancel();
				resetTimeOutTimer.purge();
				transactionId = null;
			}
		} else if (handler != null) {
			handler.handle(null);
		}
	}

	public void rollback() {
		if (transactionId != null) {
			neo4j.rollbackTransaction(transactionId, null);
			resetTimeOutTimer.cancel();
			resetTimeOutTimer.purge();
			transactionId = null;
		}
	}

	public void flush(Handler<Message<JsonObject>> handler) {
		flush(handler, true);
	}

	public void flush(Handler<Message<JsonObject>> handler, boolean th) {
		if (error != null) {
			if (!th && handler != null) {
				handler.handle(error);
			} else {
				throw new IllegalStateException(error.body().getString("message"));
			}
		}
		if (waitingQuery) {
			flush = true;
			flushHandler = handler;
		} else if (transactionId != null) {
			final JsonArray s = statements;
			statements = new JsonArray();
			send(s, handler);
			remainingStatementNumber = new AtomicInteger(statementNumber);
		}
	}

	private Integer getTransactionId() {
		return transactionId;
	}

	public Neo4j getNeo4j() {
		return neo4j;
	}

	public boolean isEmpty() {
		return statements == null || statements.size() == 0;
	}


	public boolean isAutoSend() {
		return autoSend;
	}

	public void setAutoSend(boolean autoSend) {
		this.autoSend = autoSend;
	}

}
