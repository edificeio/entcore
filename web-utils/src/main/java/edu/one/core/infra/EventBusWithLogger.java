package edu.one.core.infra;

import java.util.Date;
import java.util.UUID;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class EventBusWithLogger implements EventBus {

	private final EventBus eb;
	private static final Logger logger = LoggerFactory.getLogger(EventBusWithLogger.class);

	public EventBusWithLogger(EventBus eb) {
		this.eb = eb;
	}

	private void sendLog(String address, String message) {
		logger.info(MongoDb.formatDate(new Date()) + " send : " + address + " - " + message);
	}

	private void publishLog(String address, String message) {
		logger.info(MongoDb.formatDate(new Date()) + " publish : " + address + " - " + message);
	}

	private String sendLogwithResponse(String address, String message) {
		String logMessageId = UUID.randomUUID().toString();
		logger.info(MongoDb.formatDate(new Date()) + " send : " + logMessageId + " - " + address + " - " + message);
		return logMessageId;
	}

	private <T> void responseLog(String logMessageId, T response) {
		String r;
		if (response instanceof JsonObject) {
			r = ((JsonObject) response).encode();
		} else if (response instanceof JsonArray) {
			r = ((JsonArray) response).encode();
		} else if (response instanceof Buffer) {
			r = "Buffer not displayed";
		} else {
			r = response.toString();
		}
		logger.info(MongoDb.formatDate(new Date()) + " response : " + logMessageId + " - " + r);
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
		final String logMessageId = sendLogwithResponse(address, message.toString());
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
		final String logMessageId = sendLogwithResponse(address, message.encode());
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
		sendLog(address, message.encode());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, JsonArray message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.encode());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, JsonArray message) {
		sendLog(address, message.encode());
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
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, JsonObject message) {
		publishLog(address, message.encode());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, JsonArray message) {
		publishLog(address, message.encode());
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
