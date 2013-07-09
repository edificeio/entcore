package com.wse.gridfs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.UUID;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;

public class GridFSPersistor extends BusModBase implements Handler<Message<Buffer>> {

	protected String address;
	protected String host;
	protected int port;
	protected String dbName;
	protected String username;
	protected String password;
	protected String bucket;

	protected Mongo mongo;
	protected DB db;

	public void start() {
		super.start();

		address = getOptionalStringConfig("address", "vertx.gridfspersistor");

		host = getOptionalStringConfig("host", "localhost");
		port = getOptionalIntConfig("port", 27017);
		dbName = getOptionalStringConfig("db_name", "default_db");
		username = getOptionalStringConfig("username", null);
		password = getOptionalStringConfig("password", null);
		int poolSize = getOptionalIntConfig("pool_size", 10);
		bucket = getOptionalStringConfig("bucket", "fs");

		try {
			MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
			builder.connectionsPerHost(poolSize);
			ServerAddress address = new ServerAddress(host, port);
			mongo = new MongoClient(address, builder.build());
			db = mongo.getDB(dbName);
			if (username != null && password != null) {
				db.authenticate(username, password.toCharArray());
			}
		} catch (UnknownHostException e) {
			logger.error("Failed to connect to mongo server", e);
		}
		eb.registerHandler(address, this);
	}

	public void stop() {
		mongo.close();
	}

	@Override
	public void handle(Message<Buffer> message) {
		if (message.body() != null) {
			Buffer content = message.body();
			int headerSize = content.getInt(content.length() - 4);
			byte [] header = content.getBytes(content.length() - 4 - headerSize, content.length() - 4);
			JsonObject json = new JsonObject();
			try {
				json = new JsonObject(new String(header, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				container.logger().error(e.getMessage(), e);
			}
			byte [] data = content.getBytes(0, content.length() - 4 - headerSize);

			switch (json.getString("action")) {
			case "save":
				persistFile(message, data, json);
				break;
			case "findone":
				getFile(message, json);
				break;
			case "remove":
				removeFile(message, json);
				break;
			case "copy":
				copyFile(message, json);
				break;
			default:
				replyError(message, "Invalid message");
				break;
			}
		} else {
			replyError(message, "Invalid message");
		}
	}

	private void getFile(Message<Buffer> message, JsonObject json) {
		JsonObject query = json.getObject("query");
		if (query == null) {
			return;
		}
		GridFS fs = new GridFS(db, bucket);
		try {
			GridFSDBFile f = fs.findOne(jsonToDBObject(query));
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			f.writeTo(os);
			message.reply(new Buffer(os.toByteArray()));
		} catch (IOException | MongoException e) {
			container.logger().error(e.getMessage(), e);
			JsonObject j = new JsonObject().putString("status", "error").putString("message", e.getMessage());
			try {
				message.reply(new Buffer(j.encode().getBytes("UTF-8")));
			} catch (UnsupportedEncodingException e1) {
				container.logger().error(e1.getMessage(), e1);
			}
		}
	}

	private void persistFile(Message<Buffer> message, byte[] data, JsonObject header) {
		GridFS fs = new GridFS(db, bucket);
		GridFSInputFile f = fs.createFile(data);
		String id = header.getString("_id");
		if (id == null || id.trim().isEmpty()) {
			id = UUID.randomUUID().toString();
		}
		f.setId(id);
		f.setContentType(header.getString("content-type"));
		f.setFilename(header.getString("filename"));
		f.save();
		JsonObject reply = new JsonObject();
		reply.putString("_id", id);
		replyOK(message, reply);
	}

	private void copyFile(Message<Buffer> message, JsonObject json) {
		JsonObject query = json.getObject("query");
		if (query == null) {
			return;
		}
		GridFS fs = new GridFS(db, bucket);
		try {
			GridFSDBFile f = fs.findOne(jsonToDBObject(query));
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			f.writeTo(os);
			JsonObject j = new JsonObject();
			j.putString("content-type", f.getContentType());
			j.putString("filename", f.getFilename());
			persistFile(message, os.toByteArray(), j);
		} catch (IOException | MongoException e) {
			replyError(message, e.getMessage());
		}
	}

	private void removeFile(Message<Buffer> message, JsonObject json) {
		JsonObject query = json.getObject("query");
		if (query == null) {
			return;
		}
		GridFS fs = new GridFS(db, bucket);
		try {
			fs.remove(jsonToDBObject(query));
			replyOK(message, null);
		} catch (MongoException e) {
			replyError(message, e.getMessage());
		}
	}

	private DBObject jsonToDBObject(JsonObject object) {
		String str = object.encode();
		return (DBObject)JSON.parse(str);
	}

	protected void replyOK(Message<Buffer> message, JsonObject reply) {
		if (reply == null) {
			reply = new JsonObject();
		}
		reply.putString("status", "ok");
		message.reply(reply);
	}

	protected void replyError(Message<Buffer> message, String error) {
		logger.error(error);
		JsonObject json = new JsonObject().putString("status", "error").putString("message", error);
		message.reply(json);
	}
}
