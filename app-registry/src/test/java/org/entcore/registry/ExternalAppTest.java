package org.entcore.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.entcore.registry.services.impl.DefaultExternalApplicationService;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ExternalAppTest {
    private static final TestHelper test = TestHelper.helper();

    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    static DefaultExternalApplicationService service = new DefaultExternalApplicationService(5);

    static Map<String, String> variables = new HashMap<>();

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        Async async = context.async();
        test.database().initNeo4j(context, neo4jContainer);
        test.directory().createStructure("structure 1", "111111A").compose(structId -> {
            variables.put("structure1.id", structId);
            return test.registry().createApplicationUser("test", "address", new JsonArray(), structId)
                    .compose(appId -> {
                        variables.put("application1.id", appId);
                        return test.registry().createRole("role1", structId).compose(roleId -> {
                            variables.put("role1.id", roleId);
                            return test.registry().createActionWorkflow("action1", appId).compose(actionId -> {
                                return test.registry().attachActionToRole(actionId, roleId);
                            });
                        });
                    });
        }).onComplete(res -> {
            if (res.failed()) {
                res.cause().printStackTrace();
            }
            context.assertTrue(res.succeeded());
            async.complete();
        });
    }

    @Test
    public void testExternalAppServiceShouldMassAuthorizeToTeacherStructure(TestContext context) {
        final Async async = context.async();
        final List<String> profiles = new ArrayList<>();
        profiles.add("Teacher1");
        final String structureId = variables.get("structure1.id");
        final String applicationId = variables.get("application1.id");
        final String role1Id = variables.get("role1.id");
        test.directory().createProfileGroup("Teacher1").compose(groupId -> {
            variables.put("group1.id", groupId);
            return test.directory().attachGroupToStruct(groupId, structureId);
        }).onComplete(res0 -> {
            context.assertTrue(res0.succeeded());
            final String group1Id = variables.get("group1.id");
            service.massAuthorize(structureId, applicationId, profiles, res -> {
                context.assertTrue(res.isRight());
                test.directory().groupHasRole(group1Id, role1Id).onComplete(res2 -> {
                    context.assertTrue(res2.succeeded());
                    context.assertTrue(res2.result());
                    context.assertEquals(1, res.right().getValue().getInteger("nbCreation"));
                    // unauthorize
                    service.massUnauthorize(structureId, applicationId, profiles, resU -> {
                        test.directory().groupHasRole(group1Id, role1Id).onComplete(resU1 -> {
                            context.assertTrue(resU1.succeeded());
                            context.assertFalse(resU1.result());
                            context.assertEquals(1, resU.right().getValue().getInteger("nbDeletion"));
                            async.complete();
                        });
                    });
                });
            });
        });
    }

    @Test
    public void testExternalAppServiceShouldMassAuthorizeToAdmlSubStructure(TestContext context) {
        final Async async = context.async();
        final String structureId = variables.get("structure1.id");
        final String applicationId = variables.get("application1.id");
        final String role1Id = variables.get("role1.id");
        test.directory().createStructure("structure2", "222222A").compose(subStructureId -> {
            return test.directory().attachSubstructure(structureId, subStructureId).compose(res -> {
                return test.directory().addAdminLocalFunctionToStructure(subStructureId).map(e -> {
                    variables.put("functionGroup1.id", e);
                    return e;
                });
            }).map(subStructureId);
        }).onComplete(res0 -> {
            context.assertTrue(res0.succeeded());
            final List<String> profiles = new ArrayList<>();
            profiles.add("AdminLocal");
            service.massAuthorize(structureId, applicationId, profiles, res -> {
                context.assertTrue(res.isRight());
                final String fg1 = variables.get("functionGroup1.id");
                test.directory().functionGroupHasRole(fg1, role1Id).onComplete(res2 -> {
                    context.assertTrue(res2.succeeded());
                    context.assertTrue(res2.result());
                    context.assertTrue(1 <= res.right().getValue().getInteger("nbCreation"));
                    // unauthorize
                    service.massUnauthorize(structureId, applicationId, profiles, resU -> {
                        test.directory().functionGroupHasRole(fg1, role1Id).onComplete(resU1 -> {
                            context.assertTrue(resU1.succeeded());
                            context.assertFalse(resU1.result());
                            context.assertTrue(1 <= resU.right().getValue().getInteger("nbDeletion"));
                            async.complete();
                        });
                    });
                });
            });
        });

    }

    @Test
    public void testExternalAppServiceShouldMassAuthorizeToStudentSubStructure(TestContext context) {
        final Async async = context.async();
        final String structureId = variables.get("structure1.id");
        final String applicationId = variables.get("application1.id");
        test.directory().createStructure("structure3", "333333A").compose(subStructureId -> {
            return test.directory().createProfileGroup("Student1").compose(groupId -> {
                variables.put("group2.id", groupId);
                return test.directory().attachGroupToStruct(groupId, subStructureId);
            }).compose(resx -> {
                return test.directory().attachSubstructure(structureId, subStructureId);
            });
        }).onComplete(res0 -> {
            context.assertTrue(res0.succeeded());
            final String groupId = variables.get("group2.id");
            final String role1Id = variables.get("role1.id");
            final List<String> profiles = new ArrayList<>();
            profiles.add("Student1");
            service.massAuthorize(structureId, applicationId, profiles, res -> {
                context.assertTrue(res.isRight());
                test.directory().groupHasRole(groupId, role1Id).onComplete(res2 -> {
                    context.assertTrue(res2.succeeded());
                    context.assertTrue(res2.result());
                    context.assertEquals(1, res.right().getValue().getInteger("nbCreation"));
                    // unauthorize
                    service.massUnauthorize(structureId, applicationId, profiles, resU -> {
                        test.directory().groupHasRole(groupId, role1Id).onComplete(resU1 -> {
                            context.assertTrue(resU1.succeeded());
                            context.assertFalse(resU1.result());
                            context.assertEquals(1, resU.right().getValue().getInteger("nbDeletion"));
                            async.complete();
                        });
                    });
                });
            });
        });

    }

    // @Test
    // public void testExternalAppServiceShouldMassAuthorizeToBothAdmlAndParentsSubStructure(TestContext context) {
    //     final Async async = context.async();
    //     final String structureId = variables.get("structure1.id");
    //     final String applicationId = variables.get("application1.id");
    //     test.directory().createStructure("structure4", "4444A").compose(subStructureId -> {
    //         return test.directory().createProfileGroup("Parent1").compose(groupId -> {
    //             variables.put("group3.id", groupId);
    //             return test.directory().attachGroupToStruct(groupId, subStructureId);
    //         }).compose(resx -> {
    //             return test.directory().attachSubstructure(structureId, subStructureId);
    //         });
    //     }).compose(res0 -> {
    //         return test.directory().createStructure("structure4", "444444A").compose(subStructureId -> {
    //             return test.directory().attachSubstructure(structureId, subStructureId).compose(res -> {
    //                 return test.directory().addAdminLocalFunctionToStructure(subStructureId).map(e -> {
    //                     variables.put("functionGroup2.id", e);
    //                     return e;
    //                 });
    //             }).map(subStructureId);
    //         });
    //     }).onComplete(res0 -> {
    //         context.assertTrue(res0.succeeded());
    //         final String groupId = variables.get("group3.id");
    //         final String fGroupId = variables.get("functionGroup2.id");
    //         final String role1Id = variables.get("role1.id");
    //         final List<String> profiles = new ArrayList<>();
    //         profiles.add("Parent1");
    //         profiles.add("AdminLocal");
    //         service.massAuthorize(structureId, applicationId, profiles, res -> {
    //             context.assertTrue(res.isRight());
    //             test.directory().functionGroupHasRole(fGroupId, role1Id).onComplete(res2a -> {
    //                 context.assertTrue(res2a.succeeded());
    //                 context.assertTrue(res2a.result());
    //                 test.directory().groupHasRole(groupId, role1Id).onComplete(res2b -> {
    //                     context.assertTrue(res2b.succeeded());
    //                     context.assertTrue(res2b.result());
    //                     context.assertEquals(2, res.right().getValue().getInteger("nbCreation"));
    //                     // unauthorize
    //                     service.massUnauthorize(structureId, applicationId, profiles, resU -> {
    //                         test.directory().groupHasRole(groupId, role1Id).onComplete(resU1 -> {
    //                             context.assertTrue(resU1.succeeded());
    //                             context.assertFalse(resU1.result());
    //                             context.assertEquals(2, resU.right().getValue().getInteger("nbDeletion"));
    //                             async.complete();
    //                         });
    //                     });
    //                 });
    //             });
    //         });
    //     });

    // }

    @Test
    public void testExternalAppServiceShouldNotMassAuthorizeBecauseOfBadFilter(TestContext context) {
        final Async async = context.async();
        final String structureId = variables.get("structure1.id");
        final String applicationId = variables.get("application1.id");
        test.directory().createStructure("structure5", "555555A").compose(subStructureId -> {
            return test.directory().createProfileGroup("Parent4").compose(groupId -> {
                variables.put("group4.id", groupId);
                return test.directory().attachGroupToStruct(groupId, subStructureId);
            }).compose(resx -> {
                return test.directory().attachSubstructure(structureId, subStructureId);
            });
        }).compose(res0 -> {
            return test.directory().createStructure("structure6", "666666A").compose(subStructureId -> {
                return test.directory().attachSubstructure(structureId, subStructureId).compose(res -> {
                    return test.directory().addAdminLocalFunctionToStructure(subStructureId).map(e -> {
                        variables.put("functionGroup3.id", e);
                        return e;
                    });
                }).map(subStructureId);
            });
        }).onComplete(res0 -> {
            context.assertTrue(res0.succeeded());
            final String groupId = variables.get("group4.id");
            final String fGroupId = variables.get("functionGroup3.id");
            final String role1Id = variables.get("role1.id");
            final List<String> profiles = new ArrayList<>();
            profiles.add("Teacher");
            service.massAuthorize(structureId, applicationId, profiles, res -> {
                context.assertTrue(res.isRight());
                test.directory().functionGroupHasRole(fGroupId, role1Id).onComplete(res2a -> {
                    context.assertTrue(res2a.succeeded());
                    context.assertFalse(res2a.result());
                    test.directory().groupHasRole(groupId, role1Id).onComplete(res2b -> {
                        context.assertTrue(res2b.succeeded());
                        context.assertFalse(res2b.result());
                        context.assertEquals(0, res.right().getValue().getInteger("nbCreation"));
                        // unauthorize
                        service.massUnauthorize(structureId, applicationId, profiles, resU -> {
                            test.directory().groupHasRole(groupId, role1Id).onComplete(resU1 -> {
                                context.assertTrue(resU1.succeeded());
                                context.assertFalse(resU1.result());
                                context.assertEquals(0, resU.right().getValue().getInteger("nbDeletion"));
                                async.complete();
                            });
                        });

                    });
                });
            });
        });

    }

    @Test
    public void testExternalAppServiceShouldNotMassAuthorizeTwice(TestContext context) {
        final Async async = context.async();
        final String structureId = variables.get("structure1.id");
        final String applicationId = variables.get("application1.id");
        final List<String> profiles = new ArrayList<>();
        profiles.add("Parent5");
        test.directory().createStructure("structure6", "666666A").compose(subStructureId -> {
            return test.directory().createProfileGroup("Parent5").compose(groupId -> {
                variables.put("group6.id", groupId);
                return test.directory().attachGroupToStruct(groupId, subStructureId);
            }).compose(resx -> {
                return test.directory().attachSubstructure(structureId, subStructureId);
            });
        }).onComplete(res0 -> {
            context.assertTrue(res0.succeeded());
            service.massAuthorize(structureId, applicationId, profiles, resa -> {
                context.assertTrue(resa.isRight());
                context.assertEquals(1, resa.right().getValue().getInteger("nbCreation"));
                service.massAuthorize(structureId, applicationId, profiles, res -> {
                    context.assertTrue(res.isRight());
                    context.assertEquals(0, res.right().getValue().getInteger("nbCreation"));
                    async.complete();
                });
            });
        });
    }
}