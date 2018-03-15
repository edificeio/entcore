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
//package org.entcore.communication.test.integration.java;
//
//import org.entcore.common.http.request.JsonHttpServerRequest;
//import org.entcore.common.http.response.JsonHttpResponse;
//import org.entcore.common.neo4j.Neo4j;
//import org.entcore.common.neo4j.StatementsBuilder;
//import org.entcore.common.user.UserUtils;
//import org.entcore.communication.controllers.CommunicationController;
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
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.Scanner;
//
//import static io.vertx.testtools.VertxAssert.*;
//import static io.vertx.testtools.VertxAssert.assertEquals;
//
//public class CommunicationTest extends TestVerticle {
//	public static final String NEO4J_PERSISTOR = "wse.neo4j.persistor";
//	public static final String ENTCORE_FEEDER = "entcore.feeder";
//	public static final String ENTCORE_COMMUNICATION = "wse.communication";
//	public static final String ENTCORE_COMMUNICATION_USERS = "wse.communication.users";
//	private String neo4jDeploymentId;
//	private TemporaryFolder neo4jTmpFolder;
//	private EventBus eb;
//	private Neo4j neo4j;
//	private CommunicationController communicationController;
//
//	@Override
//	public void start() {
//		neo4jTmpFolder = new TemporaryFolder();
//		try {
//			neo4jTmpFolder.create();
//		} catch (IOException e) {
//			e.printStackTrace();
//			return;
//		}
//		final String moduleName = System.getProperty("vertx.modulename");
//		final String version = moduleName.substring(moduleName.lastIndexOf('~') + 1);
//		final String p = CommunicationTest.class.getClassLoader().getResource("be1d-test").getPath();
//		JsonObject neo4jConfig = new JsonObject()
//				.put("address", NEO4J_PERSISTOR)
//				.put("datastore-path", neo4jTmpFolder.getRoot().getAbsolutePath())
//				.put("neo4j", new JsonObject()
//						.put("node_keys_indexable", "externalId")
//						.put("node_auto_indexing", "true"));
//		container.deployModule("fr.wseduc~mod-neo4j-persistor~1.3-SNAPSHOT", neo4jConfig, 1,
//				new Handler<AsyncResult><String>() {
//					@Override
//					public void handle(AsyncResult<String> event) {
//						JsonObject config = new JsonObject()
//								.put("neo4j-address", NEO4J_PERSISTOR)
//								.put("feeder", "BE1D")
//								.put("import-files", p);
//						container.deployModule("org.entcore~feeder~" + version, config, 1,
//								new Handler<AsyncResult><String>() {
//									public void handle(AsyncResult<String> ar) {
//										if (ar.succeeded()) {
//											eb = vertx.eventBus();
//											neo4j = Neo4j.getInstance();
//											neo4j.init(eb, NEO4J_PERSISTOR);
//											neo4jDeploymentId = ar.result();
//											neo4j.execute("CREATE (n:DeleteGroup {externalId :'DeleteGroup'})",
//													(JsonObject) null, new Handler<Message<JsonObject>>() {
//														@Override
//														public void handle(Message<JsonObject> event) {
//															try {
//																loadCommunicationModule();
//															} catch (IOException e) {
//																e.printStackTrace();
//															}
//														}
//													});
//										} else {
//											ar.cause().printStackTrace();
//										}
//									}
//								});
//					}
//				});
//	}
//
//	private void loadCommunicationModule() throws IOException {
//		InputStream is = CommunicationTest.class.getClassLoader().getResourceAsStream("initDefaultComRules.json");
//		Scanner s = new Scanner(is).useDelimiter("\\A");
//		String json = s.hasNext() ? s.next() : "{}";
//		is.close();
//		s.close();
//		config.put("initDefaultCommunicationRules", new JsonObject(json));
//		communicationController = new CommunicationController();
//		communicationController.init(vertx, container, null, null);
//		vertx.eventBus().localConsumer(ENTCORE_COMMUNICATION, new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				communicationController.communicationEventBusHandler(message);
//			}
//		});
//		vertx.eventBus().localConsumer(ENTCORE_COMMUNICATION_USERS, new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				communicationController.visibleUsers(message);
//			}
//		});
//		CommunicationTest.super.start();
//	}
//
//	@Override
//	public void stop() {
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
//							CommunicationTest.super.stop();
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
//	public void test() {
//		importWithDefaultComRules(new Handler<String>() {
//			@Override
//			public void handle(final String structureId) {
//				checkComUser(new Handler<Void>() {
//					@Override
//					protected void handle() {
//						personaliseComRules(new Handler<Void>() {
//							@Override
//							protected void handle() {
//								checkComUser2(new Handler<Void>() {
//									@Override
//									protected void handle() {
//										initAndApplyDefaultCommunicationRules(structureId, new Handler<Void>() {
//											@Override
//											protected void handle() {
//												checkComUser2(new Handler<Void>() {
//													@Override
//													protected void handle() {
//														testComplete();
//													}
//												});
//											}
//										});
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
//	private void importWithDefaultComRules(final Handler<String> handler) {
//		importWithoutCom(new Handler<String>() {
//			@Override
//			public void handle(final String structureId) {
//				initDefaultComRules(structureId, new Handler<Void>() {
//					@Override
//					protected void handle() {
//						applyDefaultComRules(structureId, new Handler<Void>() {
//							@Override
//							protected void handle() {
//								handler.handle(structureId);
//							}
//						});
//					}
//				});
//			}
//		});
//	}
//
//	private void checkComUser(final Handler<Void> handler) {
//		userCanCommunicateWith("rachelle.pires", "melanie.jean", true, new Handler<Void>() {
//			@Override
//			protected void handle() {
//				userCanCommunicateWith("melanie.jean", "rachelle.pires", false, new Handler<Void>() {
//					@Override
//					protected void handle() {
//						userCanCommunicateWith("rachelle.pires", "charlotte.eneman", true, new Handler<Void>() {
//							@Override
//							protected void handle() {
//								userCanCommunicateWith("charlotte.eneman", "rachelle.pires", true, new Handler<Void>() {
//									@Override
//									protected void handle() {
//										handler.handle(null);
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
//	private void personaliseComRules(final Handler<Void> handler) {
//		String query = "MATCH (g:ProfileGroup) WHERE g.name IN {groups} RETURN g.id as id, g.name as name ";
//		JsonObject params = new JsonObject().put("groups", new fr.wseduc.webutils.collections.JsonArray()
//				.add("TPS-Student").add("TPS-Teacher")
//				.add("Ecole primaire Emile Zola-Student").add("Ecole primaire Emile Zola-Teacher"));
//		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				assertEquals("ok", message.body().getString("status"));
//				JsonArray res = message.body().getJsonArray("result");
//				assertEquals(4, res.size());
//				final JsonObject j = new JsonObject();
//				for (Object o : res) {
//					JsonObject json = (JsonObject) o;
//					j.put(json.getString("name"), json.getString("id"));
//				}
//				JsonObject req = new JsonObject().put("params", new JsonObject()
//						.put("startGroupId", j.getString("TPS-Teacher"))
//						.put("endGroupId", j.getString("TPS-Student"))
//				);
//				JsonHttpResponse resp = new JsonHttpResponse(new Handler<String>() {
//					@Override
//					public void handle(String s) {
//						JsonObject req = new JsonObject().put("params", new JsonObject()
//								.put("startGroupId", j.getString("Ecole primaire Emile Zola-Teacher"))
//								.put("endGroupId", j.getString("Ecole primaire Emile Zola-Student"))
//						);
//						JsonHttpResponse resp = new JsonHttpResponse(new Handler<String>() {
//							@Override
//							public void handle(String s) {
//								JsonObject req = new JsonObject().put("params", new JsonObject()
//										.put("groupId", j.getString("TPS-Teacher"))
//								);
//								JsonHttpResponse resp = new JsonHttpResponse(new Handler<String>() {
//									@Override
//									public void handle(String s) {
//										handler.handle(null);
//									}
//								});
//								communicationController.addLinksWithUsers(new JsonHttpServerRequest(req, resp));
//							}
//						});
//						communicationController.removeLink(new JsonHttpServerRequest(req, resp));
//					}
//				});
//				communicationController.addLink(new JsonHttpServerRequest(req, resp));
//
//
//			}
//		});
//	}
//
//	private void checkComUser2(final Handler<Void> handler) {
//		userCanCommunicateWith("rachelle.pires", "melanie.jean", false, new Handler<Void>() {
//			@Override
//			protected void handle() {
//				userCanCommunicateWith("melanie.jean", "rachelle.pires", false, new Handler<Void>() {
//					@Override
//					protected void handle() {
//						userCanCommunicateWith("rachelle.pires", "charlotte.eneman", true, new Handler<Void>() {
//							@Override
//							protected void handle() {
//								userCanCommunicateWith("charlotte.eneman", "rachelle.pires", true, new Handler<Void>() {
//									@Override
//									protected void handle() {
//										handler.handle(null);
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
//	private void importWithoutCom(final Handler<String> handler) {
//		eb.send(ENTCORE_FEEDER, new JsonObject().put("action", "import"), new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				assertEquals("ok", message.body().getString("status"));
//				StatementsBuilder s = new StatementsBuilder();
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.communiqueWith) AND LENGTH(g.communiqueWith) <> 0 RETURN COUNT(*) as nbCW ");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.users) RETURN COUNT(*) as nbU ");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.relativeCommuniqueStudent) RETURN COUNT(*) as nbR");
//				s.add("MATCH ()-[:COMMUNIQUE]->() RETURN COUNT(*) as nbC ");
//				s.add("MATCH ()-[:COMMUNIQUE_DIRECT]->() RETURN count(*) as nbCD");
//				s.add("MATCH (s:Structure) RETURN s.id as structureId");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.communiqueWith) RETURN COUNT(*) as nbCW ");
//				neo4j.executeTransaction(s.build(), null, true, new Handler<Message<JsonObject>>() {
//					@Override
//					public void handle(Message<JsonObject> event) {
//						assertEquals("ok", event.body().getString("status"));
//						JsonArray r = event.body().getJsonArray("results");
//						final String structureId = ((JsonObject) ((JsonArray) r.get(5)).get(0)).getString("structureId");
//						assertNotNull(structureId);
//						assertEquals(0, (int) ((JsonObject) ((JsonArray) r.get(6)).get(0)).getInteger("nbCW", 1));
//						assertEquals(0, (int) ((JsonObject) ((JsonArray) r.get(0)).get(0)).getInteger("nbCW", 1));
//						assertEquals(0, (int) ((JsonObject) ((JsonArray) r.get(1)).get(0)).getInteger("nbU", 1));
//						assertEquals(0, (int) ((JsonObject) ((JsonArray) r.get(2)).get(0)).getInteger("nbR", 1));
//						assertEquals(0, (int) ((JsonObject) ((JsonArray) r.get(3)).get(0)).getInteger("nbC", 1));
//						assertEquals(0, (int) ((JsonObject) ((JsonArray) r.get(4)).get(0)).getInteger("nbCD", 1));
//						handler.handle(structureId);
//					}
//				});
//			}
//		});
//	}
//
//
//	private void initDefaultComRules(String structureId, final Handler<Void> handler) {
//		eb.send(ENTCORE_COMMUNICATION, new JsonObject()
//				.put("action", "initDefaultCommunicationRules")
//				.put("schoolIds", new fr.wseduc.webutils.collections.JsonArray().add(structureId)),
//				new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				assertTrue(message.body().size() == 0);
//				StatementsBuilder s = new StatementsBuilder();
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.communiqueWith) AND LENGTH(g.communiqueWith) <> 0 RETURN COUNT(*) as nbCW ");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.users) RETURN COUNT(*) as nbU ");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.relativeCommuniqueStudent) RETURN COUNT(*) as nbR");
//				s.add("MATCH ()-[:COMMUNIQUE]->() RETURN COUNT(*) as nbC ");
//				s.add("MATCH ()-[:COMMUNIQUE_DIRECT]->() RETURN count(*) as nbCD");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.communiqueWith) RETURN COUNT(*) as nbCW ");
//				neo4j.executeTransaction(s.build(), null, true, new Handler<Message<JsonObject>>() {
//					@Override
//					public void handle(Message<JsonObject> event) {
//						assertEquals("ok", event.body().getString("status"));
//						JsonArray r = event.body().getJsonArray("results");
//						assertEquals(44, (int) ((JsonObject) ((JsonArray) r.get(5)).get(0)).getInteger("nbCW", 1));
//						assertEquals(22, (int) ((JsonObject) ((JsonArray) r.get(0)).get(0)).getInteger("nbCW", 1));
//						assertEquals(44, (int) ((JsonObject) ((JsonArray) r.get(1)).get(0)).getInteger("nbU", 1));
//						assertEquals(10, (int) ((JsonObject) ((JsonArray) r.get(2)).get(0)).getInteger("nbR", 1));
//						assertEquals(0, (int) ((JsonObject) ((JsonArray) r.get(3)).get(0)).getInteger("nbC", 1));
//						assertEquals(0, (int) ((JsonObject) ((JsonArray) r.get(4)).get(0)).getInteger("nbCD", 1));
//						handler.handle(null);
//					}
//				});
//			}
//		});
//	}
//
//	private void applyDefaultComRules(String structureId, final Handler<Void> handler) {
//		eb.send(ENTCORE_COMMUNICATION, new JsonObject()
//				.put("action", "setMultipleDefaultCommunicationRules")
//				.put("schoolIds", new fr.wseduc.webutils.collections.JsonArray().add(structureId)),
//				new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				assertTrue(message.body().size() == 0);
//				StatementsBuilder s = new StatementsBuilder();
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.communiqueWith) AND LENGTH(g.communiqueWith) <> 0 RETURN COUNT(*) as nbCW ");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.users) RETURN COUNT(*) as nbU ");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.relativeCommuniqueStudent) RETURN COUNT(*) as nbR");
//				s.add("MATCH ()-[:COMMUNIQUE]->() RETURN COUNT(*) as nbC ");
//				s.add("MATCH ()-[:COMMUNIQUE_DIRECT]->() RETURN count(*) as nbCD");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.communiqueWith) RETURN COUNT(*) as nbCW ");
//				neo4j.executeTransaction(s.build(), null, true, new Handler<Message<JsonObject>>() {
//					@Override
//					public void handle(Message<JsonObject> event) {
//						assertEquals("ok", event.body().getString("status"));
//						JsonArray r = event.body().getJsonArray("results");
//						assertEquals(44, (int) ((JsonObject) ((JsonArray) r.get(5)).get(0)).getInteger("nbCW", 1));
//						assertEquals(22, (int) ((JsonObject) ((JsonArray) r.get(0)).get(0)).getInteger("nbCW", 1));
//						assertEquals(44, (int) ((JsonObject) ((JsonArray) r.get(1)).get(0)).getInteger("nbU", 1));
//						assertEquals(10, (int) ((JsonObject) ((JsonArray) r.get(2)).get(0)).getInteger("nbR", 1));
//						assertEquals(2286, (int) ((JsonObject) ((JsonArray) r.get(3)).get(0)).getInteger("nbC", 1));
//						assertEquals(491, (int) ((JsonObject) ((JsonArray) r.get(4)).get(0)).getInteger("nbCD", 1));
//						handler.handle(null);
//					}
//				});
//			}
//		});
//	}
//
//	private void userCanCommunicateWith(String fromUserLogin, final String toUserLogin, final boolean assertion,
//			final Handler<Void> handler) {
//		String query = "MATCH (u:User {login : {login}}) RETURN u.id as id";
//		JsonObject params = new JsonObject().put("login", fromUserLogin);
//		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> event) {
//				assertEquals("ok", event.body().getString("status"));
//				JsonArray r = event.body().getJsonArray("result");
//				final String fromId = ((JsonObject) r.get(0)).getString("id");
//				UserUtils.findVisibleUsers(eb, fromId, false, new Handler<JsonArray>() {
//					@Override
//					public void handle(JsonArray objects) {
//						boolean ok = false;
//						for (Object o : objects) {
//							if (toUserLogin.equals(((JsonObject) o).getString("login"))) {
//								ok = true;
//								break;
//							}
//						}
//						assertEquals(assertion, ok);
//						handler.handle(null);
//					}
//				});
//			}
//		});
//	}
//
//	private void initAndApplyDefaultCommunicationRules(String structureId, final Handler<Void> handler) {
//		eb.send(ENTCORE_COMMUNICATION, new JsonObject()
//				.put("action", "initAndApplyDefaultCommunicationRules")
//				.put("schoolIds", new fr.wseduc.webutils.collections.JsonArray().add(structureId)),
//				new Handler<Message<JsonObject>>() {
//			@Override
//			public void handle(Message<JsonObject> message) {
//				assertTrue(message.body().size() == 0);
//				StatementsBuilder s = new StatementsBuilder();
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.communiqueWith) AND LENGTH(g.communiqueWith) <> 0 RETURN COUNT(*) as nbCW ");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.users) RETURN COUNT(*) as nbU ");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.relativeCommuniqueStudent) RETURN COUNT(*) as nbR");
//				s.add("MATCH ()-[:COMMUNIQUE]->() RETURN COUNT(*) as nbC ");
//				s.add("MATCH ()-[:COMMUNIQUE_DIRECT]->() RETURN count(*) as nbCD");
//				s.add("MATCH (g:ProfileGroup) WHERE HAS(g.communiqueWith) RETURN COUNT(*) as nbCW ");
//				neo4j.executeTransaction(s.build(), null, true, new Handler<Message<JsonObject>>() {
//					@Override
//					public void handle(Message<JsonObject> event) {
//						assertEquals("ok", event.body().getString("status"));
//						JsonArray r = event.body().getJsonArray("results");
//						assertEquals(44, (int) ((JsonObject) ((JsonArray) r.get(5)).get(0)).getInteger("nbCW", 1));
//						assertEquals(23, (int) ((JsonObject) ((JsonArray) r.get(0)).get(0)).getInteger("nbCW", 1));
//						assertEquals(44, (int) ((JsonObject) ((JsonArray) r.get(1)).get(0)).getInteger("nbU", 1));
//						assertEquals(10, (int) ((JsonObject) ((JsonArray) r.get(2)).get(0)).getInteger("nbR", 1));
//						assertEquals(2288, (int) ((JsonObject) ((JsonArray) r.get(3)).get(0)).getInteger("nbC", 1));
//						assertEquals(491, (int) ((JsonObject) ((JsonArray) r.get(4)).get(0)).getInteger("nbCD", 1));
//						handler.handle(null);
//					}
//				});
//			}
//		});
//	}
//
//}
>>>>>>> 7eaaaee... [Evo] migration vertx3
