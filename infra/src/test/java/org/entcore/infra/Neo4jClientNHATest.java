/*
 * Copyright © "Open Digital Education", 2019
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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.infra;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.*;
import org.entcore.test.DatabaseClusterTestHelper;
import org.entcore.test.PostgresReactiveTestHelper;
import org.entcore.test.TestHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
public class Neo4jClientNHATest {
    static Logger log = LoggerFactory.getLogger(Neo4jClientNHATest.class);
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static DatabaseClusterTestHelper.Neo4jCluster cluster = test.database().cluster().initNeo4j(1, 0);
    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer().withInitScript("init_neo4j.sql");
    static PostgresReactiveTestHelper pgReactive;

    int eventReceived = 0;
    int mailReceived = 0;
    int nbTests = 0;
    final List<Promise> pending = new ArrayList<>();
    final List<Promise> pendingMails = new ArrayList<>();

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        EventStoreFactory.getFactory().setVertx(test.vertx());
        test.config().withMailerConfig(pgContainer).withStatConfig(pgContainer);
        pgReactive = test.database().pgReactive(pgContainer);
    }

    @Before
    public void beforeEach(TestContext context) throws Exception {
        if (nbTests > 0) {
            log.info("Clean events and mails....");
            pgContainer.stop();
            pgContainer.start();
            eventReceived = 0;
            mailReceived = 0;
            final Async async = context.async();
            cluster.start(test.vertx()).compose(r -> waitPending()).onComplete(context.asyncAssertSuccess(r -> {
                pending.clear();
                pendingMails.clear();
                async.complete();
            }));
        }
        nbTests++;
    }

    //@Test
    public void testHealthCheckShouldDetectChanges(TestContext context) {
        final Neo4jRestClientNodeManager manager = manager(context, true, false, false);
        final Async async = context.async();
        assertMasterAvailable(context, manager, 1);
        assertSlaveAvailable(context, manager, 1);
        assertEventReceived(context, Optional.empty(), Optional.empty(), Optional.empty());
        assertMailReceived(context, Optional.empty());
        log.info("Should first health check...");
        manager.getChecker().check(manager).compose(r -> waitMS(50)).compose(r -> {
            log.info("Should detect 1 master...");
            assertMasterAvailable(context, manager, 1);
            assertSlaveAvailable(context, manager, 1);
            assertMailReceived(context, 0);
            assertBanned(context, manager, 0);
            final Optional<Neo4jRestClientNode> node = manager.getClients().stream().filter(e -> e.isAvailable()).findFirst();
            final boolean res = cluster.stop(node.get().getUrl());
            context.assertTrue(res);
            return waitSwitch();
        }).compose(r -> waitPending()).compose(r -> manager.getChecker().check(manager)).compose(r -> {
            log.info("Should not detect 1 unavailable (no health check nonha)...");
            assertMasterAvailable(context, manager, 1);
            assertSlaveAvailable(context, manager, 1);
            assertUnvailable(context, manager, 0);
            assertEventReceived(context, 0);
            assertMailReceived(context, 0);
            assertBanned(context, manager, 0);
            final Optional<Neo4jRestClientNode> node = manager.getClients().stream().filter(e -> e.isAvailable()).findFirst();
            final boolean res = cluster.stop(node.get().getUrl());
            context.assertTrue(res);
            return waitPending();
        }).compose(r -> {
            return cluster.start(test.vertx()).map(e -> {
                syncManager(context, manager, cluster);
                return e;
            });
        }).compose(r -> manager.getChecker().check(manager)).compose(r -> waitMS(50)).compose(r -> {
            log.info("Should detect 1 available...");
            assertMasterAvailable(context, manager, 1);
            assertSlaveAvailable(context, manager, 1);
            assertUnvailable(context, manager, 0);
            assertEventReceived(context, 0);
            assertMailReceived(context, 0);
            return waitPending();
        }).onComplete(context.asyncAssertSuccess(r -> {
            async.complete();
        }));
    }


    @Test
    public void testReadCheckShouldDetectChanges(TestContext context) {
        final Neo4jRestClientNodeManager manager = manager(context, false, true, false);
        final Async async = context.async();
        assertMasterAvailable(context, manager, 0);
        assertSlaveAvailable(context, manager, 0);
        log.info("Should first read check...");
        manager.getChecker().check(manager).compose(r -> waitMS(50)).compose(r -> {
            //wait store event to finish....
            log.info("Should detect 1 master (any)...");
            assertMasterAvailable(context, manager, 1);
            assertSlaveAvailable(context, manager, 1);
            assertUnvailable(context, manager, 0);
            assertEventReceived(context, 1);
            assertMailReceived(context, 0);
            assertBanned(context, manager, 0);
            final Optional<Neo4jRestClientNode> node = manager.getClients().stream().filter(e -> e.isSlaveAvailable()).findFirst();
            final boolean res = cluster.stop(node.get().getUrl());
            context.assertTrue(res);
            return waitSwitch();
        }).compose(r -> waitPending()).compose(r -> manager.getChecker().check(manager)).compose(r -> {
            log.info("Should detect 1 unavailable...");
            assertMasterAvailable(context, manager, 0);
            assertSlaveAvailable(context, manager, 0);
            assertUnvailable(context, manager, 1);
            assertEventReceived(context, 1);
            assertMailReceived(context, 1);
            assertBanned(context, manager, 1);
            return waitPending();
        }).compose(r -> {
            return cluster.start(test.vertx()).map(e -> {
                syncManager(context, manager, cluster);
                return e;
            });
        }).compose(r -> waitSwitch()).compose(r -> waitPending()).compose(r -> {
            //set still ban
            manager.getClients().forEach(e->{
                if(!e.isAvailable()){
                    e.setNotAvailableFrom(LocalDateTime.now().minusSeconds(5));
                }
            });
            return manager.getChecker().check(manager);
        }).compose(r -> {
            log.info("Should not detect 1 new available (banned)...");
            assertBanned(context, manager, 1);
            assertMasterAvailable(context, manager, 0);
            assertSlaveAvailable(context, manager, 0);
            assertUnvailable(context, manager, 1);
            assertEventReceived(context, 0);
            assertMailReceived(context, 0);
            manager.getClients().forEach(e -> {
                e.setNotAvailableFrom(LocalDateTime.now().minusSeconds(15));
            });
            assertBanned(context, manager, 0);
            return waitPending();
        }).compose(r -> manager.getChecker().check(manager)).compose(r -> waitMS(50)).compose(r -> {
            //wait store of events 50ms
            log.info("Should detect 1 new available (unbanned)...");
            assertMasterAvailable(context, manager, 1);
            assertSlaveAvailable(context, manager, 1);
            assertUnvailable(context, manager, 0);
            assertEventReceived(context, 1);
            assertMailReceived(context, 0);
            return waitPending();
        }).onComplete(context.asyncAssertSuccess(r -> {
            async.complete();
        }));
    }

    @Test
    public void testMapCheckShouldDetectChanges(TestContext context) {
        final Neo4jRestClientNodeManager managerActif = manager(context, true, true, true, true);
        final Neo4jRestClientNodeManager managerPassive = manager(context, true, true, true, false);
        final List<Neo4jRestClientNodeManager> managers = Arrays.asList(managerActif, managerPassive);
        final Async async = context.async();
        assertMasterAvailable(context, managerActif, 0);
        assertSlaveAvailable(context, managerActif, 0);
        assertMasterAvailable(context, managerPassive, 1);
        assertSlaveAvailable(context, managerPassive, 1);
        assertEventReceived(context, Optional.empty(), Optional.empty(), Optional.empty());
        assertMailReceived(context, Optional.empty());
        log.info("Should first map check...");
        checkAll(managers).compose(r -> waitMS(50)).compose(r -> {
            //wait store event to finish....
            log.info("Should detect 1 master (any)...");
            for (final Neo4jRestClientNodeManager manager : managers) {
                assertMasterAvailable(context, manager, 1);
                assertUnvailable(context, manager, 0);
                assertBanned(context, manager, 0);
            }
            assertEventReceived(context, 2);
            assertMailReceived(context, 0);
            final Optional<Neo4jRestClientNode> node = managerActif.getClients().stream().filter(e -> e.isAvailable()).findFirst();
            final boolean res = cluster.stop(node.get().getUrl());
            context.assertTrue(res);
            return waitSwitch();
        }).compose(r -> waitPending()).compose(r -> checkAll(managers)).compose(r -> {
            log.info("Should detect 1 unavailable...");
            for (final Neo4jRestClientNodeManager manager : managers) {
                assertMasterAvailable(context, manager, 0);
                assertSlaveAvailable(context, manager, 0);
                assertUnvailable(context, manager, 1);
                assertBanned(context, manager, 1);
            }
            assertEventReceived(context, 2);
            assertMailReceived(context, 2);
            return waitPending();
        }).compose(r -> checkAll(managers)).compose(r -> {
            return cluster.start(test.vertx()).map(e -> {
                for (final Neo4jRestClientNodeManager manager : managers) {
                    syncManager(context, manager, cluster);
                }
                return e;
            });
        }).compose(r -> waitSwitch()).compose(r -> waitPending()).compose(r -> {
            managers.forEach(m->{
                //set still ban
                m.getClients().forEach(e->{
                    if(!e.isAvailable()){
                        e.setNotAvailableFrom(LocalDateTime.now().minusSeconds(5));
                    }
                });
            });
            return checkAll(managers);
        }).compose(r -> waitMS(50)).compose(r -> {
            log.info("Should not detect 1 new available (banned)...");
            assertBanned(context, managerActif, 1);
            assertBanned(context, managerPassive, 1);
            assertMasterAvailable(context, managerActif, 0);
            assertMasterAvailable(context, managerPassive, 0);
            for (final Neo4jRestClientNodeManager manager : managers) {
                assertSlaveAvailable(context, manager, 0);
                assertUnvailable(context, manager, 1);
                manager.getClients().forEach(e -> {
                    e.setNotAvailableFrom(LocalDateTime.now().minusSeconds(15));
                });
                assertBanned(context, manager, 0);
            }
            assertEventReceived(context, 0);
            assertMailReceived(context, 0);
            return waitPending();
        }).compose(r -> checkAll(managers)).compose(r -> waitMS(50)).compose(r -> {
            //wait store of events 50ms
            log.info("Should detect 1 new available (unbanned)...");
            for (final Neo4jRestClientNodeManager manager : managers) {
                assertMasterAvailable(context, manager, 1);
                assertUnvailable(context, manager, 0);
            }
            assertEventReceived(context, 2);
            assertMailReceived(context, 0);
            return waitPending();
        }).onComplete(context.asyncAssertSuccess(r -> {
            async.complete();
        }));
    }

    @Test
    public void testClientShouldReadAndWrite(TestContext context) {
        final Neo4jRestClientNodeManager manager = manager(context, true, true, false);
        final Async async = context.async();
        log.info("Should read and write...");
        check(manager).compose(r -> waitMS(50)).compose(r -> {
            //wait store event to finish....
            log.info("Should read and write when 1 slave or master (any)...");
            assertMasterAvailable(context, manager, 1);
            assertSlaveAvailable(context, manager, 1);
            assertQuery(context, true, manager);
            assertQuery(context, false, manager);
            return waitPending();
        }).compose(r -> {
            log.info("Should stop 1 node...");
            final Optional<Neo4jRestClientNode> node = manager.getClients().stream().filter(e -> e.isSlaveAvailable()).findFirst();
            final boolean res = cluster.stop(node.get().getUrl());
            context.assertTrue(res);
            return waitSwitch();
        }).compose(r -> check(manager)).compose(r -> {
            log.info("Should not read and write when 1 unavailable...");
            assertMasterAvailable(context, manager, 0);
            assertSlaveAvailable(context, manager, 0);
            assertFailQuery(context, true, manager);
            assertFailQuery(context, false, manager);
            return waitPending();
        }).compose(r -> {
            log.info("Should start all...");
            return cluster.start(test.vertx()).map(e -> {
                syncManager(context, manager, cluster);
                return e;
            });
        }).compose(r -> waitSwitch()).compose(r -> waitPending()).compose(r -> {
            //set still ban
            manager.getClients().forEach(e->{
                if(!e.isAvailable()){
                    e.setNotAvailableFrom(LocalDateTime.now().minusSeconds(5));
                }
            });
            return check(manager);
        }).compose(r -> waitMS(50)).compose(r -> {
            log.info("Should read/write 1 new available (banned but return first)...");
            assertMasterAvailable(context, manager, 0);
            assertSlaveAvailable(context, manager, 0);
            assertQuery(context, true, manager);
            assertQuery(context, false, manager);
            manager.getClients().forEach(e -> {
                e.setNotAvailableFrom(LocalDateTime.now().minusSeconds(15));
            });
            return waitPending();
        }).compose(r -> check(manager)).compose(r -> waitMS(50)).compose(r -> {
            log.info("Should read/write 1 new available (unbanned)...");
            assertMasterAvailable(context, manager, 1);
            assertSlaveAvailable(context, manager, 1);
            //wait store of events 50ms
            assertQuery(context, true, manager);
            assertQuery(context, false, manager);
            return waitPending();
        }).onComplete(context.asyncAssertSuccess(r -> {
            async.complete();
        }));
    }

    private Neo4jRestClientNodeManager manager(final TestContext context, boolean healthcheck, boolean readcheck, boolean optimized) {
        return manager(context, healthcheck, readcheck, optimized, false);
    }

    private Neo4jRestClientNodeManager manager(final TestContext context, boolean healthcheck, boolean readcheck, boolean optimized, boolean optimizeActive) {
        final URI[] uris = cluster.getUris();
        context.assertEquals(1, uris.length);
        final long checkDelay = 100;
        final int poolSize = 10;
        final boolean keepAlive = true;
        final String authorizationHeader = null;
        final JsonObject neo4jConfig = new JsonObject();
        neo4jConfig.put("ban-duration-seconds", 10);
        neo4jConfig.put("notification-enable", true);
        neo4jConfig.put("healthcheck-enable", healthcheck);
        neo4jConfig.put("readcheck-enable", readcheck);
        neo4jConfig.put("optimized-check-enable", optimized);
        neo4jConfig.put("optimized-check-active", optimizeActive);
        neo4jConfig.put("email-alerts-dest", new JsonArray().add("test@test.com"));
        neo4jConfig.put("email-alerts-mindown", 1);
        final Neo4jRestClientNodeManager manager = new Neo4jRestClientNodeManager(uris, test.vertx(), checkDelay, poolSize, keepAlive, authorizationHeader, neo4jConfig, false);
        final Neo4jRestClientCheckGroup group = (Neo4jRestClientCheckGroup) manager.getChecker();
        final Long countHealth = group.getChecks().stream().filter(e -> e instanceof Neo4jRestClientCheckHealth).count();
        final Long countRead = group.getChecks().stream().filter(e -> e instanceof Neo4jRestClientCheckRead).count();
        final Long countNotifier = group.getChecks().stream().filter(e -> e instanceof Neo4jRestClientCheckNotifier).map(e -> {
            ((Neo4jRestClientCheckNotifier) e).setBeforeSendMail(ee -> {
                this.pendingMails.add(Promise.promise());
            });
            ((Neo4jRestClientCheckNotifier) e).setOnSendMail(ee -> {
                for (Promise a : pendingMails) {
                    if (!a.tryComplete()) {
                        return;
                    }
                }
            });
            return e;
        }).count();
        final Long countMap = group.getChecks().stream().filter(e -> e instanceof Neo4jRestClientCheckLocalMap).count();
        context.assertEquals(0, countHealth.intValue());
        if (optimized) {
            context.assertEquals(readcheck && optimizeActive ? 1 : 0, countRead.intValue());
        } else {
            context.assertEquals(readcheck ? 1 : 0, countRead.intValue());
        }
        context.assertEquals(optimized ? 1 : 0, countMap.intValue());
        context.assertEquals(1, countNotifier.intValue());
        context.assertEquals(1, manager.getClients().size());
        for (final Neo4jRestClientNode node : manager.getClients()) {
            //if health check disabled should be available and type=any
            context.assertEquals(Neo4jRestClientNode.Type.Any, node.getType());
            context.assertTrue( node.getAvailable());
            if (optimized) {
                //if read check disabled should be readable
                context.assertEquals(readcheck && optimizeActive ? null : true, node.getReadable());
            } else {
                //if read check disabled should be readable
                context.assertEquals(readcheck ? null : true, node.getReadable());
            }
        }
        return manager;
    }

    private void assertBanned(final TestContext context, final Neo4jRestClientNodeManager manager, final long expected) {
        final long nbMaster = manager.getClients().stream().filter(e -> e.isBanned()).count();
        try{
            context.assertEquals(expected, nbMaster);
        }catch(AssertionError exc){
            log.error("Count master: "+manager.getClients().stream().filter(e -> e.isMaster()).count());
            log.error("Count slave: "+manager.getClients().stream().filter(e -> e.isSlave()).count());
            log.error("Count master available: "+manager.getClients().stream().filter(e -> e.isMasterAvailable()).count());
            log.error("Count slave available: "+manager.getClients().stream().filter(e -> e.isSlaveAvailable()).count());
            log.error("Count available: "+manager.getClients().stream().filter(e -> e.isAvailable()).count());
            log.error("Count banned: "+manager.getClients().stream().filter(e -> e.isBanned()).count());
            throw exc;
        }
    }

    private void assertMasterAvailable(final TestContext context, final Neo4jRestClientNodeManager manager, final long expected) {
        final long nbMaster = manager.getClients().stream().filter(e -> e.isMasterAvailable()).count();
        try{
            context.assertEquals(expected, nbMaster);
        }catch(AssertionError exc){
            log.error("Count master: "+manager.getClients().stream().filter(e -> e.isMaster()).count());
            log.error("Count slave: "+manager.getClients().stream().filter(e -> e.isSlave()).count());
            log.error("Count master available: "+manager.getClients().stream().filter(e -> e.isMasterAvailable()).count());
            log.error("Count slave available: "+manager.getClients().stream().filter(e -> e.isSlaveAvailable()).count());
            log.error("Count available: "+manager.getClients().stream().filter(e -> e.isAvailable()).count());
            log.error("Count banned: "+manager.getClients().stream().filter(e -> e.isBanned()).count());
            throw exc;
        }
    }

    private void assertSlaveAvailable(final TestContext context, final Neo4jRestClientNodeManager manager, final long expected) {
        try{
            final long nbMaster = manager.getClients().stream().filter(e -> e.isSlaveAvailable()).count();
            context.assertEquals(expected, nbMaster);
        }catch(AssertionError exc){
            log.error("Count master: "+manager.getClients().stream().filter(e -> e.isMaster()).count());
            log.error("Count slave: "+manager.getClients().stream().filter(e -> e.isSlave()).count());
            log.error("Count master available: "+manager.getClients().stream().filter(e -> e.isMasterAvailable()).count());
            log.error("Count slave available: "+manager.getClients().stream().filter(e -> e.isSlaveAvailable()).count());
            log.error("Count available: "+manager.getClients().stream().filter(e -> e.isAvailable()).count());
            log.error("Count banned: "+manager.getClients().stream().filter(e -> e.isBanned()).count());
            throw exc;
        }
    }

    private void assertUnvailable(final TestContext context, final Neo4jRestClientNodeManager manager, final long expected) {
        final long nbUnavailable = manager.getClients().stream().filter(e -> !e.isAvailable()).count();
        try{
            context.assertEquals(expected, nbUnavailable);
        }catch(AssertionError exc){
            log.error("Count master: "+manager.getClients().stream().filter(e -> e.isMaster()).count());
            log.error("Count slave: "+manager.getClients().stream().filter(e -> e.isSlave()).count());
            log.error("Count master available: "+manager.getClients().stream().filter(e -> e.isMasterAvailable()).count());
            log.error("Count slave available: "+manager.getClients().stream().filter(e -> e.isSlaveAvailable()).count());
            log.error("Count available: "+manager.getClients().stream().filter(e -> e.isAvailable()).count());
            log.error("Count banned: "+manager.getClients().stream().filter(e -> e.isBanned()).count());
            throw exc;
        }
    }

    private Future<Void> assertEventReceived(final TestContext context, final int expected) {
        return assertEventReceived(context, Optional.of(expected), Optional.empty(), Optional.empty());
    }

    private Future<Void> assertEventReceived(final TestContext context, final Optional<Integer> expectedOpt, Optional<Integer> nbUp, Optional<Integer> nbDown) {
        final Promise<Void> future = Promise.promise();
        pgReactive.execute("SELECT * FROM events.neo4j_change_events ORDER BY date ASC ", new JsonArray()).onComplete(context.asyncAssertSuccess(r -> {
            final int count = r.size() - eventReceived;
            eventReceived = r.size();
            if (expectedOpt.isPresent()) {
                final int expected = expectedOpt.get();
                //System.out.println(new JsonArray(r).encodePrettily());
                context.assertEquals(expected, count);
                if (nbDown.isPresent() || nbUp.isPresent()) {
                    int nbUpCount = 0, nbDownCount = 0;
                    for (int i = 0; i < expected; i++) {
                        final JsonObject row = r.get(r.size() - 1 - i);
                        if (row.getString("state").equals("up")) {
                            nbUpCount++;
                        } else {
                            nbDownCount++;
                        }
                    }
                    //
                    if (nbDown.isPresent()) {
                        context.assertEquals(nbDown.get().intValue(), nbDownCount);
                    }
                    if (nbUp.isPresent()) {
                        context.assertEquals(nbUp.get().intValue(), nbUpCount);
                    }
                }
            }
            future.complete();
            pending.remove(future);
        }));
        pending.add(future);
        return future.future();
    }


    private Future<Void> assertMailReceived(final TestContext context, final int expected) {
        return assertMailReceived(context, Optional.of(new Integer(expected)));
    }

    private Future<Void> assertMailReceived(final TestContext context, final Optional<Integer> expectedOpt) {
        return Future.all(this.pendingMails.stream().map(p -> (Future<?>)p.future()).collect(Collectors.toList())).compose(ro -> {
            final Promise<Void> future = Promise.promise();
            pgReactive.execute("SELECT * FROM mail.mail_events ORDER BY date ASC ", new JsonArray()).onComplete(context.asyncAssertSuccess(r -> {
                final int count = r.size() - mailReceived;
                mailReceived = r.size();
                if (expectedOpt.isPresent()) {
                    //System.out.println(new JsonArray(r).encodePrettily());
                    context.assertEquals(expectedOpt.get(), count);
                }
                future.complete();
                pending.remove(future);
            }));
            pending.add(future);
            return future.future();
        });
    }

    private Future<Void> waitMS(int time) {
        final Promise<Void> future = Promise.promise();
        test.vertx().setTimer(time, r -> {
            future.complete();
        });
        return future.future();
    }

    private Future<Void> waitSwitch() {
        final Promise<Void> future = Promise.promise();
        test.vertx().setTimer(DatabaseClusterTestHelper.NEO4J_SWITCH_TIMEOUT_MS * 2, r -> {
            future.complete();
        });
        return future.future();
    }

    private Future<Void> waitPending() {
        return Future.all(pending.stream().map(p -> (Future<?>)p.future()).collect(Collectors.toList())).mapEmpty();
    }

    private void syncManager(final TestContext context, final Neo4jRestClientNodeManager manager, final DatabaseClusterTestHelper.Neo4jCluster cluster) {
        try {
            final Neo4jRestClientCheckGroup group = (Neo4jRestClientCheckGroup) manager.getChecker();
            final List<Neo4jRestClientCheckNotifier> notifiers = group.getChecks().stream().filter(e -> e instanceof Neo4jRestClientCheckNotifier).map(e -> (Neo4jRestClientCheckNotifier) e).collect(Collectors.toList());
            final List<Neo4jRestClientNode> nodes = new ArrayList<>();
            for (Neo4jRestClientNode node : manager.getClients()) {
                final String newUrl = cluster.getNewUrl(node.getUrl());
                if (!newUrl.equals(node.getUrl())) {
                    final Neo4jRestClientNode newNode = new Neo4jRestClientNode(newUrl, test.vertx(), node.getBanDurationSecond());
                    newNode.setReadable(node.getReadable());
                    newNode.setAvailable(node.getAvailable());
                    newNode.setNotAvailableFrom(node.getNotAvailableFrom());
                    newNode.setNotReadableFrom(node.getNotReadableFrom());
                    newNode.setType(node.getType());
                    nodes.add(newNode);
                    for (Neo4jRestClientCheckNotifier n : notifiers) {
                        if (n.getPreviousUnavailableUris().contains(node.getUrl())) {
                            n.getPreviousUnavailableUris().add(newUrl);
                        }
                    }
                } else {
                    nodes.add(node);
                }
            }
            manager.getClients().clear();
            manager.getClients().addAll(nodes);
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private Future<Void> check(final Neo4jRestClientNodeManager manager) {
        return manager.getChecker().check(manager);
    }

    private Future<Void> checkAll(final List<Neo4jRestClientNodeManager> managers) {
        Future<Void> future = Future.succeededFuture();
        for (final Neo4jRestClientNodeManager manager : managers) {
            future = future.compose(r -> manager.getChecker().check(manager));
        }
        return future;
    }

    private void assertQuery(final TestContext context, final boolean write, final Neo4jRestClientNodeManager manager) {
        try {
            final Promise<Void> future = Promise.promise();
            pending.add(future);
            if (write) {
                final Neo4jRestClientNode master = manager.getMasterNode();
                master.execute("CREATE(t:Temp {name:\"2\"}) RETURN t.name as name;", Optional.empty(), body -> {
                    final JsonObject json = new JsonObject(body.toString());
                    context.assertFalse(json.getJsonArray("data").isEmpty(), "data should not be empty");
                    final String name = json.getJsonArray("data").getJsonArray(0).getString(0);
                    context.assertEquals("2", name);
                    future.complete();
                }, err -> {
                    context.fail(err);
                    future.tryComplete();
                });
            } else {
                final Neo4jRestClientNode slave = manager.getSlaveNode();
                slave.execute("MATCH(t:Temp) RETURN t.name as name;", Optional.empty(), body -> {
                    final JsonObject json = new JsonObject(body.toString());
                    if (!json.getJsonArray("data").isEmpty()) {
                        final String name = json.getJsonArray("data").getJsonArray(0).getString(0);
                        context.assertEquals("2", name);
                    }
                    future.complete();
                }, err -> {
                    context.fail(err);
                    future.tryComplete();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            context.fail("no node available");
        }
    }

    private void assertFailQuery(final TestContext context, final boolean write, final Neo4jRestClientNodeManager manager) {
        try {
            final Promise<Void> future = Promise.promise();
            pending.add(future);
            if (write) {
                final Neo4jRestClientNode master = manager.getMasterNode();
                master.execute("CREATE(t:Temp {name:\"2\"}) RETURN t.name as name;", Optional.empty(), body -> {
                    context.fail("Should not create");
                    future.complete();
                }, err -> future.tryComplete());
            } else {
                final Neo4jRestClientNode slave = manager.getSlaveNode();
                slave.execute("MATCH(t:Temp) RETURN t.name as name;", Optional.empty(), body -> {
                    context.fail("Should not read");
                    future.complete();
                }, err -> future.tryComplete());
            }
            //no node available => ok
            future.complete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
