package org.entcore.registry.services.impl;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DefaultLibraryServiceTest {
    HttpClient http;
    JsonObject config;
    HttpClientRequest request;
    HttpClientResponse response;

    @Captor
    ArgumentCaptor<Handler<HttpClientResponse>> handler;

    @Before
    public void prepare() {
        this.http = mock(HttpClient.class);
        this.handler = ArgumentCaptor.forClass(Handler.class);
        this.request = mock(HttpClientRequest.class);
        this.response = mock(HttpClientResponse.class);
        when(http.postAbs(any(String.class))).thenReturn(request);
    }

    @Test
    public void add_shouldDisabledMessage_WhenLibraryIsNotEnabled() {
        config = new LibraryConfigBuilder().setEnabled(false).value();
        DefaultLibraryService service = new DefaultLibraryService(http, config);
        CompletableFuture<JsonObject> future = service.publish(MultiMap.caseInsensitiveMultiMap(), Buffer.buffer());
        assertTrue(future.isDone());
        try {
            JsonObject json = future.get();
            assertEquals(false, json.getBoolean("success"));
            assertEquals(REASON.BAD_CONFIGURATION.name(), json.getString("reason"));
            assertEquals(MESSAGE.DISABLED.name(), json.getString("message"));
        } catch (InterruptedException | ExecutionException e) {
            fail("No exception expected");
        }
    }

    @Test
    public void add_shouldReturnApiUrlNotSetMessage_WhenLibraryApiUrlIsNotSet() {
        config = new LibraryConfigBuilder().setApiUrl(null).value();
        DefaultLibraryService service = new DefaultLibraryService(http, config);
        CompletableFuture<JsonObject> future = service.publish(MultiMap.caseInsensitiveMultiMap(), Buffer.buffer());
        assertTrue(future.isDone());
        try {
            JsonObject json = future.get();
            assertEquals(false, json.getBoolean("success"));
            assertEquals(REASON.BAD_CONFIGURATION.name(), json.getString("reason"));
            assertEquals(MESSAGE.API_URL_NOT_SET.name(), json.getString("message"));
        } catch (InterruptedException | ExecutionException e) {
            fail("No exception expected");
        }
    }

    @Test
    public void add_shouldReturnWrongTokenMessage_WhenLibraryTokenIsNotSet() {
        config = new LibraryConfigBuilder().setToken(null).value();
        DefaultLibraryService service = new DefaultLibraryService(http, config);
        CompletableFuture<JsonObject> future = service.publish(MultiMap.caseInsensitiveMultiMap(), Buffer.buffer());
        assertTrue(future.isDone());
        try {
            JsonObject json = future.get();
            assertEquals(false, json.getBoolean("success"));
            assertEquals(REASON.BAD_CONFIGURATION.name(), json.getString("reason"));
            assertEquals(MESSAGE.WRONG_TOKEN.name(), json.getString("message"));
        } catch (InterruptedException | ExecutionException e) {
            fail("No exception expected");
        }
    }
    @Test
    public void add_shouldSetTheAuthorizationHeaderToBearerTokenAThenEndRequest_GivenConfigToken_TokenA() {
        config = new LibraryConfigBuilder().setToken("tokenA").value();
        DefaultLibraryService service = new DefaultLibraryService(http, config);

        service.publish(MultiMap.caseInsensitiveMultiMap(), Buffer.buffer());

        verify(request).putHeader("Authorization", "Bearer tokenA");
        verify(request).end();
    }

    @Test
    public void add_shouldReturnWrongTokenMessage_WhenNotAuthenticatedOnLibraryApi() {
        config = new LibraryConfigBuilder().value();
        DefaultLibraryService service = new DefaultLibraryService(http, config);
        when(response.statusCode()).thenReturn(401);
        CompletableFuture<JsonObject> future = service.publish(MultiMap.caseInsensitiveMultiMap(), Buffer.buffer());
        verify(request).handler(handler.capture());
        handler.getValue().handle(response);
        assertTrue(future.isDone());
        try {
            JsonObject json = future.get();
            assertEquals(false, json.getBoolean("success"));
            assertEquals(REASON.LIBRARY.name(), json.getString("reason"));
            assertEquals(MESSAGE.WRONG_TOKEN.name(), json.getString("message"));
        } catch (InterruptedException | ExecutionException e) {
            fail("No exception expected");
        }
    }

    @Test
    public void add_shouldReturnOkMessage_WhenLibraryReturnsOk() {
        config = new LibraryConfigBuilder().value();
        DefaultLibraryService service = new DefaultLibraryService(http, config);
        when(response.statusCode()).thenReturn(200);
        CompletableFuture<JsonObject> future = service.publish(MultiMap.caseInsensitiveMultiMap(), Buffer.buffer());
        verify(request).handler(handler.capture());
        handler.getValue().handle(response);
        assertTrue(future.isDone());
        try {
            JsonObject json = future.get();
            assertEquals(true, json.getBoolean("success"));
            assertEquals(REASON.OK.name(), json.getString("reason"));
            assertEquals(MESSAGE.OK.name(), json.getString("message"));
        } catch (InterruptedException | ExecutionException e) {
            fail("No exception expected");
        }
    }

    @Test
    public void add_shouldReturnLibraryErrorMessage_WhenLibraryReturns500() {
        config = new LibraryConfigBuilder().value();
        DefaultLibraryService service = new DefaultLibraryService(http, config);
        when(response.statusCode()).thenReturn(500);
        CompletableFuture<JsonObject> future = service.publish(MultiMap.caseInsensitiveMultiMap(), Buffer.buffer());
        verify(request).handler(handler.capture());
        handler.getValue().handle(response);
        assertTrue(future.isDone());
        try {
            JsonObject json = future.get();
            assertEquals(false, json.getBoolean("success"));
            assertEquals(REASON.LIBRARY.name(), json.getString("reason"));
            assertEquals(MESSAGE.LIBRARY_KO.name(), json.getString("message"));
        } catch (InterruptedException | ExecutionException e) {
            fail("No exception expected");
        }
    }

    class LibraryConfigBuilder {
        boolean enabled = true;
        String apiUrl = "https://library.com";
        String token = "token";

        public LibraryConfigBuilder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public LibraryConfigBuilder setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public LibraryConfigBuilder setToken(String token) {
            this.token = token;
            return this;
        }

        public JsonObject value() {
            return new JsonObject()
                    .put("library-enabled", enabled)
                    .put("library-api-url", apiUrl)
                    .put("library-token", token);
        }
    }
}
