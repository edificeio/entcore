/*
 * Copyright © WebServices pour l'Éducation, 2014
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
 */

package org.entcore.common.test.integration.java;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.share.ShareService;
import org.entcore.common.share.impl.SqlShareService;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import java.util.UUID;

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.assertTrue;
import static org.vertx.testtools.VertxAssert.testComplete;

public class SqlTestVerticle extends TestVerticle {

	public static final String ADDRESS = "sql.persistor";
	private EventBus eb;
	private CrudService crudService;
	private ShareService shareService;

	@Override
	public void start() {
		JsonObject config = new JsonObject()
			.putString("address", ADDRESS)
			.putString("username", "web-education")
			.putString("password", "We_1234")
			.putString("database", "test");
		container.deployModule("io.vertx~mod-mysql-postgresql~0.3.0-SNAPSHOT", config, new AsyncResultHandler<String>() {
			@Override
			public void handle(AsyncResult<String> ar) {
				if (ar.succeeded()) {
					eb = vertx.eventBus();
					Sql.getInstance().init(eb, ADDRESS);
					JsonArray r = new JsonArray().add("id").add("name").add("number").add("modified");
					JsonArray rl = new JsonArray().add("id").add("name");
					crudService = new SqlCrudService(null, "tests", null, r, rl);
					shareService = new SqlShareService(eb, null, null);
					SqlTestVerticle.super.start();
				} else {
					ar.cause().printStackTrace();
				}
			}
		});
	}

	@Test
	public void createResource() {
		JsonObject j = new JsonObject().putString("name", "paper").putNumber("number", 3);
		UserInfos user = new UserInfos();
		user.setUserId(UUID.randomUUID().toString());
		user.setUsername("Titi TOTO");
		crudService.create(j, user, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				assertTrue(r.isRight());
				System.out.println(r.right().getValue().encodePrettily());
				testComplete();
			}
		});
	}

	@Test
	public void updateResource() {
		JsonObject j = new JsonObject().putString("name", "carton").putNumber("number", 4);
		crudService.update("3", j, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				assertTrue(r.isRight());
				System.out.println(r.right().getValue().encodePrettily());
				testComplete();
			}
		});
	}

	@Test
	public void deleteResource() {
		crudService.delete("4", new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				assertTrue(r.isRight());
				System.out.println(r.right().getValue().encodePrettily());
				testComplete();
			}
		});
	}

	@Test
	public void getResource() {
		crudService.retrieve("3", new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				assertTrue(r.isRight());
				System.out.println(r.right().getValue().encodePrettily());
				testComplete();
			}
		});
	}

	@Test
	public void listResources() {
		crudService.list(new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> r) {
				assertTrue(r.isRight());
				System.out.println(r.right().getValue().encodePrettily());
				testComplete();
			}
		});
	}

	@Test
	public void checkIfScriptsTableExists()  {
		String q = "select count(*) as nb from information_schema.tables where table_name = 'scripts'";
		Sql.getInstance().raw(q, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				System.out.println(message.body().encodePrettily());
				testComplete();
			}
		});
	}

	@Test
	public void testInsertReturn()  {
		String q =  "INSERT INTO tests (name,number,owner) VALUES " +
				"('paper',3,'ae52be49-3970-4cbf-a1fe-252fb7f48aa7') RETURNING id";
		Sql.getInstance().raw(q, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				System.out.println(message.body().encodePrettily());
				testComplete();
			}
		});
	}
}
