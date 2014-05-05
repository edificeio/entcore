/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.http.response;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

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
							.putString("error", event.left().getValue());
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
		return new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					if (event.right().getValue() != null && event.right().getValue().size() > 0) {
						Renders.renderJson(request, event.right().getValue(), successCode);
					} else {
						request.response().setStatusCode(404).end();
					}
				} else {
					JsonObject error = new JsonObject()
							.putString("error", event.left().getValue());
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
							.putString("error", event.left().getValue());
					Renders.renderJson(request, error, 400);
				}
			}
		};
	}

	public static void leftToResponse(HttpServerRequest request, Either.Left<String, ?> left) {
		if (left != null) {
			Renders.renderJson(request, new JsonObject().putString("error", left.getValue()), 400);
		} else {
			request.response().setStatusCode(400).end();
		}
	}

}
