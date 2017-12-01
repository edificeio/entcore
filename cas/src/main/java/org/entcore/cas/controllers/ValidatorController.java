/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.cas.controllers;

import fr.wseduc.cas.endpoint.Validator;
import fr.wseduc.rs.Get;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.cas.http.WrappedRequest;
import io.vertx.core.http.HttpServerRequest;

public class ValidatorController extends BaseController {

	private Validator validator;

	@Get("/serviceValidate")
	public void serviceValidate(HttpServerRequest request) {
		validator.serviceValidate(new WrappedRequest(request));
	}

	@Get("/proxyValidate")
	public void proxyValidate(HttpServerRequest request) {
		validator.proxyValidate(new WrappedRequest(request));
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

}
