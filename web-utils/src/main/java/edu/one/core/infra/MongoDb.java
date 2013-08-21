package edu.one.core.infra;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class MongoDb {

	private static final String ISO_DATE_FORMAT = "yyyy-MM-dd HH:mm.ss.SSS";
	private final EventBus eb;
	private final String address;

	public static enum WriteConcern {
		NONE, NORMAL, SAFE, MAJORITY, FSYNC_SAFE, JOURNAL_SAFE, REPLICAS_SAFE;
	}

	public MongoDb(EventBus eb, String address) {
		this.eb = eb;
		this.address = address;
	}

	public void save(String collection, JsonObject document, WriteConcern writeConcern,
			Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "save");
		jo.putString("collection", collection);
		jo.putObject("document", document);
		if (writeConcern != null) {
			jo.putString("write_concern", writeConcern.name());
		}
		eb.send(address, jo, callback);
	}

	public void save(String collection, JsonObject document,
			Handler<Message<JsonObject>> callback) {
		save(collection, document, null, callback);
	}

	public void save(String collection, JsonObject document) {
		save(collection, document, null, null);
	}

	public void insert(String collection, JsonArray documents, WriteConcern writeConcern,
			Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "insert");
		jo.putString("collection", collection);
		if (documents.size() > 1) {
			jo.putArray("documents", documents);
			jo.putBoolean("multiple", true);
		} else {
			jo.putObject("document", (JsonObject) documents.get(0));
		}
		if (writeConcern != null) {
			jo.putString("write_concern", writeConcern.name());
		}
		eb.send(address, jo, callback);
	}

	public void insert(String collection, JsonArray documents,
			Handler<Message<JsonObject>> callback) {
		insert(collection, documents, null, callback);
	}

	public void insert(String collection, JsonObject document,
			Handler<Message<JsonObject>> callback) {
		insert(collection, new JsonArray().add(document), null, callback);
	}

	public void insert(String collection, JsonArray documents) {
		insert(collection, documents, null, null);
	}

	public void insert(String collection, JsonObject document) {
		insert(collection, new JsonArray().add(document), null, null);
	}

	/**
	 *
	 * @param collection
	 * @param criteria the query argument corresponds to the WHERE statement
	 * @param objNew the update corresponds to the SET ... statement
	 * @param upsert if true and document doesn't exist, save it
	 * @param multi update all document matching query
	 * @param writeConcern
	 * @param callback
	 */
	public void update(String collection, JsonObject criteria, JsonObject objNew,
			boolean upsert, boolean multi, WriteConcern writeConcern,
			Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "update");
		jo.putString("collection", collection);
		jo.putObject("criteria", criteria);
		jo.putObject("objNew", objNew);
		jo.putBoolean("upsert", upsert);
		jo.putBoolean("multi", multi);
		if (writeConcern != null) {
			jo.putString("write_concern", writeConcern.name());
		}
		eb.send(address, jo, callback);
	}

	public void update(String collection, JsonObject criteria, JsonObject objNew,
			boolean upsert, boolean multi, Handler<Message<JsonObject>> callback) {
		update(collection, criteria, objNew, upsert, multi, null, callback);
	}

	public void update(String collection, JsonObject criteria, JsonObject objNew,
			boolean upsert, boolean multi) {
		update(collection, criteria, objNew, upsert, multi, null, null);
	}

	public void update(String collection, JsonObject criteria, JsonObject objNew) {
		update(collection, criteria, objNew, false, false, null, null);
	}

	public void update(String collection, JsonObject criteria, JsonObject objNew,
			Handler<Message<JsonObject>> callback) {
		update(collection, criteria, objNew, false, false, null, callback);
	}

	public void find(String collection, JsonObject matcher, JsonObject sort, JsonObject keys,
			int skip, int limit, int batchSize, Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "find");
		jo.putString("collection", collection);
		jo.putObject("matcher", matcher);
		jo.putObject("sort", sort);
		jo.putObject("keys", keys);
		jo.putNumber("skip", skip);
		jo.putNumber("limit", limit);
		eb.send(address, jo, callback);
	}

	public void find(String collection, JsonObject matcher, JsonObject sort, JsonObject keys,
			Handler<Message<JsonObject>> callback) {
		find(collection, matcher, sort, keys, -1, -1, 100, callback);
	}

	public void find(String collection, JsonObject matcher,
			Handler<Message<JsonObject>> callback) {
		find(collection, matcher, null, null, -1, -1, 100, callback);
	}

	public void findOne(String collection, JsonObject matcher, JsonObject keys,
			Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "findone");
		jo.putString("collection", collection);
		jo.putObject("matcher", matcher);
		jo.putObject("keys", keys);
		eb.send(address, jo, callback);
	}

	public void findOne(String collection, JsonObject matcher,
			Handler<Message<JsonObject>> callback) {
		findOne(collection, matcher, null, callback);
	}

	public void count(String collection, JsonObject matcher,
			Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "count");
		jo.putString("collection", collection);
		jo.putObject("matcher", matcher);
		eb.send(address, jo, callback);
	}

	public void distinct(String collection, String key, JsonObject matcher, Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "distinct");
		jo.putString("collection", collection);
		jo.putString("key", key);
		jo.putObject("matcher", matcher);
		eb.send(address, jo, callback);
	}

	public void distinct(String collection, String key, Handler<Message<JsonObject>> callback) {
		distinct(collection, key, null, callback);
	}

	public void delete(String collection, JsonObject matcher, WriteConcern writeConcern,
			Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "delete");
		jo.putString("collection", collection);
		jo.putObject("matcher", matcher);
		if (writeConcern != null) {
			jo.putString("write_concern", writeConcern.name());
		}
		eb.send(address, jo, callback);
	}

	public void delete(String collection, JsonObject matcher,
			Handler<Message<JsonObject>> callback) {
		delete(collection, matcher, null, callback);
	}

	public void delete(String collection, JsonObject matcher) {
		delete(collection, matcher, null, null);
	}

	public void command(String command, Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "command");
		jo.putString("command", command);
		eb.send(address, jo, callback);
	}

	public void command(String command) {
		command(command, null);
	}

	public void getCollections(Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "getCollections");
		eb.send(address, jo, callback);
	}

	public void getCollectionStats(String collection, Handler<Message<JsonObject>> callback) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "collectionStats");
		jo.putString("collection", collection);
		eb.send(address, jo, callback);
	}

	public static String formatDate(Date date) {
		DateFormat df = new SimpleDateFormat(ISO_DATE_FORMAT);
		return df.format(date);
	}

	public static Date parseDate(String date) throws ParseException {
		DateFormat df = new SimpleDateFormat(ISO_DATE_FORMAT);
		return df.parse(date);
	}

	public static JsonObject now() {
		return new JsonObject().putNumber("$date", System.currentTimeMillis());
	}

	public static Date parseIsoDate(JsonObject date) {
		Calendar c = DatatypeConverter.parseDateTime(date.getString("$date"));
		return c.getTime();
	}
}
