package org.entcore.common.neo4j;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class TransactionHelper {

    private abstract class WithHandler<T> {

        protected final Handler<T> handler;

        protected WithHandler(final Handler<T> handler) {
            this.handler = handler;
        }

        public abstract void runAndHandle();

    }

    private class EventBusRequestWithHandler extends WithHandler<Message<JsonObject>> {

        private final String address;
        private final JsonObject action;

        public EventBusRequestWithHandler(final String address, final JsonObject action,
                                          final Handler<Message<JsonObject>> handler) {
            super(handler);
            this.address = address;
            this.action = action;
        }

        @Override
        public void runAndHandle() {
            addEventBusRequest(address, action, handler);
        }

    }

    private class StatementsWithHandler extends WithHandler<Message<JsonObject>> {

        private final JsonArray statements;

        public StatementsWithHandler(final JsonArray statements, final Handler<Message<JsonObject>> handler) {
            super(handler);
            this.statements = statements;
        }

        @Override
        public void runAndHandle() {
            addStatements(statements, handler);
        }
    }

    private class RollbackWithHandler extends WithHandler<Boolean> {

        public RollbackWithHandler(final Handler<Boolean> handler) {
            super(handler);
        }

        @Override
        public void runAndHandle() {
            rollback(handler);
        }
    }

    private class CommitWithHandler extends WithHandler<Boolean> {

        public CommitWithHandler(final Handler<Boolean> handler) {
            super(handler);
        }

        @Override
        public void runAndHandle() {
            commit(handler);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TransactionHelper.class);

    private final Neo4j neo4j;
    private final EventBus eb;
    private Integer transactionId;
    private final List<WithHandler> waitingList;
    private final AtomicBoolean waitingCallback = new AtomicBoolean(false);

    public TransactionHelper(final Neo4j neo4j, final EventBus eb, final boolean begin) {
        this.neo4j = neo4j;
        this.eb = eb;
        this.waitingList = new ArrayList<>();
        if (begin) {
            begin(null);
        }
    }

    public void begin(final Handler<Boolean> handler) {
        waitingCallback.set(true);
        if (transactionId != null) {
            log.error("[Neo4j] - TransactionHelper: couldn't begin transaction because \"transactionId\" is already set with value: " + transactionId);
            handleIfNotNull(handler, false);
            waitingCallback.set(false);
            return;
        }
        neo4j.executeTransaction(new JsonArray(), null, false, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                Integer tId = message.body().getInteger("transactionId");
                if (tId != null) {
                    transactionId = tId;
                    log.info("[Neo4j] - TransactionHelper: new transaction with id \"" + transactionId + "\" has begun");
                    handleIfNotNull(handler, true);
                    waitingCallback.set(false);
                    if (!waitingList.isEmpty()) {
                        WithHandler wh = waitingList.remove(0);
                        wh.runAndHandle();
                    }
                } else {
                    log.error("[Neo4j] - TransactionHelper: couldn't begin new transaction, no transactionId found");
                    handleIfNotNull(handler, false);
                }
            } else {
                log.error("[Neo4j] - TransactionHelper: couldn't begin new transaction, error: " + message.body().getString("message"));
                handleIfNotNull(handler, false);
            }
        });
    }

    public void commit(final Handler<Boolean> handler) {
        if (waitingCallback.getAndSet(true)) {
            waitingList.add(new CommitWithHandler(handler));
            return;
        }
        if (transactionId == null) {
            log.error("[Neo4j] - TransactionHelper: couldn't commit transaction because \"transactionId\" is null. The transaction has either not begun, or been rollbacked or committed");
            handleIfNotNull(handler, false);
            waitingCallback.set(false);
            return;
        }
        neo4j.executeTransaction(new JsonArray(), transactionId, true, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                log.info("[Neo4j] - TransactionHelper: transaction with id \"" + transactionId + "\" has been committed");
                handleIfNotNull(handler, true);
                transactionId = null;
                waitingList.clear(); // As the transaction was committed, is it useless to run the waitingList
            } else {
                log.error("[Neo4j] - TransactionHelper: couldn't commit transaction with id \"" + transactionId + "\", error: " + message.body().getString("message"));
                handleIfNotNull(handler, false);
            }
            waitingCallback.set(false);
        });
    }

    public void rollback(final Handler<Boolean> handler) {
        if (waitingCallback.getAndSet(true)) {
            waitingList.add(new RollbackWithHandler(handler));
            return;
        }
        if (transactionId == null) {
            log.error("[Neo4j] - TransactionHelper: couldn't rollback transaction because \"transactionId\" is null. The transaction has either not begun, or been rollbacked or committed");
            handleIfNotNull(handler, false);
            waitingCallback.set(false);
            return;
        }
        neo4j.rollbackTransaction(transactionId, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                log.info("[Neo4j] - TransactionHelper: transaction with id \"" + transactionId + "\" has been rollbacked");
                handleIfNotNull(handler, true);
                waitingList.clear(); // As the transaction was rollbacked, is it useless to run the waitingList
            } else {
                log.error("[Neo4j] - TransactionHelper: couldn't rollback transaction with id \"" + transactionId + "\", error: " + message.body().getString("message") +
                    " (it may have been automatically rollbacked already)");
                handleIfNotNull(handler, false);
            }
            transactionId = null;
            waitingCallback.set(false);
        });
    }

    public void addStatements(final JsonArray statements, final Handler<Message<JsonObject>> handler) {
        if (waitingCallback.getAndSet(true)) {
            waitingList.add(new StatementsWithHandler(statements, handler));
            return;
        }
        if (transactionId == null) {
            log.error("[Neo4j] - TransactionHelper: couldn't execute transaction because \"transactionId\" is null. The transaction has either not begun, or been rollbacked or committed");
            handleIfNotNull(handler, null);
            waitingCallback.set(false);
            return;
        }
        neo4j.executeTransaction(statements, transactionId, false, messageHandler(handler));
    }

    public void addEventBusRequest(final String address, final JsonObject action, final Handler<Message<JsonObject>> handler) {
        if (waitingCallback.getAndSet(true)) {
            waitingList.add(new EventBusRequestWithHandler(address, action, handler));
            return;
        }
        if (transactionId == null) {
            log.error("[Neo4j] - TransactionHelper: couldn't request EventBus because \"transactionId\" is null. The transaction has either not begun, or been rollbacked or committed");
            handleIfNotNull(handler, null);
            waitingCallback.set(false);
            return;
        }
        action.put("transactionId", transactionId);
        action.put("commit", false);
        action.put("autoSend", false);
        eb.request(address, action, handlerToAsyncHandler(messageHandler(handler)));
    }

    private Handler<Message<JsonObject>> messageHandler(final Handler<Message<JsonObject>> handler) {
        return message -> {
            if (!"ok".equals(message.body().getString("status"))) {
                waitingCallback.set(false);
                waitingList.clear();
                log.error("[Neo4j] - TransactionHelper: an error occurred on transaction with id \"" + transactionId +
                        "\", which will thus be rollbacked. Error: " + message.body().getString("message"));
                rollback(null); // We try rollbacking the transaction in case it wasn't done automatically
                handleIfNotNull(handler, message);
            } else {
                handleIfNotNull(handler, message);
                waitingCallback.set(false);
                if (!waitingList.isEmpty()) {
                    WithHandler wh = waitingList.remove(0);
                    wh.runAndHandle();
                }
            }
        };
    }

    private <T> void handleIfNotNull(final Handler<T> handler, final T value) {
        if (handler != null) {
            handler.handle(value);
        }
    }

}
