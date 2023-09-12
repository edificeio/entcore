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

package org.entcore.common.neo4j;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.schema.Source;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import fr.wseduc.webutils.Either;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

public class TransactionHelper
{
    public static final TransactionHelper DEFAULT = new TransactionHelper(Source.MANUAL)
	{
		@Override
		public void add(Statement s)
		{
			super.add(s);
			this.commit();
		}
	};

	private static final Logger log = LoggerFactory.getLogger(TransactionHelper.class);
	private final Neo4j neo4j;
	private volatile List<Statement> statements;
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

	public final Source source;

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

	public static class Statement
	{
		public String query;
		public JsonObject params;
		public Handler<Either<String, JsonArray>> resultHandler;

		public Statement(String query)
		{
			this(query, (JsonObject) null);
		}

		public Statement(String query, Promise<JsonArray> promise)
		{
			this(query, null, promise);
		}

		public Statement(String query, JsonObject params)
		{
			this(query, params, (Handler<Either<String, JsonArray>>) null);
		}

		public Statement(String query, JsonObject params, Promise<JsonArray> promise)
		{
			this(query, params, new Handler<Either<String, JsonArray>>()
			{
				@Override
				public void handle(Either<String, JsonArray> res)
				{
					if(promise != null)
					{
						if(res.isLeft())
							promise.fail(res.left().getValue());
						else
							promise.complete(res.right().getValue());
					}
				}
			});
		}

		public Statement(String query, JsonObject params, Handler<Either<String, JsonArray>> resultHandler)
		{
			this.query = query;
			this.params = params;
			this.resultHandler = resultHandler;
		}

		protected JsonObject toJsonObject()
		{
			JsonObject o = new JsonObject().put("statement", this.query);
			if(this.params != null)
				o.put("parameters", this.params);
			return o;
		}

		protected void handleResult(JsonArray result)
		{
			if(this.resultHandler != null)
				this.resultHandler.handle(new Either.Right<String, JsonArray>(result));
		}

		protected void handleError(String error)
		{
			if(this.resultHandler != null)
				this.resultHandler.handle(new Either.Left<String, JsonArray>(error));
		}

		protected static JsonArray toNeoStatements(List<Statement> statements)
		{
			JsonArray neoStatements = new JsonArray();
			for(int i = 0; i < statements.size(); i++)
				neoStatements.add(statements.get(i).toJsonObject());
			return neoStatements;
		}
	}

	public TransactionHelper(Source source)
	{
		this(Neo4j.getInstance(), source);
	}

	public TransactionHelper(Neo4j neo4j, Source source, Integer transactionId) {
		this(neo4j, source, 1000, transactionId);
	}

	public TransactionHelper(Neo4j neo4j, Source source) {
		this(neo4j, source, 1000);
	}

	public TransactionHelper(Neo4j neo4j, Source source, int statementNumber) {
		this(neo4j, source, statementNumber, null);
	}

	public TransactionHelper(Neo4j neo4j, Source source, int statementNumber, Integer transactionId) {
		this.neo4j = neo4j;
		this.source = source;
		this.remainingStatementNumber = new AtomicInteger(statementNumber);
		this.statementNumber = statementNumber;
		this.statements = new ArrayList<Statement>();
		this.transactionId = transactionId;
		if (this.transactionId == null) {
			send(new ArrayList<Statement>());
		}
	}

	public void add(String query, JsonObject params)
	{
		this.add(query, params, (Promise<JsonArray>) null);
	}

	public void add(String query, JsonObject params, Promise<JsonArray> promise)
	{
		this.add(new Statement(query, params, promise));
	}

	public void add(String query, JsonObject params, Handler<Either<String, JsonArray>> resultHandler)
	{
		this.add(new Statement(query, params, resultHandler));
	}
	
	public void add(Statement statement)
	{
		if(statement == null)
			return;
		if (autoSend && !waitingQuery && transactionId != null &&
				remainingStatementNumber.getAndDecrement() == 0) {
			final List<Statement> s = statements;
			this.statements = new ArrayList<Statement>();
			send(s);
			remainingStatementNumber = new AtomicInteger(statementNumber);
		}
		String query = statement.query;
		JsonObject params = statement.params;
		if (query != null && !query.trim().isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("query : " + query + " - params : " + (params != null ? params.encode() : "{}"));
			}
			statements.add(statement);
		}
	}

	private void send(List<Statement> s) {
		send(s, null);
	}

	private void send(List<Statement> s, final Handler<Message<JsonObject>> handler) {
		if (error != null) {
			throw new IllegalStateException(error.body().getString("message"));
		}
		waitingQuery = true;
		neo4j.executeTransaction(Statement.toNeoStatements(s), transactionId, false, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				if (handler != null) {
					if(handler == flushHandler)
						flushHandler = null;
					handler.handle(message);
				}
				JsonObject body = message.body();
				if ("ok".equals(message.body().getString("status"))) {
					Integer tId = message.body().getInteger("transactionId");
					if (transactionId == null && tId != null) {
						transactionId = tId;
						resetTimeOutTimer = new Timer();
						//resetTimeOutTimer.schedule(new ResetTransactionTimer(), 0, 55000); // TODO use transaction expires
					}
					JsonArray results = body.getJsonArray("results");
					for(int i = 0; i < s.size(); i++)
						s.get(i).handleResult(results.getJsonArray(i));
				} else {
					error = message;
					log.error(message.body().encode());
					String error = body.getString("message", "");
					for(int i = 0; i < s.size(); i++)
						s.get(i).handleError(error);
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

	public Future<JsonArray> commit()
	{
		Promise<JsonArray> promise = Promise.promise();
		commit(new Handler<Message<JsonObject>>()
		{
			@Override
			public void handle(Message<JsonObject> msg)
			{
				JsonObject body = msg.body();
				if("ok".equals(body.getString("status")))
					promise.complete(body.getJsonArray("results"));
				else
					promise.fail(body.getString("message", ""));
			}
		});
		return promise.future();
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
		if (transactionId != null || statements.size() > 0)
		{
			List<Statement> commitStatements = statements;
			this.statements = new ArrayList<Statement>();
			neo4j.executeTransaction(Statement.toNeoStatements(commitStatements), transactionId, true, new Handler<Message<JsonObject>>()
			{
				@Override
				public void handle(Message<JsonObject> msg)
				{
					JsonObject body = msg.body();

					if("ok".equals(body.getString("status")))
					{
						JsonArray results = body.getJsonArray("results");
						for(int i = 0; i < commitStatements.size(); i++)
							commitStatements.get(i).handleResult(results.getJsonArray(i));
					}
					else
					{
						String error = body.getString("message", "");
						for(int i = 0; i < commitStatements.size(); i++)
							commitStatements.get(i).handleError(error);
					}

					if(handler != null)
						handler.handle(msg);
				}
			});
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

	public Future<JsonArray> flush()
	{
		Promise<JsonArray> promise = Promise.promise();
		flush(new Handler<Message<JsonObject>>()
		{
			@Override
			public void handle(Message<JsonObject> msg)
			{
				JsonObject body = msg.body();
				if("ok".equals(body.getString("status")))
					promise.complete(body.getJsonArray("results"));
				else
					promise.fail(body.getString("message", ""));
			}
		});
		return promise.future();
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
			final List<Statement> s = statements;
			statements = new ArrayList<Statement>();
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
