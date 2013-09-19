package edu.one.core.infra;

import java.util.UUID;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class EventBusWithMongoDBLogger implements EventBus {

	private static final String LOGS_COLLECTION = "logs";
	private final EventBus eb;
	private final MongoDb mongo;

	public EventBusWithMongoDBLogger(EventBus eb) {
		this.eb = eb;
		this.mongo = new MongoDb(eb, "wse.mongodb.persistor");
	}

	private <T> JsonObject prepareLog(String address, T message) {
		JsonObject doc = new JsonObject();
		if (message instanceof JsonObject) {
			//doc.putObject("message", (JsonObject) message);
			doc.putString("message", ((JsonObject) message).encode());
		} else if (message instanceof JsonArray) {
			//doc.putArray("message", (JsonArray) message);
			doc.putString("message", ((JsonArray) message).encode());
		} else if (message instanceof Buffer) {
			doc.putString("message", "Buffer not displayed");
		} else {
			doc.putString("message", message.toString());
		}
		doc.putString("address", address)
		.putObject("date", MongoDb.now());
		return doc;
	}


	private <T> void sendLog(String address, T message) {
		JsonObject doc = prepareLog(address, message);
		doc.putString("type", "SEND");
		mongo.save(LOGS_COLLECTION, doc, MongoDb.WriteConcern.NONE, null);
	}

	private <T> void publishLog(String address, T message) {
		JsonObject doc = prepareLog(address, message);
		doc.putString("type", "PUBLISH");
		mongo.save(LOGS_COLLECTION, doc, MongoDb.WriteConcern.NONE, null);
	}

	private <T> String sendLogwithResponse(String address, T message) {
		String logMessageId = UUID.randomUUID().toString();
		JsonObject doc = prepareLog(address, message);
		doc.putString("_id", logMessageId)
		.putString("type", "SEND_WITH_REPLY");
		mongo.save(LOGS_COLLECTION, doc, MongoDb.WriteConcern.NONE, null);
		return logMessageId;
	}

	private <T> void responseLog(String logMessageId, T response) {
		JsonObject doc = new JsonObject();
		if (response instanceof JsonObject) {
			//doc.putObject("response", (JsonObject) response);
			doc.putString("response", ((JsonObject) response).encode());
		} else if (response instanceof JsonArray) {
			//doc.putArray("response", (JsonArray) response);
			doc.putString("response", ((JsonArray) response).encode());
		} else if (response instanceof Buffer) {
			doc.putString("response", "Buffer not displayed");
		} else {
			doc.putString("response", response.toString());
		}
		doc.putString("messageId", logMessageId)
		.putObject("date", MongoDb.now())
		.putString("type", "REPLY");
		mongo.save(LOGS_COLLECTION, doc, MongoDb.WriteConcern.NONE, null);
	}

	@Override
	public void close(Handler<AsyncResult<Void>> doneHandler) {
		eb.close(doneHandler);
	}

	@Override
	public EventBus send(String address, Object message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus send(String address, Object message,
			final Handler<Message> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message);
		if (replyHandler != null) {
			return eb.send(address, message, new Handler<Message>() {

				@Override
				public void handle(Message event) {
					responseLog(logMessageId, event.body());
					replyHandler.handle(event);
				}
			});
		} else {
			return eb.send(address, message, replyHandler);
		}
	}

	@Override
	public <T> EventBus send(String address, JsonObject message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message);
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	private <T> Handler<Message<T>> replyHandler(
			final Handler<Message<T>> replyHandler, final String logMessageId) {
		if (replyHandler == null) {
			return null;
		}
		return new Handler<Message<T>>() {

			@Override
			public void handle(Message<T> event) {
				responseLog(logMessageId, event.body());
				replyHandler.handle(event);
			}
		};
	}

	@Override
	public EventBus send(String address, JsonObject message) {
		sendLog(address, message);
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, JsonArray message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message);
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, JsonArray message) {
		sendLog(address, message);
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Buffer message,
			final Handler<Message<T>> replyHandler) {
//		final String logMessageId = sendLogwithResponse(address, message.toString());
//		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
		return eb.send(address, message, replyHandler);
	}

	@Override
	public EventBus send(String address, Buffer message) {
		//sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, byte[] message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, new String(message));
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, byte[] message) {
		sendLog(address, new String(message));
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, String message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message);
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, String message) {
		sendLog(address, message);
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Integer message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Integer message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Long message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Long message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Float message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Float message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Double message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Double message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Boolean message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Boolean message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Short message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Short message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Character message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Character message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Byte message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Byte message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Object message) {
		publishLog(address, message);
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, JsonObject message) {
		publishLog(address, message);
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, JsonArray message) {
		publishLog(address, message);
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Buffer message) {
		//publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, byte[] message) {
		publishLog(address, new String(message));
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, String message) {
		publishLog(address, message);
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Integer message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Long message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Float message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Double message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Boolean message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Short message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Character message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Byte message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus unregisterHandler(String address,
			Handler<? extends Message> handler,
			Handler<AsyncResult<Void>> resultHandler) {
		return eb.unregisterHandler(address, handler, resultHandler);
	}

	@Override
	public EventBus unregisterHandler(String address,
			Handler<? extends Message> handler) {
		return eb.unregisterHandler(address, handler);
	}

	@Override
	public EventBus registerHandler(String address,
			Handler<? extends Message> handler,
			Handler<AsyncResult<Void>> resultHandler) {
		return eb.registerHandler(address, handler, resultHandler);
	}

	@Override
	public EventBus registerHandler(String address,
			Handler<? extends Message> handler) {
		return eb.registerHandler(address, handler);
	}

	@Override
	public EventBus registerLocalHandler(String address,
			Handler<? extends Message> handler) {
		return eb.registerLocalHandler(address, handler);
	}

}
