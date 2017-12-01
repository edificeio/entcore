///*
// * Copyright © WebServices pour l'Éducation, 2016
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
//import fr.wseduc.mongodb.MongoDb;
//import org.entcore.common.storage.BucketStats;
//import org.entcore.common.storage.Storage;
//import org.entcore.common.storage.StorageFactory;
//import org.junit.Test;
//import io.vertx.core.AsyncResult;
//import io.vertx.core.Handler<AsyncResult>;
//import io.vertx.core.json.JsonObject;
//import io.vertx.testtools.TestVerticle;
//
//import static io.vertx.testtools.VertxAssert.assertTrue;
//import static io.vertx.testtools.VertxAssert.testComplete;
//
//public class GridfsStorageTestVerticle extends TestVerticle {
//
//	private StorageTests storageTests;
//
//	@Override
//	public void start() {
//		JsonObject config = new JsonObject("{\n" +
//				"        \"worker\": true,\n" +
//				"        \"multi-threaded\": true,\n" +
//				"        \"address\": \"wse.mongodb.persistor\",\n" +
//				"        \n" +
//				"        \"host\": \"localhost\",\n" +
//				"        \"port\": 27017,\n" +
//				"        \"db_name\": \"one_gridfs\",\n" +
//				"        \"use_mongo_types\": true,\n" +
//				"        \"pool_size\": 10\n" +
//				"      }");
//		container.deployModule("io.vertx~mod-mongo-persistor~2.1.2-WSE", config, new Handler<AsyncResult><String>() {
//			@Override
//			public void handle(AsyncResult<String> ar) {
//				if (ar.succeeded()) {
//					MongoDb.getInstance().init(vertx.eventBus(), "wse.mongodb.persistor");
//					storageTests = new StorageTests(new StorageFactory(vertx, new JsonObject()
//							.put("gridfs-address", "wse.gridfs.persistor")).getStorage());
//					GridfsStorageTestVerticle.super.start();
//				} else {
//					ar.cause().printStackTrace();
//
//				}
//			}
//		});
//	}
//
//	@Test
//	public void statsTest() {
//		storageTests.statsTest();
//	}
//
//}
