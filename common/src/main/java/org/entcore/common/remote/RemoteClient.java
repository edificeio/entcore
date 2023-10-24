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

package org.entcore.common.remote;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.http.WebsocketVersion;
import io.vertx.core.MultiMap;
import io.vertx.core.http.WebSocket;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RemoteClient implements HttpClient
{
    private static class Header
    {
        public String name;
        public String value;

        public Header(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        public static ArrayList<Header> fromJson(JsonObject headers)
        {
            ArrayList<Header> list = new ArrayList<Header>();

            if(headers != null)
                for(String attr : headers.fieldNames())
                    list.add(new Header(attr, headers.getString(attr)));

            return list;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(RemoteClient.class);

    private Vertx vertx;
    private JsonObject conf;

    private HttpClient vertxClient;

    private String host;
    private int port = 0;
    private boolean useSsl = false;
    private List<Header> headers;

    /**
     *
     * @param vertx
     * @param conf:
     * {
     *    "uri": "https://example.com:443", // URI components can be overridden by host, port and ssl
     *    "host": "example.com",            // Mandatory
     *    "port": 80,
     *    "ssl": false,
     *    "poolSize": 5,
     *    "keepAlive": true,
     *    "connectTimeout": 10000,
     *    "tryUseCompresssion": true,
     *    "headers": {                      // Headers will be added to every request
     *      "headerName1": "headerValue1",
     *      "headerName2": "headerValue2",
     *      ...
     *    }
     * }
     * @throws URISyntaxException
     */
    public RemoteClient(Vertx vertx, JsonObject conf)
    {
        this.vertx = vertx;
        this.conf = conf;

        if(conf == null)
            conf = new JsonObject();

        String confURI = conf.getString("uri");
        if(confURI != null)
        {
            try
            {
                URI uri = new URI(confURI);
                this.host = uri.getHost();
                this.port = uri.getPort();
                this.useSsl = "https".equals(uri.getScheme());
            }
            catch(URISyntaxException e)
            {
                log.error("Invalid URI syntax for remote client: " + confURI);
            }
        }

        this.host = conf.getString("host");
        this.useSsl = conf.getBoolean("ssl", this.useSsl);
        this.port = conf.getInteger("port", this.port == 0 ? this.useSsl ? 443 : 80 : this.port).intValue();

        final HttpClientOptions options = new HttpClientOptions()
                                            .setDefaultHost(this.host)
                                            .setDefaultPort(this.port)
                                            .setSsl(this.useSsl)
                                            .setMaxPoolSize(conf.getInteger("poolSize", 5))
                                            .setKeepAlive(conf.getBoolean("keepAlive", true))
                                            .setConnectTimeout(conf.getInteger("connectTimeout", 10000))
                                            .setTryUseCompression(conf.getBoolean("tryUseCompression", true));

        this.headers = Header.fromJson(conf.getJsonObject("headers"));

        this.vertxClient = vertx.createHttpClient(options);
    }

    private RequestOptions addHeaders(RequestOptions options)
    {
        if(this.headers != null) {
            for (Header h : this.headers) {
                options.putHeader(h.name, h.value);
            }
        }
        return options;
    }

    // =================================================================================================================================================

    @Override
    public Future<Void> close()
    {
        return this.vertxClient.close();
    }

    @Override
    public void close(Handler<AsyncResult<Void>> handler) {
        this.vertxClient.close(handler);
    }

    @Override
    public HttpClient connectionHandler(Handler<HttpConnection> handler)
    {
        this.vertxClient.connectionHandler(handler);
        return this;
    }

    @Override
    public HttpClient redirectHandler(Function<HttpClientResponse, Future<RequestOptions>> handler) {
        return null;
    }

    @Override
    public Function<HttpClientResponse, Future<RequestOptions>> redirectHandler() {
        return null;
    }

    @Override
    public void request(RequestOptions options, Handler<AsyncResult<HttpClientRequest>> handler) {
        this.vertxClient.request(this.addHeaders(options), handler);
    }

    @Override
    public Future<HttpClientRequest> request(RequestOptions options) {
        return this.vertxClient.request(this.addHeaders(options));
    }

    @Override
    public void request(HttpMethod method, int port, String host, String requestURI, Handler<AsyncResult<HttpClientRequest>> handler) {
        this.vertxClient.request(method, port, host, requestURI, handler);
    }

    @Override
    public Future<HttpClientRequest> request(HttpMethod method, int port, String host, String requestURI) {
        return this.vertxClient.request(method, port, host, requestURI);
    }

    @Override
    public void request(HttpMethod method, String host, String requestURI, Handler<AsyncResult<HttpClientRequest>> handler) {
        this.vertxClient.request(method, host, requestURI, handler);
    }

    @Override
    public Future<HttpClientRequest> request(HttpMethod method, String host, String requestURI) {
        return this.vertxClient.request(method, host, requestURI);
    }

    @Override
    public void request(HttpMethod method, String requestURI, Handler<AsyncResult<HttpClientRequest>> handler) {
        this.request(method, requestURI, handler);
    }

    @Override
    public Future<HttpClientRequest> request(HttpMethod method, String requestURI) {
        return this.vertxClient.request(method, requestURI);
    }

    @Override
    public boolean isMetricsEnabled()
    {
        return this.vertxClient.isMetricsEnabled();
    }

    @Override
    public void webSocket(int port, String host, String requestURI, Handler<AsyncResult<WebSocket>> handler)
    {
        this.vertxClient.webSocket(port, host, requestURI, handler);
    }

    @Override
    public Future<WebSocket> webSocket(int port, String host, String requestURI) {
        return null;
    }

    @Override
    public void webSocket(String requestURI, Handler<AsyncResult<WebSocket>> handler)
    {
        this.vertxClient.webSocket(requestURI, handler);
    }

    @Override
    public Future<WebSocket> webSocket(String requestURI) {
        return null;
    }


    @Override
    public void webSocket(String host, String requestURI, Handler<AsyncResult<WebSocket>> handler)
    {
        this.vertxClient.webSocket(host, requestURI, handler);
    }

    @Override
    public Future<WebSocket> webSocket(String host, String requestURI) {
        return null;
    }

    @Override
    public void webSocket(WebSocketConnectOptions options, Handler<AsyncResult<WebSocket>> handler)
    {
        this.vertxClient.webSocket(options, handler);
    }

    @Override
    public Future<WebSocket> webSocket(WebSocketConnectOptions options) {
        return null;
    }


    @Override
    public void webSocketAbs(String url, MultiMap headers, WebsocketVersion version, List<String> subProtocols, Handler<AsyncResult<WebSocket>> handler)
    {
        this.vertxClient.webSocketAbs(url, headers, version, subProtocols, handler);
    }

    @Override
    public Future<WebSocket> webSocketAbs(String url, MultiMap headers, WebsocketVersion version, List<String> subProtocols) {
        return null;
    }

    @Override
    public Future<Boolean> updateSSLOptions(SSLOptions options) {
        return null;
    }

    @Override
    public void updateSSLOptions(SSLOptions options, Handler<AsyncResult<Boolean>> handler) {
        HttpClient.super.updateSSLOptions(options, handler);
    }

    @Override
    public Future<Boolean> updateSSLOptions(SSLOptions options, boolean force) {
        return null;
    }

    @Override
    public void updateSSLOptions(SSLOptions options, boolean force, Handler<AsyncResult<Boolean>> handler) {
        HttpClient.super.updateSSLOptions(options, force, handler);
    }


}
