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

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Neo4j  {

	private EventBus eb;
	private String address;

	public Neo4j (EventBus eb, String address) {
		this.eb = eb;
		this.address = address;
	}

	public void execute(String query, JsonObject params, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		if (params != null) {
			jo.putObject("params", params);
		}
		eb.send(address, jo, handler);
	}

	public void executeTransaction(JsonArray statements, Integer transactionId, boolean commit,
								   Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "executeTransaction");
		jo.putArray("statements", statements);
		jo.putBoolean("commit", commit);
		if (transactionId != null) {
			jo.putNumber("transactionId", transactionId);
		}
		eb.send(address, jo, handler);
	}

	public void resetTransactionTimeout(int transactionId, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "resetTransactionTimeout");
		jo.putNumber("transactionId", transactionId);
		eb.send(address, jo, handler);
	}

	public void rollbackTransaction(int transactionId, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "rollbackTransaction");
		jo.putNumber("transactionId", transactionId);
		eb.send(address, jo, handler);
	}

	public static String nodeSetPropertiesFromJson(String nodeAlias, JsonObject json, String... ignore) {
		StringBuilder sb = new StringBuilder();
		List<String> i;
		if (ignore != null) {
			i = Arrays.asList(ignore);
		} else {
			i = Collections.emptyList();
		}
		for (String attr: json.getFieldNames()) {
			if (i.contains(attr)) continue;
			sb.append(", ").append(nodeAlias).append(".").append(attr).append(" = {").append(attr).append("}");
		}
		if (sb.length() > 2) {
			return sb.append(" ").substring(2);
		}
		return " ";
	}

	public void unmanagedExtension(String method, String uri, String body, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "unmanagedExtension");
		jo.putString("method", method);
		jo.putString("uri", uri);
		if (body != null) {
			jo.putString("body", body);
		}
		eb.send(address, jo, handler);
	}

}
