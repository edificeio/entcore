/* Copyright © "Open Digital Education", 2014
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
 */

package org.entcore.common.http.response;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class DefaultResponseHandler {

	private DefaultResponseHandler() {
	}

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
					JsonObject error = new JsonObject().put("error", event.left().getValue());
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
					JsonObject error = new JsonObject().put("error", event.left().getValue());
					Renders.renderJson(request, error, 400);
				}
			}
		};
	}

	public static Handler<Either<String, JsonObject>> notEmptyResponseHandler(final HttpServerRequest request) {
		return notEmptyResponseHandler(request, 200);
	}

	public static Handler<Either<String, JsonObject>> notEmptyResponseHandler(final HttpServerRequest request,
			final int successCode) {
		return notEmptyResponseHandler(request, successCode, 404);
	}

	public static Handler<Either<String, JsonObject>> notEmptyResponseHandler(final HttpServerRequest request,
			final int successCode, final int emptyCode) {
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
					JsonObject error = new JsonObject().put("error", event.left().getValue());
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
					JsonObject error = new JsonObject().put("error", event.left().getValue());
					Renders.renderJson(request, error, 400);
				}
			}
		};
	}

	public static Handler<Either<JsonObject, JsonObject>> reportResponseHandler(final Vertx vertx, final String path,
			final HttpServerRequest request) {
		return new Handler<Either<JsonObject, JsonObject>>() {
			@Override
			public void handle(Either<JsonObject, JsonObject> event) {
				if (event.isRight()) {
					Renders.renderJson(request, event.right().getValue(), 200);
				} else {
					JsonObject error = new JsonObject().put("errors", event.left().getValue());
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

	public static Handler<AsyncResult<JsonObject>> asyncDefaultResponseHandler(final HttpServerRequest request) {
		return asyncDefaultResponseHandler(request, 200);
	}

	public static Handler<AsyncResult<JsonObject>> asyncDefaultResponseHandler(final HttpServerRequest request,
			final int successCode) {
		return event -> {
			if (event.succeeded()) {
				Renders.renderJson(request, event.result(), successCode);
			} else {
				JsonObject error = new JsonObject().put("error", event.cause().getMessage());
				Renders.renderJson(request, error, 400);
			}
		};
	}

	public static Handler<AsyncResult<Void>> asyncVoidResponseHandler(final HttpServerRequest request) {
		return asyncVoidResponseHandler(request, 200);
	}

	public static Handler<AsyncResult<Void>> asyncVoidResponseHandler(final HttpServerRequest request,
			final int successCode) {
		return event -> {
			if (event.succeeded()) {
				Renders.ok(request);
			} else {
				JsonObject error = new JsonObject().put("error", event.cause().getMessage());
				Renders.renderJson(request, error, 400);
			}
		};
	}

	public static Handler<AsyncResult<JsonObject>> asyncNotEmptyResponseHandler(final HttpServerRequest request) {
		return asyncNotEmptyResponseHandler(request, 200);
	}

	public static Handler<AsyncResult<JsonObject>> asyncNotEmptyResponseHandler(final HttpServerRequest request,
			final int successCode) {
		return asyncNotEmptyResponseHandler(request, successCode, 404);
	}

	public static Handler<AsyncResult<JsonObject>> asyncNotEmptyResponseHandler(final HttpServerRequest request,
			final int successCode, final int emptyCode) {
		return event -> {
			if (event.succeeded()) {
				if (event.result() != null && event.result().size() > 0) {
					Renders.renderJson(request, event.result(), successCode);
				} else {
					request.response().setStatusCode(emptyCode).end();
				}
			} else {
				JsonObject error = new JsonObject().put("error", event.cause().getMessage());
				Renders.renderJson(request, error, 400);
			}
		};
	}

	public static Handler<AsyncResult<JsonArray>> asyncArrayResponseHandler(final HttpServerRequest request) {
		return event -> {
			if (event.succeeded()) {
				Renders.renderJson(request, event.result());
			} else {
				JsonObject error = new JsonObject().put("error", event.cause().getMessage());
				Renders.renderJson(request, error, 400);
			}
		};
	}

}
