///*
// * Copyright © WebServices pour l'Éducation, 2014
// *
// * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as
// * published by the Free Software Foundation (version 3 of the License).
// *
// * For the sake of explanation, any module that communicate over native
// * Web protocols, such as HTTP, with ENT Core is outside the scope of this
// * license and could be license under its own terms. This is merely considered
// * normal use of ENT Core, and does not fall under the heading of "covered work".
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// */
//
//package org.entcore.common.test.integration.java;
//
//import fr.wseduc.webutils.Either;
//import org.entcore.common.service.CrudService;
//import org.entcore.common.service.VisibilityFilter;
//import org.entcore.common.service.impl.SqlCrudService;
//import org.entcore.common.share.ShareService;
//import org.entcore.common.share.impl.SqlShareService;
//import org.entcore.common.sql.Sql;
//import org.entcore.common.user.UserInfos;
//import org.junit.Test;
//import io.vertx.core.AsyncResult;
//import io.vertx.core.Handler<AsyncResult>;
//import io.vertx.core.Handler;
//import io.vertx.core.eventbus.EventBus;
//import io.vertx.core.eventbus.Message;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import io.vertx.testtools.TestVerticle;
//
//import java.util.UUID;
//
//import static io.vertx.testtools.VertxAssert.assertEquals;
//import static io.vertx.testtools.VertxAssert.assertTrue;
//import static io.vertx.testtools.VertxAssert.testComplete;
//
//public class SqlTestVerticle extends TestVerticle {
//
//	public static final String ADDRESS = "sql.persistor";
//	private EventBus eb;
//	private CrudService crudService;
//	private ShareService shareService;
//
//	@Override
//	public void start() {
//		JsonObject config = new JsonObject()
//			.put("address", ADDRESS)
//			.put("username", "web-education")
//			.put("password", "We_1234")
//			.put("database", "test");
//		container.deployModule("io.vertx~mod-mysql-postgresql~0.3.0-SNAPSHOT", config, new Handler<AsyncResult><String>() {
//			@Override
//			public void handle(AsyncResult<String> ar) {
//				if (ar.succeeded()) {
//					eb = vertx.eventBus();
//					Sql.getInstance().init(eb, ADDRESS);
//					JsonArray r = new JsonArray().add("id").add("name").add("number").add("modified");
//					JsonArray rl = new JsonArray().add("id").add("name");
//					crudService = new SqlCrudService("test", "tests", null, r, rl, true);
//					shareService = new SqlShareService(eb, null, null);
//					SqlTestVerticle.super.start();
//				} else {
//					ar.cause().printStackTrace();
//				}
//			}
//		});
//	}
//
//	@Test
//	public void createResource() {
//		JsonObject j = new JsonObject().put("name", "paper").put("number", 3);
//		UserInfos user = new UserInfos();
//		user.setUserId(UUID.randomUUID().toString());
//		user.setUsername("Titi TOTO");
//		crudService.create(j, user, new Handler<Either<String, JsonObject>>() {
//			@Override
//			public void handle(Either<String, JsonObject> r) {
//				assertTrue(r.isRight());
//				System.out.println(r.right().getValue().encodePrettily());
//				testComplete();
//			}
//		});
//	}
//
//	@Test
//	public void updateResource() {
//		JsonObject j = new JsonObject().put("name", "carton").put("number", 4);
//		crudService.update("3", j, new Handler<Either<String, JsonObject>>() {
//			@Override
//			public void handle(Either<String, JsonObject> r) {
//				assertTrue(r.isRight());
//				System.out.println(r.right().getValue().encodePrettily());
//				testComplete();
//			}
//		});
//	}
//
//	@Test
//	public void deleteResource() {
//		crudService.delete("4", new Handler<Either<String, JsonObject>>() {
//			@Override
//			public void handle(Either<String, JsonObject> r) {
//				assertTrue(r.isRight());
//				System.out.println(r.right().getValue().encodePrettily());
//				testComplete();
//			}
//		});
//	}
//
//	@Test
//	public void getResource() {
//		crudService.retrieve("3", new Handler<Either<String, JsonObject>>() {
//			@Override
//			public void handle(Either<String, JsonObject> r) {
//				assertTrue(r.isRight());
//				System.out.println(r.right().getValue().encodePrettily());
//				testComplete();
//			}
//		});
//	}
//
//	@Test
//	public void listResources() {
//		crudService.list(new Handler<Either<String, JsonArray>>() {
//			@Override
//			public void handle(Either<String, JsonArray> r) {
//				assertTrue(r.isRight());
//				System.out.println(r.right().getValue().encodePrettily());
//				testComplete();
//			}
//		});
//	}
//
//	@Test
//	public void checkIfScriptsTableExists()  {
//		String q = "select count(*) as nb from information_schema.tables where table_name = 'scripts'";
//		Sql.getInstance().raw(q, new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				System.out.println(message.body().encodePrettily());
//				testComplete();
//			}
//		});
//	}
//
//	@Test
//	public void testInsertReturn()  {
//		String q =  "INSERT INTO test.tests (name,number,owner) VALUES " +
//				"('paper',3,'a6930a8f-d5cc-4968-9208-5251210f99bd') RETURNING id";
//		Sql.getInstance().raw(q, new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				System.out.println(message.body().encodePrettily());
//				testComplete();
//			}
//		});
//	}
//
//	@Test
//	public void getResourceWithShare() {
//		crudService.retrieve("6", new Handler<Either<String, JsonObject>>() {
//			@Override
//			public void handle(Either<String, JsonObject> r) {
//				assertTrue(r.isRight());
//				System.out.println(r.right().getValue().encodePrettily());
//				testComplete();
//			}
//		});
//	}
//
//	@Test
//	public void listResourceWithShare() {
//		crudService.list(new Handler<Either<String, JsonArray>>() {
//			@Override
//			public void handle(Either<String, JsonArray> r) {
//				assertTrue(r.isRight());
//				System.out.println(r.right().getValue().encodePrettily());
//				testComplete();
//			}
//		});
//	}
//
//}
