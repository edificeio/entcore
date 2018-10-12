/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
