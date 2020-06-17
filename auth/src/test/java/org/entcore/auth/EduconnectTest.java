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
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.entcore.auth.security.SamlUtils;
import org.entcore.auth.services.impl.SSOEduConnect;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//@RunWith(VertxUnitRunner.class)
public class EduconnectTest {

	//@Test
	public void testAssertion(TestContext context) throws Exception {
        final String assertion = "";

		final Async async = context.async();
		DefaultBootstrap.bootstrap();
		Assertion a = SamlUtils.unmarshallAssertion(assertion);
		SSOEduConnect eSsoEduConnect = new MockSSOProvider(true);
		eSsoEduConnect.execute(a, either -> {
			if (either.isRight()) {
				async.complete();
			} else {
				context.fail();
			}
		});
	}

}
