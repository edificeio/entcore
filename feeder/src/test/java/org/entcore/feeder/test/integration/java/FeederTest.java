///* Copyright © WebServices pour l'Éducation, 2014
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
// *
// */
//
//package org.entcore.feeder.test.integration.java;
//
//import org.entcore.common.neo4j.Neo4j;
//import org.junit.Test;
//import org.junit.rules.TemporaryFolder;
//import io.vertx.core.AsyncResult;
//import io.vertx.core.Handler<AsyncResult>;
//import io.vertx.core.Handler;
//import io.vertx.core.Handler<Void>;
//import io.vertx.core.eventbus.EventBus;
//import io.vertx.core.eventbus.Message;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import io.vertx.testtools.TestVerticle;
//
//import java.io.File;
//import java.io.IOException;
//
//import static io.vertx.testtools.VertxAssert.*;
//
//public class FeederTest extends TestVerticle {
//
//	public static final String NEO4J_PERSISTOR = "neo4j.persistor";
//	public static final String ENTCORE_FEEDER = "entcore.feeder";
//	private String neo4jDeploymentId;
//	private TemporaryFolder neo4jTmpFolder;
//	private TemporaryFolder importTmpFolder;
//	private EventBus eb;
//	private Neo4j neo4j;
//
//	@Override
//	public void start() {
//		neo4jTmpFolder = new TemporaryFolder();
//		importTmpFolder = new TemporaryFolder();
//		try {
//			neo4jTmpFolder.create();
//			importTmpFolder.create();
//		} catch (IOException e) {
//			e.printStackTrace();
//			return;
//		}
//		vertx.fileSystem().copySync(FeederTest.class.getClassLoader().getResource("aaf-test").getPath(),
//				importTmpFolder.getRoot().getAbsolutePath(), true);
//		JsonObject neo4jConfig = new JsonObject()
//			.put("address", NEO4J_PERSISTOR)
//			.put("datastore-path", neo4jTmpFolder.getRoot().getAbsolutePath())
//			.put("neo4j", new JsonObject()
//					.put("node_keys_indexable", "externalId")
//					.put("node_auto_indexing", "true"));
//		container.deployModule("fr.wseduc~mod-neo4j-persistor~1.6.0", neo4jConfig, 1,
//				new Handler<AsyncResult><String>() {
//			@Override
//			public void handle(AsyncResult<String> event) {
//				JsonObject config = new JsonObject()
//						.put("neo4j-address", NEO4J_PERSISTOR)
//						.put("feeder", "AAF")
//						.put("import-files", importTmpFolder.getRoot().getAbsolutePath())
//						.put("delete-user-delay", 10000l)
//						.put("pre-delete-user-delay", 1000l)
//						.put("delete-cron", "0 */1 * * * ? *")
//						.put("pre-delete-cron", "0 */1 * * * ? *");
//				container.deployModule(System.getProperty("vertx.modulename"), config, 1,
//						new Handler<AsyncResult><String>() {
//							public void handle(AsyncResult<String> ar) {
//								if (ar.succeeded()) {
//									eb = vertx.eventBus();
//									neo4j = new Neo4j(eb, NEO4J_PERSISTOR);
//									neo4jDeploymentId = ar.result();
//									neo4j.execute("CREATE (n:DeleteGroup {externalId :'DeleteGroup'})", null,
//											new Handler<Message<JsonObject>>() {
//										@Override
//										public void handle(Message<JsonObject> event) {
//											FeederTest.super.start();
//										}
//									});
//								} else {
//									ar.cause().printStackTrace();
//								}
//							}
//						});
//			}
//		});
//	}
//
//	@Override
//	public void stop() {
//		if (importTmpFolder != null) {
//			importTmpFolder.delete();
//		}
//		if (neo4jDeploymentId != null) {
//			container.undeployModule(neo4jDeploymentId, new Handler<AsyncResult<Void>>() {
//				@Override
//				public void handle(AsyncResult<Void> event) {
//					vertx.setTimer(1000, new Handler<Long>() {
//						@Override
//						public void handle(Long event) {
//							if (neo4jTmpFolder != null) {
//								neo4jTmpFolder.delete();
//							}
//							FeederTest.super.stop();
//						}
//					});
//				}
//			});
//		} else {
//			if (neo4jTmpFolder != null) {
//				neo4jTmpFolder.delete();
//			}
//			super.stop();
//		}
//	}
//
//	@Test
//	public void testAAF() {
//		importAAF(new Handler<Void>() {
//			@Override
//			protected void handle() {
//				addStructure(new Handler<String>() {
//					@Override
//					public void handle(final String structureId) {
//						addManualUser(structureId, "ok");
//						transition(new Handler<Void>() {
//							@Override
//							protected void handle() {
//								deleteUsersInImport();
//								importUpdate(new Handler<Void>() {
//									@Override
//									protected void handle() {
//										testComplete();
//									}
//								});
//							}
//						});
//					}
//				});
//			}
//		});
//	}
//
//	private void addStructure(final Handler<String> handler) {
//		JsonObject action = new JsonObject().put("action", "manual-create-structure")
//				.put("data", new JsonObject().put("name", "bla"));
//		eb.send(ENTCORE_FEEDER, action,new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> event) {
//				assertEquals("ok", event.body().getString("status"));
//				JsonObject r = event.body().getJsonArray("result").get(0);
//				handler.handle(r.getString("id"));
//			}
//		});
//
//	}
//
//	private void addManualUser(String structureId, final String expectedStatus) {
//		JsonObject j = new JsonObject()
//				.put("action", "manual-create-user")
//				.put("profile", "Personnel")
//				.put("structureId", structureId)
//				.put("data", new JsonObject()
//						.put("firstName", "blip").putString("lastName", "blop"));
//		eb.send(ENTCORE_FEEDER, j, new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> event) {
//				assertEquals(expectedStatus, event.body().getString("status"));
//			}
//		});
//	}
//
//	private void deleteUsersInImport() {
//		vertx.fileSystem().deleteSync(importTmpFolder.getRoot().getAbsolutePath() + File.separator +
//				"FULL_ENTTSSERVICES_Complet_20130117_Eleve_0000.xml");
//		vertx.fileSystem().deleteSync(importTmpFolder.getRoot().getAbsolutePath() + File.separator +
//				"FULL_ENTTSSERVICES_Complet_20130117_PersEducNat_0000.xml");
//	}
//
//	private void transition(final Handler<Void> handler) {
//		eb.registerHandler("user.repository", new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				String action = message.body().getString("action", "");
//				switch (action) {
//					case "delete-groups" :
//						JsonArray groups = message.body().getJsonArray("old-groups", new JsonArray());
//						assertEquals(177 * 5 + 167, groups.size());
//						String countQuery =
//								"MATCH (s:Structure) " +
//								"OPTIONAL MATCH s<-[:BELONGS]-(c:Class) " +
//								"OPTIONAL MATCH s<-[:DEPENDS]-(f:FunctionalGroup) " +
//								"OPTIONAL MATCH s<-[:DEPENDS*2]-(p:ProfileGroup) " +
//								"RETURN count(distinct s) as nbStructures, count(distinct c) as nbClasses, " +
//								"count(distinct p) as nbClassProfileGroups, count(distinct f) as nbFunctionalGroups";
//						neo4j.execute(countQuery, new JsonObject(), new Handler<Message<JsonObject>>() {
//							@Override
//							public void handle(Message<JsonObject> event) {
//								assertEquals("ok", event.body().getString("status"));
//								JsonObject r = event.body().getJsonArray("result").get(0);
//								assertEquals(11, (int) r.getInteger("nbStructures", 0));
//								assertEquals(0, (int) r.getInteger("nbClasses", 0));
//								assertEquals(0, (int) r.getInteger("nbFunctionalGroups", 0));
//								assertEquals(0, (int) r.getInteger("nbClassProfileGroups", 0));
//								handler.handle(null);
//							}
//						});
//						eb.unregisterHandler("user.repository", this);
//						break;
//					default:
//						fail("invalid.action");
//				}
//			}
//		}, new Handler<AsyncResult><Void>() {
//			@Override
//			public void handle(AsyncResult<Void> event) {
//				if (event.succeeded()) {
//					eb.send(ENTCORE_FEEDER, new JsonObject().put("action", "transition"),
//							new Handler<Message<JsonObject>>() {
//						@Override
//						public void handle(Message<JsonObject> message) {
//							assertEquals("ok", message.body().getString("status"));
//						}
//					});
//				} else {
//					fail();
//				}
//			}
//		});
//	}
//
//	private void importAAF(final Handler<Void> handler) {
//		eb.send(ENTCORE_FEEDER, new JsonObject().put("action", "import"), new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				assertEquals("ok", message.body().getString("status"));
//				String countQuery =
//						"MATCH (:User) WITH count(*) as nbUsers " +
//						"MATCH (:Structure) WITH count(*) as nbStructures, nbUsers " +
//						"MATCH (:Class) WITH nbUsers, nbStructures, count(*) as nbClasses " +
//						"MATCH (:FunctionalGroup) WITH nbUsers, nbStructures, nbClasses, count(*) as nbFunctionalGroups " +
//						"MATCH (:ProfileGroup) " +
//						"RETURN nbUsers, nbStructures, nbClasses, nbFunctionalGroups, count(*) as nbProfileGroups";
//				neo4j.execute(countQuery, new JsonObject(), new Handler<Message<JsonObject>>() {
//					@Override
//					public void handle(Message<JsonObject> event) {
//						assertEquals("ok", event.body().getString("status"));
//						JsonObject r = event.body().getJsonArray("result").get(0);
//						assertEquals(13295, (int) r.getInteger("nbUsers", 0));
//						assertEquals(10, (int) r.getInteger("nbStructures", 0));
//						assertEquals(177, (int) r.getInteger("nbClasses", 0));
//						assertEquals(167, (int) r.getInteger("nbFunctionalGroups", 0));
//						assertEquals(177 * 5 + 10 * 5 + 5, (int) r.getInteger("nbProfileGroups", 0));
//						handler.handle(null);
//					}
//				});
//			}
//		});
//	}
//
//	private void importUpdate(final Handler<Void> handler) {
//		eb.registerHandler("user.repository", new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				String action = message.body().getString("action", "");
//				switch (action) {
//					case "delete-users":
//						JsonArray users = message.body().getJsonArray("old-users", new JsonArray());
//						assertEquals(11769, users.size());
//						String type = users.<JsonObject>get(0).getString("type");
//						assertNotNull(type);
//						assertTrue("Personnel".equals(type) || "Teacher".equals(type) ||
//								"Student".equals(type) || "Relative".equals(type));
//						String countQuery = "MATCH (:User) RETURN count(*) as nbUsers ";
//						neo4j.execute(countQuery, new JsonObject(), new Handler<Message<JsonObject>>() {
//							@Override
//							public void handle(Message<JsonObject> event) {
//								assertEquals("ok", event.body().getString("status"));
//								JsonObject r = event.body().getJsonArray("result").get(0);
//								assertEquals(13295 - 11769 + 1, (int) r.getInteger("nbUsers", 0));
//								handler.handle(null);
//							}
//						});
//						break;
//					default:
//						fail("invalid.action : " + action);
//				}
//			}
//		}, new Handler<AsyncResult><Void>() {
//			@Override
//			public void handle(AsyncResult<Void> event) {
//				if (event.succeeded()) {
//					eb.send(ENTCORE_FEEDER, new JsonObject().put("action", "import"),
//							new Handler<Message<JsonObject>>() {
//								@Override
//								public void handle(Message<JsonObject> message) {
//									assertEquals("ok", message.body().getString("status"));
//									String countQuery =
//											"MATCH (u:User) " +
//											"WHERE HAS(u.disappearanceDate) " +
//											"RETURN count(*) as nbUsers ";
//									neo4j.execute(countQuery, new JsonObject(), new Handler<Message<JsonObject>>() {
//										@Override
//										public void handle(Message<JsonObject> event) {
//											assertEquals("ok", event.body().getString("status"));
//											JsonObject r = event.body().getJsonArray("result").get(0);
//											assertEquals(11769, (int) r.getInteger("nbUsers", 0));
//										}
//									});
//								}
//							});
//				} else {
//					fail();
//				}
//			}
//		});
//	}
//
//}
