package org.entcore.cas.controllers;

import fr.wseduc.cas.endpoint.Validator;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.cas.http.WrappedRequest;
import org.vertx.java.core.http.HttpServerRequest;

public class SamlValidatorController extends BaseController {

	private Validator validator;

	@Post("/samlValidate")
	public void serviceValidate(HttpServerRequest request) {
		validator.serviceValidate(new WrappedRequest(request));
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}
}
