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

package org.entcore.auth;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.entcore.auth.services.SafeRedirectionService;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class SafeRedirectionTest {
    private static final TestHelper test = TestHelper.helper();

    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    static SafeRedirectionService redirectionService = SafeRedirectionService.getInstance();
    static final String entHost = "http://entcore.org";
    static final JsonObject redirectConfig = new JsonObject().put("delayInMinutes", 1l).put("defaultDomains",
            new JsonArray().add("https://entcore-config.com").add("badhost"));
    static final URI entUri = URI.create(entHost);

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initNeo4j(context, neo4jContainer);
        test.portal().addSkin(entUri.getHost(), "default");
        final List<Future> futures = new ArrayList<Future>();
        futures.add(test.registry().createApplicationUser("connecteur1", "https://connecteur1.com", new JsonArray()));
        futures.add(test.registry().createApplicationUser("connecteur2", "https://connecteur2.com", new JsonArray()));
        futures.add(test.registry().createApplicationUser("connecteur3", "/path", new JsonArray()));
        futures.add(test.registry().createApplicationUser("connecteur4", "://badadress", new JsonArray()));
        final Async async = context.async();
        CompositeFuture.all(futures).setHandler(res -> {
            context.assertTrue(res.succeeded());
            redirectionService.init(test.vertx(), redirectConfig);
            async.complete();
        });

    }

    HttpServerRequest getRequest() {
        final HttpServerRequest request = test.http().get("/auth/login");
        request.headers().set("Host", entUri.getHost());
        return request;
    }

    @Test
    public void testAuthServiceShouldRedirectToPath(TestContext context) {
        final Async async = context.async();
        final HttpServerRequest request = getRequest();
        request.response().endHandler(end -> {
            final String location = request.response().headers().get("Location");
            context.assertEquals(entUri.toString() + "/custompath", location);
            async.complete();
        });
        redirectionService.redirect(request, "/custompath");
    }

    @Test
    public void testAuthServiceShouldRedirectToInternalHost(TestContext context) {
        final Async async = context.async();
        final HttpServerRequest request = getRequest();
        request.response().endHandler(end -> {
            final String location = request.response().headers().get("Location");
            context.assertEquals(entUri.toString() + "/custompath", location);
            async.complete();
        });
        redirectionService.redirect(request, entUri.toString(), "/custompath");
    }

    @Test
    public void testAuthServiceShouldRedirectToExternalHost(TestContext context) {
        final Async async = context.async();
        final HttpServerRequest request = getRequest();
        request.response().endHandler(end -> {
            final String location = request.response().headers().get("Location");
            context.assertEquals("https://connecteur2.com/custompath", location);
            async.complete();
        });
        redirectionService.redirect(request, "https://connecteur2.com", "/custompath");
    }

    @Test
    public void testAuthServiceShouldRedirectToExternalHostWithoutScheme(TestContext context) {
        final Async async = context.async();
        final HttpServerRequest request = getRequest();
        request.response().endHandler(end -> {
            final String location = request.response().headers().get("Location");
            //webutils seems to concat enthost with host without scheme??
            context.assertEquals("http://entcore.orgconnecteur2.com/custompath", location);
            async.complete();
        });
        redirectionService.redirect(request, "connecteur2.com", "/custompath");
    }

    @Test
    public void testAuthServiceShouldRedirectToConfiguredHost(TestContext context) {
        final Async async = context.async();
        final HttpServerRequest request = getRequest();
        request.response().endHandler(end -> {
            final String location = request.response().headers().get("Location");
            context.assertEquals("https://entcore-config.com/custompath", location);
            async.complete();
        });
        redirectionService.redirect(request, "https://entcore-config.com", "/custompath");
    }

    @Test
    public void testAuthServiceShouldNotRedirectToUnknownHost(TestContext context) {
        final Async async = context.async();
        final HttpServerRequest request = getRequest();
        request.response().endHandler(end -> {
            final String location = request.response().headers().get("Location");
            context.assertEquals(entHost + "/", location);
            async.complete();
        });
        redirectionService.redirect(request, "https://hacker.com.com", "/custompath");
    }

}
