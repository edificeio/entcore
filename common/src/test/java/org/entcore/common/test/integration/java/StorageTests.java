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
//import org.entcore.common.storage.BucketStats;
//import org.entcore.common.storage.Storage;
//import io.vertx.core.AsyncResult;
//import io.vertx.core.Handler<AsyncResult>;
//
//import static io.vertx.testtools.VertxAssert.assertTrue;
//import static io.vertx.testtools.VertxAssert.testComplete;
//
//public class StorageTests {
//
//	private final Storage storage;
//
//	public StorageTests(Storage storage) {
//		this.storage = storage;
//	}
//
//	public void statsTest() {
//		storage.stats(new Handler<AsyncResult><BucketStats>() {
//			@Override
//			public void handle(AsyncResult<BucketStats> event) {
//				System.out.println(event.result().getJsonObjectNumber());
//				System.out.println(event.result().getStorageSize());
//				assertTrue(event.succeeded());
//				assertTrue(event.result().getJsonObjectNumber() > 0);
//				assertTrue(event.result().getStorageSize() > 1000);
//				testComplete();
//			}
//		});
//	}
//
//}
