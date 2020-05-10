/*
 * Copyright © "Open Digital Education", 2020
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

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


@RunWith(VertxUnitRunner.class)
public class HostTest {

    private static final String HOST = "test.entcore.org";
    private static final String HEADER_INJECTION_HOST = "attack.com";
    private static final HttpServerRequest ATTACK_HTTP_REQUEST = new JsonHttpServerRequest(new JsonObject()
            .put("headers", new JsonObject().put("Host", HEADER_INJECTION_HOST)));
    private static final HttpServerRequest HTTP_REQUEST = new JsonHttpServerRequest(new JsonObject()
            .put("headers", new JsonObject().put("Host", HOST)));
    private Vertx vertx;

    @Before
	public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        vertx.sharedData().getLocalMap("skins").put(HOST, "test");
        vertx.sharedData().getLocalMap("server").put("emailConfig", "{\"email\":\"ne-pas-repondre@entcore.org\", \"host\":\"https://test.entcore.org\", " +
            "\"type\":\"GoMail\", \"user\": \"test\", \"password\": \"test\", \"uri\" : \"http://localhost:3080\", \"platform\": \"test\"}");
        final Map<String, String> skins = vertx.sharedData().getLocalMap("skins");
		Renders.getAllowedHosts().addAll(skins.keySet());
    }

    @Test
    public void testGetHost(TestContext context) {
        final String host = Renders.getHost(HTTP_REQUEST);
        assertEquals(HOST, host);
    }

    @Test
    public void testHostHeaderInjection(TestContext context) {
        final String host = Renders.getHost(ATTACK_HTTP_REQUEST);
        assertEquals(HOST, host);
    }

    @Test
    public void testNotificationGetHost() {
        final EmailSender notification = new EmailFactory(vertx).getSender();
        assertEquals("http://" + HOST, notification.getHost(HTTP_REQUEST));
    }

    @Test
    public void testNotificationHostHeaderInjection() {
        final EmailSender notification = new EmailFactory(vertx).getSender();
        assertEquals("http://" + HOST, notification.getHost(ATTACK_HTTP_REQUEST));
    }

}
