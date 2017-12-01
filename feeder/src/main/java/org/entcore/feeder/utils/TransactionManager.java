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
import org.entcore.feeder.exceptions.TransactionException;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TransactionManager {

	private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);
	private Neo4j neo4j;
	private final ConcurrentMap<String, TransactionHelper> transactions = new ConcurrentHashMap<>();

	private TransactionManager() {}

	private static class TransactionManagerHolder {
		private static final TransactionManager instance = new TransactionManager();
	}

	public static TransactionManager getInstance() {
		return TransactionManagerHolder.instance;
	}

	public TransactionHelper getTransaction(String name) {
		return transactions.get(name);
	}

	public void rollback(String name) {
		TransactionHelper tx = transactions.remove(name);
		if (tx != null) {
			tx.rollback();
		}
	}

	public TransactionHelper begin() throws TransactionException {
		return begin(UUID.randomUUID().toString());
	}

	public synchronized TransactionHelper begin(String name) throws TransactionException {
		if (transactions.containsKey(name)) {
			throw new TransactionException("Concurrent transaction already in use");
		}
		TransactionHelper tx = new TransactionHelper(neo4j, 1000);
		transactions.put(name, tx);
		return tx;
	}

	public void persist(final String name, final boolean reopen, final Handler<Message<JsonObject>> handler) {
		TransactionHelper transactionHelper = transactions.remove(name);
		if (transactionHelper != null) {
			transactionHelper.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					if (reopen) {
						try {
							TransactionManager.this.begin(name);
						} catch (TransactionException e) {
							log.error("Error opening transaction.", e);
							if (handler != null) {
								handler.handle(new ResultMessage().error(e.getMessage()));
							}
							return;
						}
					}
					if (handler != null) {
						handler.handle(message);
					}
				}
			});
		}
	}

	public void flush(String name, Handler<Message<JsonObject>> handler) {
		TransactionHelper transactionHelper = transactions.get("name");
		if (transactionHelper != null) {
			transactionHelper.flush(handler);
		}
	}

//	/**
//	 * Warning : all data in old uncommitted transaction will be lost.
//	 */
//	public  reinitTransaction() {
//		return transactionHelper = new TransactionHelper(neo4j, 1000);
//	}


	public void setNeo4j(Neo4j neo4j) {
		this.neo4j = neo4j;
	}

	public Neo4j getNeo4j() {
		return neo4j;
	}

	public static Neo4j getNeo4jHelper() {
		return TransactionManager.getInstance().getNeo4j();
	}

	public static TransactionHelper getTransaction() throws TransactionException {
		return TransactionManager.getInstance().begin();
	}

	public static TransactionHelper getTransaction(boolean autoSend) throws TransactionException {
		TransactionHelper tx = TransactionManager.getInstance().begin();
		tx.setAutoSend(autoSend);
		return tx;
	}

	public static void executeTransaction(final Function<TransactionHelper, Message<JsonObject>> f) {
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin();
			f.apply(tx);
			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					f.handle(event);
				}
			});
		} catch (TransactionException e) {
			log.error(e.getMessage(), e);
			f.handle(new ResultMessage().error(e.getMessage()));
		}
	}

}
