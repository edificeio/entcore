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

package org.entcore.common.pdf;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.Utils.isEmpty;

public class NodePdfClient implements PdfGenerator {

	private static final Logger log = LoggerFactory.getLogger(NodePdfClient.class);
	private final Vertx vertx;
	private final HttpClient client;
	private final String authHeader;
	private final String clientId;

	public NodePdfClient(Vertx vertx, JsonObject conf) throws URISyntaxException {
		this.vertx = vertx;
		this.authHeader = "Basic " + conf.getString("auth");
		this.clientId = conf.getString("pdf-connector-id");
		final URI uri = new URI(conf.getString("url"));
		final HttpClientOptions options = new HttpClientOptions()
				.setDefaultHost(uri.getHost()).setDefaultPort(uri.getPort()).setSsl("https".equals(uri.getScheme()));
		this.client = vertx.createHttpClient(options);
	}

	@Override
	public void generatePdfFromTemplate(String name, String template, Handler<AsyncResult<Pdf>> handler) {
		generatePdfFromTemplate(name, template, null, handler);
	}

	private void generatePdfFromTemplate(String name, String template, String token, Handler<AsyncResult<Pdf>> handler) {
		final HttpClientRequest req = client.post("/generate/pdf", responseHandler(handler));
		final String boundary = UUID.randomUUID().toString();
		req.putHeader("Authorization", authHeader);
		req.putHeader("Content-Type","multipart/form-data; boundary=" + boundary);
		req.end(multipartBody(name, token, template, boundary));
	}

	@Override
	public void generatePdfFromUrl(String name, String url, Handler<AsyncResult<Pdf>> handler) {
		generatePdfFromUrl(name, url, null, handler);
	}

	private void generatePdfFromUrl(String name, String url, String token, Handler<AsyncResult<Pdf>> handler) {
		final HttpClientRequest req = client.post("/print/pdf", responseHandler(handler));
		req.putHeader("Authorization", authHeader);
		req.putHeader("Content-Type", "application/json");
		JsonObject j = new JsonObject().put("url", url).put("name", name);
		if (isNotEmpty(token)) {
			j.put("token", token);
		}
		req.end(j.encode());
	}

	@Override
	public void generatePdfFromTemplate(UserInfos user, String name, String template, Handler<AsyncResult<Pdf>> handler) {
		try {
			final String token = createToken(user);
			generatePdfFromTemplate(name, template, token, handler);
		} catch (Exception e) {
			log.error("Error creating when send generate pdf from template.", e);
			handler.handle(new DefaultAsyncResult<>(e));
		}
	}

	@Override
	public void generatePdfFromUrl(UserInfos user, String name, String url, Handler<AsyncResult<Pdf>> handler) {
		try {
			final String token = createToken(user);
			generatePdfFromUrl(name, url, token, handler);
		} catch (Exception e) {
			log.error("Error creating when send generate pdf from url.", e);
			handler.handle(new DefaultAsyncResult<>(e));
		}
	}

	@Override
	public Future<Pdf> generatePdfFromTemplate(String name, String template) {
		return generatePdfFromTemplate(name, template, (String) null);
	}

	@Override
	public Future<Pdf> generatePdfFromTemplate(String name, String template, String token) {
		Future<Pdf> future = Future.future();
		generatePdfFromTemplate(name, template, token, ar -> {
			if (ar.succeeded()) {
				future.complete(ar.result());
			} else {
				future.fail(ar.cause());
			}
		});
		return future;
	}

	@Override
	public Future<Pdf> generatePdfFromUrl(String name, String url) {
		return generatePdfFromUrl(name, url, (String) null);
	}

	@Override
	public Future<Pdf> generatePdfFromUrl(String name, String url, String token) {
		Future<Pdf> future = Future.future();
		generatePdfFromUrl(name, url, token, ar -> {
			if (ar.succeeded()) {
				future.complete(ar.result());
			} else {
				future.fail(ar.cause());
			}
		});
		return future;
	}

	private Handler<HttpClientResponse> responseHandler(Handler<AsyncResult<Pdf>> handler) {
		return res -> {
			if (res.statusCode() == 200) {
				res.bodyHandler(buffer -> {
					String name = res.getHeader("Content-Disposition");
					if (isNotEmpty(name)) {
						name = name.replaceFirst("attachment; filename=", "")
								.replaceAll("\"", "");
					} else {
						name = "nop.pdf";
					}
					final Pdf pdf = new Pdf(name, buffer);
					handler.handle(new DefaultAsyncResult<>(pdf));
				});
			} else {
				log.error("Invalid status code when receive pdf response.");
				handler.handle(new DefaultAsyncResult<>(new PdfException("invalid.pdf.response")));
			}
		};
	}

	private Buffer multipartBody(String name, String token, String content, String boundary) {
		Buffer buffer = Buffer.buffer();
		buffer.appendString("--" + boundary + "\r\n");
		buffer.appendString("Content-Disposition: form-data; name=\"name\"\r\n");
		buffer.appendString("\r\n");
		buffer.appendString(name + "\r\n");
		if (isNotEmpty(token)) {
			buffer.appendString("--" + boundary + "\r\n");
			buffer.appendString("Content-Disposition: form-data; name=\"token\"\r\n");
			buffer.appendString("\r\n");
			buffer.appendString(token + "\r\n");
		}
		buffer.appendString("--" + boundary + "\r\n");
		buffer.appendString("Content-Disposition: form-data; name=\"template\"; filename=\"file\"\r\n");
		buffer.appendString("Content-Type: application/xml\r\n");
		buffer.appendString("\r\n");
		buffer.appendString(content);
		buffer.appendString("\r\n");
		buffer.appendString("--" + boundary + "--\r\n");
		return buffer;
	}

	@Override
	public String createToken(UserInfos user) throws Exception {
		final String token = UserUtils.createJWTToken(vertx, user, clientId, null);
		if (isEmpty(token)) {
			throw new PdfException("invalid.token");
		}
		return token;
	}

}
