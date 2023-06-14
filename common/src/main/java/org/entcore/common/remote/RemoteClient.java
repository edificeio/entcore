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
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private void addHeaders(HttpClientRequest req)
    {
        if(this.headers != null)
            for(Header h : this.headers)
                req.putHeader(h.name, h.value);
    }

    // =================================================================================================================================================

    @Override
    public void close()
    {
        this.vertxClient.close();
    }

    @Override
    public HttpClient connectionHandler(Handler<HttpConnection> handler)
    {
        this.vertxClient.connectionHandler(handler);
        return this;
    }

    @Override
    public HttpClientRequest delete(int port, String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.delete(port, host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest delete(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.delete(port, host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest delete(RequestOptions options)
    {
        HttpClientRequest req = this.vertxClient.delete(options);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest delete(RequestOptions options, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.delete(options, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest delete(String requestURI)
    {
        HttpClientRequest req = this.vertxClient.delete(requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest delete(String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.delete(requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest delete(String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.delete(host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest delete(String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.delete(host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest deleteAbs(String absoluteURI)
    {
        HttpClientRequest req = this.vertxClient.deleteAbs(absoluteURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest deleteAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.deleteAbs(absoluteURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest get(int port, String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.get(port, host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest get(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.get(port, host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest get(RequestOptions options)
    {
        HttpClientRequest req = this.vertxClient.get(options);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest get(RequestOptions options, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.get(options, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest get(String requestURI)
    {
        HttpClientRequest req = this.vertxClient.get(requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest get(String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.get(requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest get(String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.get(host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest get(String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.get(port, host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest getAbs(String absoluteURI)
    {
        HttpClientRequest req = this.vertxClient.getAbs(absoluteURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest getAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.getAbs(absoluteURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClient getNow(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.getNow(port, host, requestURI, responseHandler);
        return this;
    }

    @Override
    public HttpClient getNow(RequestOptions options, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.getNow(options, responseHandler);
        return this;
    }

    @Override
    public HttpClient getNow(String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.getNow(requestURI, responseHandler);
        return this;
    }

    @Override
    public HttpClient getNow(String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.getNow(host, requestURI, responseHandler);
        return this;
    }

    @Override
    public HttpClientRequest head(int port, String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.head(port, host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest head(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.head(port, host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest head(RequestOptions options)
    {
        HttpClientRequest req = this.vertxClient.head(options);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest head(RequestOptions options, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.head(options, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest head(String requestURI)
    {
        HttpClientRequest req = this.vertxClient.head(requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest head(String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.head(requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest head(String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.head(host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest head(String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.head(host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest headAbs(String absoluteURI)
    {
        HttpClientRequest req = this.vertxClient.headAbs(absoluteURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest headAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.headAbs(absoluteURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClient headNow(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.headNow(port, host, requestURI, responseHandler);
        return this;
    }

    @Override
    public HttpClient headNow(RequestOptions options, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.headNow(options, responseHandler);
        return this;
    }

    @Override
    public HttpClient headNow(String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.headNow(requestURI, responseHandler);
        return this;
    }

    @Override
    public HttpClient headNow(String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.headNow(host, requestURI, responseHandler);
        return this;
    }

    @Override
    public boolean isMetricsEnabled()
    {
        return this.vertxClient.isMetricsEnabled();
    }

    @Override
    public HttpClientRequest options(int port, String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.options(port, host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest options(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.options(port, host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest options(RequestOptions options)
    {
        HttpClientRequest req = this.vertxClient.options(options);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest options(RequestOptions options, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.options(options, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest options(String requestURI)
    {
        HttpClientRequest req = this.vertxClient.options(requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest options(String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.options(requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest options(String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.options(host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest options(String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.options(host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest optionsAbs(String absoluteURI)
    {
        HttpClientRequest req = this.vertxClient.optionsAbs(absoluteURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest optionsAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.optionsAbs(absoluteURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClient optionsNow(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.optionsNow(port, host, requestURI, responseHandler);
        return this;
    }

    @Override
    public HttpClient optionsNow(RequestOptions options, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.optionsNow(options, responseHandler);
        return this;
    }

    @Override
    public HttpClient optionsNow(String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.optionsNow(requestURI, responseHandler);
        return this;
    }

    @Override
    public HttpClient optionsNow(String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        this.vertxClient.optionsNow(host, requestURI, responseHandler);
        return this;
    }

    @Override
    public HttpClientRequest post(int port, String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.post(port, host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest post(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.post(port, host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest post(RequestOptions post)
    {
        HttpClientRequest req = this.vertxClient.post(post);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest post(RequestOptions post, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.post(post, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest post(String requestURI)
    {
        HttpClientRequest req = this.vertxClient.post(requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest post(String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.post(requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest post(String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.post(host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest post(String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.post(host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest postAbs(String absoluteURI)
    {
        HttpClientRequest req = this.vertxClient.postAbs(absoluteURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest postAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.postAbs(absoluteURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest put(int port, String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.put(port, host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest put(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.put(port, host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest put(RequestOptions put)
    {
        HttpClientRequest req = this.vertxClient.put(put);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest put(RequestOptions put, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.put(put, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest put(String requestURI)
    {
        HttpClientRequest req = this.vertxClient.put(requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest put(String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.put(requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest put(String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.put(host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest put(String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.put(host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest putAbs(String absoluteURI)
    {
        HttpClientRequest req = this.vertxClient.putAbs(absoluteURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest putAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.putAbs(absoluteURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public java.util.function.Function<HttpClientResponse,Future<HttpClientRequest>> redirectHandler()
    {
        return this.vertxClient.redirectHandler();
    }

    @Override
    public HttpClient redirectHandler(java.util.function.Function<HttpClientResponse,Future<HttpClientRequest>> handler)
    {
        this.vertxClient.redirectHandler(handler);
        return this;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, int port, String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.request(method, port, host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.request(method, port, host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, RequestOptions options)
    {
        HttpClientRequest req = this.vertxClient.request(method, options);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, RequestOptions options, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.request(method, options, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, SocketAddress serverAddress, int port, String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.request(method, serverAddress, port, host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, SocketAddress serverAddress, int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.request(method, serverAddress, port, host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, SocketAddress serverAddress, RequestOptions options)
    {
        HttpClientRequest req = this.vertxClient.request(method, serverAddress, options);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, SocketAddress serverAddress, RequestOptions options, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.request(method, serverAddress, options, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.request(method, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.request(method, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, String host, String requestURI)
    {
        HttpClientRequest req = this.vertxClient.request(method, host, requestURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, String host, String requestURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.request(method, host, requestURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest requestAbs(HttpMethod method, SocketAddress serverAddress, String absoluteURI)
    {
        HttpClientRequest req = this.vertxClient.requestAbs(method, serverAddress, absoluteURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest requestAbs(HttpMethod method, SocketAddress serverAddress, String absoluteURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.requestAbs(method, serverAddress, absoluteURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest requestAbs(HttpMethod method, String absoluteURI)
    {
        HttpClientRequest req = this.vertxClient.requestAbs(method, absoluteURI);
        this.addHeaders(req);
        return req;
    }

    @Override
    public HttpClientRequest requestAbs(HttpMethod method, String absoluteURI, Handler<HttpClientResponse> responseHandler)
    {
        HttpClientRequest req = this.vertxClient.requestAbs(method, absoluteURI, responseHandler);
        this.addHeaders(req);
        return req;
    }

    @Override
    public void webSocket(int port, String host, String requestURI, Handler<AsyncResult<WebSocket>> handler)
    {
        this.vertxClient.webSocket(port, host, requestURI, handler);
    }

    @Override
    public HttpClient websocket(int port, String host, String requestURI, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(port, host, requestURI, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(int port, String host, String requestURI, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(port, host, requestURI, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(port, host, requestURI, headers, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(port, host, requestURI, headers, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(port, host, requestURI, headers, version, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(port, host, requestURI, headers, version, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(port, host, requestURI, headers, version, subProtocols, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(port, host, requestURI, headers, version, subProtocols, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions options, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(options, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions options, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(options, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions options, MultiMap headers, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(options, headers, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions options, MultiMap headers, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(options, headers, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions options, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(options, headers, version, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions options, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(options, headers, version, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions options, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(options, headers, version, subProtocols, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions options, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(options, headers, version, subProtocols, wsConnect, failureHandler);
        return this;
    }

    @Override
    public void webSocket(String requestURI, Handler<AsyncResult<WebSocket>> handler)
    {
        this.vertxClient.webSocket(requestURI, handler);
    }

    @Override
    public HttpClient websocket(String requestURI, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(requestURI, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(String requestURI, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(requestURI, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(String requestURI, MultiMap headers, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(requestURI, headers, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(String requestURI, MultiMap headers, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(requestURI, headers, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(String requestURI, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(requestURI, headers, version, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(String requestURI, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(requestURI, headers, version, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(requestURI, headers, version, subProtocols, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(requestURI, headers, version, subProtocols, wsConnect, failureHandler);
        return this;
    }

    @Override
    public void webSocket(String host, String requestURI, Handler<AsyncResult<WebSocket>> handler)
    {
        this.vertxClient.webSocket(host, requestURI, handler);
    }

    @Override
    public void webSocket(WebSocketConnectOptions options, Handler<AsyncResult<WebSocket>> handler)
    {
        this.vertxClient.webSocket(options, handler);
    }

    @Override
    public HttpClient websocket(String host, String requestURI, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(host, requestURI, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(String host, String requestURI, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(host, requestURI, wsConnect, failureHandler);
        return this;
    }
    @Override
    public HttpClient websocket(String host, String requestURI, MultiMap headers, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(host, requestURI, headers, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(String host, String requestURI, MultiMap headers, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(host, requestURI, headers, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(String host, String requestURI, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(host, requestURI, headers, version, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(String host, String requestURI, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(host, requestURI, headers, version, wsConnect, failureHandler);
        return this;
    }

    @Override
    public HttpClient websocket(String host, String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect)
    {
        this.vertxClient.websocket(host, requestURI, headers, version, subProtocols, wsConnect);
        return this;
    }

    @Override
    public HttpClient websocket(String host, String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocket(host, requestURI, headers, version, subProtocols, wsConnect, failureHandler);
        return this;
    }

    @Override
    public void webSocketAbs(String url, MultiMap headers, WebsocketVersion version, List<String> subProtocols, Handler<AsyncResult<WebSocket>> handler)
    {
        this.vertxClient.webSocketAbs(url, headers, version, subProtocols, handler);
    }

    @Override
    public HttpClient websocketAbs(String url, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler)
    {
        this.vertxClient.websocketAbs(url, headers, version, subProtocols, wsConnect, failureHandler);
        return this;
    }

    @Override
    public ReadStream<WebSocket> websocketStream(int port, String host, String requestURI)
    {
        return this.vertxClient.websocketStream(port, host, requestURI);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(int port, String host, String requestURI, MultiMap headers)
    {
        return this.vertxClient.websocketStream(port, host, requestURI, headers);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version)
    {
        return this.vertxClient.websocketStream(port, host, requestURI, headers, version);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols)
    {
        return this.vertxClient.websocketStream(port, host, requestURI, headers, version, subProtocols);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(RequestOptions options)
    {
        return this.vertxClient.websocketStream(options);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(RequestOptions options, MultiMap headers)
    {
        return this.vertxClient.websocketStream(options, headers);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(RequestOptions options, MultiMap headers, WebsocketVersion version)
    {
        return this.vertxClient.websocketStream(options, headers, version);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(RequestOptions options, MultiMap headers, WebsocketVersion version, String subProtocols)
    {
        return this.vertxClient.websocketStream(options, headers, version, subProtocols);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String requestURI)
    {
        return this.vertxClient.websocketStream(requestURI);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String requestURI, MultiMap headers)
    {
        return this.vertxClient.websocketStream(requestURI, headers);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String requestURI, MultiMap headers, WebsocketVersion version)
    {
        return this.vertxClient.websocketStream(requestURI, headers, version);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols)
    {
        return this.vertxClient.websocketStream(requestURI, headers, version, subProtocols);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String host, String requestURI)
    {
        return this.vertxClient.websocketStream(host, requestURI);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String host, String requestURI, MultiMap headers)
    {
        return this.vertxClient.websocketStream(host, requestURI, headers);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String host, String requestURI, MultiMap headers, WebsocketVersion version)
    {
        return this.vertxClient.websocketStream(host, requestURI, headers, version);
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String host, String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols)
    {
        return this.vertxClient.websocketStream(host, requestURI, headers, version, subProtocols);
    }

    @Override
    public ReadStream<WebSocket> websocketStreamAbs(String url, MultiMap headers, WebsocketVersion version, String subProtocols)
    {
        return this.vertxClient.websocketStream(url, headers, version, subProtocols);
    }
}
