/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.common.http.response;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class DefaultResponseHandler {

	private DefaultResponseHandler() {}

	public static Handler<Either<String, JsonObject>> defaultResponseHandler(final HttpServerRequest request) {
		return defaultResponseHandler(request, 200);
	}

	public static Handler<Either<String, JsonObject>> defaultResponseHandler(final HttpServerRequest request,
																	   final int successCode) {
		return new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					Renders.renderJson(request, event.right().getValue(), successCode);
				} else {
					JsonObject error = new JsonObject()
							.put("error", event.left().getValue());
					Renders.renderJson(request, error, 400);
				}
			}
		};
	}

	public static Handler<Either<String, Void>> voidResponseHandler(final HttpServerRequest request) {
		return voidResponseHandler(request, 200);
	}

	public static Handler<Either<String, Void>> voidResponseHandler(final HttpServerRequest request,
			final int successCode) {
		return new Handler<Either<String, Void>>() {
			@Override
			public void handle(Either<String, Void> event) {
				if (event.isRight()) {
					Renders.ok(request);
				} else {
					JsonObject error = new JsonObject()
							.put("error", event.left().getValue());
					Renders.renderJson(request, error, 400);
				}
			}
		};
	}

	public static Handler<Either<String, JsonObject>> notEmptyResponseHandler(
			final HttpServerRequest request) {
		return notEmptyResponseHandler(request, 200);
	}

	public static Handler<Either<String, JsonObject>> notEmptyResponseHandler(
			final HttpServerRequest request, final int successCode) {
		return notEmptyResponseHandler(request, successCode, 404);
	}

	public static Handler<Either<String, JsonObject>> notEmptyResponseHandler(
			final HttpServerRequest request, final int successCode, final int emptyCode) {
		return new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					if (event.right().getValue() != null && event.right().getValue().size() > 0) {
						Renders.renderJson(request, event.right().getValue(), successCode);
					} else {
						request.response().setStatusCode(emptyCode).end();
					}
				} else {
					JsonObject error = new JsonObject()
							.put("error", event.left().getValue());
					Renders.renderJson(request, error, 400);
				}
			}
		};
	}

	public static Handler<Either<String, JsonArray>> arrayResponseHandler(final HttpServerRequest request) {
		return new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> event) {
				if (event.isRight()) {
					Renders.renderJson(request, event.right().getValue());
				} else {
					JsonObject error = new JsonObject()
							.put("error", event.left().getValue());
					Renders.renderJson(request, error, 400);
				}
			}
		};
	}

	public static Handler<Either<JsonObject, JsonObject>> reportResponseHandler(
			final Vertx vertx, final String path, final HttpServerRequest request) {
		return new Handler<Either<JsonObject, JsonObject>>() {
			@Override
			public void handle(Either<JsonObject, JsonObject> event) {
				if (event.isRight()) {
					Renders.renderJson(request, event.right().getValue(), 200);
				} else {
					JsonObject error = new JsonObject()
							.put("errors", event.left().getValue());
					Renders.renderJson(request, error, 400);
				}
				deleteImportPath(vertx, path);
			}
		};
	}

	public static void leftToResponse(HttpServerRequest request, Either.Left<String, ?> left) {
		if (left != null) {
			Renders.renderJson(request, new JsonObject().put("error", left.getValue()), 400);
		} else {
			request.response().setStatusCode(400).end();
		}
	}

}
