/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.utils;

import org.entcore.feeder.exceptions.TransactionException;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

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

}
