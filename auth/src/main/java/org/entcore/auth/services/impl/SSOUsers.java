/*
 * Copyright © WebServices pour l'Éducation, 2015
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import org.opensaml.saml2.core.Assertion;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class SSOUsers extends AbstractSSOProvider {

	@Override
	public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
		if (!validConditions(assertion, handler)) return;

		String joinKey = getAttribute(assertion, "FrEduIdENT");
		if (joinKey != null && !joinKey.trim().isEmpty()) {
			executeQuery("MATCH (u:User {externalId:{joinKey}}) ", new JsonObject().put("joinKey", joinKey),
					assertion, handler);
		} else {
			handler.handle(new Either.Left<String, Object>("invalid.joinKey"));
		}
	}

}
