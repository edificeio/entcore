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

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.share.ShareService;
import org.entcore.common.share.impl.SqlShareService;
import org.entcore.common.user.UserInfos;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
public class AuthTest {
    private static final TestHelper test = TestHelper.helper();

    @ClassRule
    public static Neo4jContainer neo4jContainer = test.database().createNeo4jContainer();
    @ClassRule
    public static PostgreSQLContainer postgreSQLContainer = test.database().createPostgreSQLContainer();
    static String pathId;

   
    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initNeo4j(context, neo4jContainer);
    }

    @Test
    public void testAuthServiceShouldRedirect(TestContext context) {
        final Async async = context.async();
        
    }

}
