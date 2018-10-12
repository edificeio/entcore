/*
 * Copyright © "Open Digital Education", 2018
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 */

///*
// * Copyright © "Open Digital Education", 2016
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
