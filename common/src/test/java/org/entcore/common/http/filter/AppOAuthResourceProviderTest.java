package org.entcore.common.http.filter;

import io.netty.handler.codec.DecoderResult;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.junit.Test;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.entcore.common.http.filter.AppOAuthResourceProvider.getTokenHeader;
import static org.junit.Assert.*;

public class AppOAuthResourceProviderTest {

  @Test
  public void testGetTokenHeader() {
    assertFalse(
      "We should not get a token header when we have no authorization header",
      getTokenHeader(new DummyRequest()).isPresent());
    assertFalse(
      "We should not get a token header when we have an authorization method which is no OAuth or Bearer",
      getTokenHeader(new DummyRequest("Basic basic-token")).isPresent());
    assertEquals(
      "We should get a token header when we have an authorization method Bearer alone",
      "token",
      getTokenHeader(new DummyRequest("Bearer token")).get());
    assertEquals(
      "We should get a token header when we have an authorization method Bearer with another method",
      "token",
      getTokenHeader(new DummyRequest("Basic basic, Bearer token")).get());
    assertEquals(
      "We should get a token header when we have an authorization method Bearer with another method, no matter their order",
      "token",
      getTokenHeader(new DummyRequest("Bearer token, Basic basic")).get());
    assertEquals(
      "We should get a token header when we have an authorization method OAuth with another method",
      "token",
      getTokenHeader(new DummyRequest("Basic basic, OAuth token")).get());
    assertEquals(
      "We should get a token header when we have an authorization method OAuth with another method, no matter their order",
      "token",
      getTokenHeader(new DummyRequest("OAuth token, Basic basic")).get());
    assertEquals(
      "We should get a token header when we have an authorization method OAuth alone",
      "token",
      getTokenHeader(new DummyRequest("OAuth token")).get());
  }

  private static class DummyRequest implements HttpServerRequest {
    private final MultiMap headers;

    private DummyRequest(String auth) {
      this.headers = MultiMap.caseInsensitiveMultiMap();
      this.headers.add("Authorization", auth);
    }

    private DummyRequest() {
      this.headers = MultiMap.caseInsensitiveMultiMap();
    }

    @Override
    public String absoluteURI() {
      throw new UnsupportedOperationException("Unimplemented method 'absoluteURI'");
    }

    @Override
    public Future<Buffer> body() {
      throw new UnsupportedOperationException("Unimplemented method 'body'");
    }

    @Override
    public Future<Void> end() {
      throw new UnsupportedOperationException("Unimplemented method 'end'");
    }

    @Override
    public Future<NetSocket> toNetSocket() {
      throw new UnsupportedOperationException("Unimplemented method 'toNetSocket'");
    }

    @Override
    public long bytesRead() {
      throw new UnsupportedOperationException("Unimplemented method 'bytesRead'");
    }

    @Override
    public HttpConnection connection() {
      throw new UnsupportedOperationException("Unimplemented method 'connection'");
    }

    @Override
    public int cookieCount() {
      throw new UnsupportedOperationException("Unimplemented method 'cookieCount'");
    }

    @Override
    public Map<String, Cookie> cookieMap() {
      throw new UnsupportedOperationException("Unimplemented method 'cookieMap'");
    }

    @Override
    public Set<Cookie> cookies(String s) {
      throw new UnsupportedOperationException("Unimplemented method 'cookies'");
    }

    @Override
    public Set<Cookie> cookies() {
      throw new UnsupportedOperationException("Unimplemented method 'cookies'");
    }

    @Override
    public HttpServerRequest customFrameHandler(Handler<HttpFrame> arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'customFrameHandler'");
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'endHandler'");
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'exceptionHandler'");
    }

    @Override
    public HttpServerRequest fetch(long arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'fetch'");
    }

    @Override
    public MultiMap formAttributes() {
      throw new UnsupportedOperationException("Unimplemented method 'formAttributes'");
    }

