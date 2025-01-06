/*
 * Copyright © "Open Digital Education", 2017
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

 */

package org.entcore.common.storage.impl;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.*;
import org.entcore.common.storage.AntivirusClient;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class HttpAntivirusClient implements AntivirusClient {

  private static final Logger log = LoggerFactory.getLogger(HttpAntivirusClient.class);
  private HttpClient httpClient;
  private String credential;

  public HttpAntivirusClient(Vertx vertx, String host, String cretential) {
    HttpClientOptions options = new HttpClientOptions()
        .setDefaultHost(host)
        .setDefaultPort(8001)
        .setMaxPoolSize(16)
        .setConnectTimeout(10000)
        .setKeepAlive(true);
    this.httpClient = vertx.createHttpClient(options);
    this.credential = cretential;
  }

  @Override
  public void scan(final String path) {
    this.scan(path, e -> {
    });
  }

  @Override
  public void scan(String path, Handler<AsyncResult<Void>> handler) {
    httpClient.request(HttpMethod.POST, "/infra/antivirus/scan")
        .map(req -> req.putHeader("Content-Type", "application/json")
            .putHeader("Authorization", "Basic " + credential)
            .exceptionHandler(e -> {
              log.error("Exception when call scan file : " + path, e);
              handler.handle(new DefaultAsyncResult<>(e));
            }))
        .flatMap(req -> req.send(new JsonObject().put("file", path).encode()))
        .onSuccess(resp -> {
          if (resp.statusCode() != 200) {
            log.error("Error when call scan file : " + path);
            final Exception exc = new Exception("Error when call scan file (" + resp.statusCode() + "): " + path);
            handler.handle(new DefaultAsyncResult<>(exc));
          } else {
            handler.handle(new DefaultAsyncResult<>((Void) null));
          }
        });
  }
}