    @Override
    public Cookie getCookie(String arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'getCookie'");
    }

    @Override
    public @Nullable Cookie getCookie(String s, String s1, String s2) {
      throw new UnsupportedOperationException("Unimplemented method 'getCookies'");
    }

    @Override
    public String getFormAttribute(String arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'getFormAttribute'");
    }

    @Override
    public Future<ServerWebSocket> toWebSocket() {
      throw new UnsupportedOperationException("Unimplemented method 'toWebSocket'");
    }

    @Override
    public String getHeader(String arg0) {
      return this.headers.get(arg0);
    }

    @Override
    public String getHeader(CharSequence arg0) {
      return this.headers.get(arg0);
    }

    @Override
    public HttpServerRequest setParamsCharset(String s) {
      throw new UnsupportedOperationException("Unimplemented method 'setParamsCharset'");
    }

    @Override
    public String getParamsCharset() {
      throw new UnsupportedOperationException("Unimplemented method 'getParamsCharset'");
    }

    @Override
    public String getParam(String arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'getParam'");
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'handler'");
    }

    @Override
    public MultiMap headers() {
      return this.headers;
    }

    @Override
    public String host() {
      throw new UnsupportedOperationException("Unimplemented method 'host'");
    }

    @Override
    public boolean isEnded() {
      throw new UnsupportedOperationException("Unimplemented method 'isEnded'");
    }

    @Override
    public boolean isExpectMultipart() {
      throw new UnsupportedOperationException("Unimplemented method 'isExpectMultipart'");
    }

    @Override
    public boolean isSSL() {
      throw new UnsupportedOperationException("Unimplemented method 'isSSL'");
    }

    @Override
    public SocketAddress localAddress() {
      throw new UnsupportedOperationException("Unimplemented method 'localAddress'");
    }

    @Override
    public HttpMethod method() {
      throw new UnsupportedOperationException("Unimplemented method 'method'");
    }

    @Override
    public MultiMap params() {
      throw new UnsupportedOperationException("Unimplemented method 'params'");
    }

    @Override
    public String path() {
      throw new UnsupportedOperationException("Unimplemented method 'path'");
    }

    @Override
    public HttpServerRequest pause() {
      throw new UnsupportedOperationException("Unimplemented method 'pause'");
    }

    @Override
    public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
      throw new UnsupportedOperationException("Unimplemented method 'peerCertificateChain'");
    }

    @Override
    public String query() {
      throw new UnsupportedOperationException("Unimplemented method 'query'");
    }

    @Override
    public @Nullable HostAndPort authority() {
      throw new UnsupportedOperationException("Unimplemented method 'authority'");
    }

    @Override
    public SocketAddress remoteAddress() {
      throw new UnsupportedOperationException("Unimplemented method 'remoteAddress'");
    }

    @Override
    public HttpServerResponse response() {
      throw new UnsupportedOperationException("Unimplemented method 'response'");
    }

    @Override
    public HttpServerRequest resume() {
      throw new UnsupportedOperationException("Unimplemented method 'resume'");
    }

    @Override
    public String scheme() {
      throw new UnsupportedOperationException("Unimplemented method 'scheme'");
    }

    @Override
    public HttpServerRequest setExpectMultipart(boolean arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'setExpectMultipart'");
    }

    @Override
    public SSLSession sslSession() {
      throw new UnsupportedOperationException("Unimplemented method 'sslSession'");
    }

    @Override
    public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'streamPriorityHandler'");
    }

    @Override
    public DecoderResult decoderResult() {
      throw new UnsupportedOperationException("Unimplemented method 'decoderResult'");
    }

    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'uploadHandler'");
    }

    @Override
    public String uri() {
      throw new UnsupportedOperationException("Unimplemented method 'uri'");
    }

    @Override
    public HttpVersion version() {
      throw new UnsupportedOperationException("Unimplemented method 'version'");
    }
  }
}
