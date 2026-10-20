package org.entcore.timeline.controllers;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.mute.MuteHelper;
import org.entcore.common.notification.TimelineNotificationsLoader;
import org.entcore.timeline.controllers.helper.NotificationHelper;
import org.entcore.timeline.events.TimelineEventStore;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Characterization of POST /timeline/external/notification/:projectId, called in production by external partners.
 * Only the public route is exercised, so the same test runs unchanged against any version of the controller.
 */
public class ExternalNotificationsRouteTest {

    private static final JsonObject ELEA_TEMPLATE = new JsonObject()
            .put("type", "ELEA")
            .put("event-type", "EXTERNAL_NOTIFICATION")
            .put("template", "<span>{{body}}</span>")
            .put("push-notif", true);
    private static final JsonObject WORKSPACE_TEMPLATE = new JsonObject()
            .put("type", "WORKSPACE")
            .put("event-type", "SHARE-EXTERNAL")
            .put("template", "<span>{{body}}</span>")
            .put("push-notif", true);

    private static Vertx vertx;

    private TimelineController controller;
    private TimelineEventStore store;
    private NotificationHelper notificationHelper;
    private final List<JsonObject> storedEvents = new ArrayList<>();
    private JsonObject storeResult;
    private Set<String> muters;

    private static class Response {
        int status = 200;
        String body;
    }

    @BeforeClass
    public static void startVertx() throws Exception {
        vertx = Vertx.vertx();
        // Fresh loader bound to this Vert.x: out of a cluster, its registry is the local map "notificationsMap"
        setStatic(TimelineNotificationsLoader.class, "instance", null);
        final LocalMap<String, String> registry = vertx.sharedData().getLocalMap("notificationsMap");
        registry.put("elea.external_notification", ELEA_TEMPLATE.encode());
        registry.put("workspace.share-external", WORKSPACE_TEMPLATE.encode());
    }

    @AfterClass
    public static void stopVertx() throws Exception {
        setStatic(TimelineNotificationsLoader.class, "instance", null);
        vertx.close();
    }

    @Before
    public void setUp() throws Exception {
        storedEvents.clear();
        storeResult = new JsonObject().put("status", "ok").put("_id", "stored-id");
        muters = new HashSet<>();

        store = mock(TimelineEventStore.class);
        doAnswer(invocation -> {
            storedEvents.add(invocation.getArgument(0));
            invocation.<Handler<JsonObject>>getArgument(1).handle(storeResult);
            return null;
        }).when(store).add(any(JsonObject.class), any());

        final MuteHelper muteHelper = mock(MuteHelper.class);
        when(muteHelper.fetResourceMutesByEntId(anyString())).thenAnswer(invocation -> Future.succeededFuture(muters));

        notificationHelper = mock(NotificationHelper.class);
        doAnswer(invocation -> {
            invocation.<Handler<JsonArray>>getArgument(2).handle(new JsonArray());
            return null;
        }).when(notificationHelper).resolveUsersAndMarkDeferredRecipients(any(), any(), any());
        when(notificationHelper.extractDeferredUserIds(any())).thenReturn(new HashSet<>());

        controller = new TimelineController(new HashMap<>());
        controller.setNotificationHelper(notificationHelper);
        setField(controller, "store", store);
        setField(controller, "muteHelper", muteHelper);
        setField(controller, "vertx", vertx);
    }

    private static JsonObject payload() {
        return new JsonObject()
                .put("recipients", new JsonArray().add("user-1").add("user-2"))
                .put("body", "Corps de la notification")
                .put("senderId", "sender-1")
                .put("username", "Jean Martin")
                .put("subject", "Sujet")
                .put("resourceUri", "https://partner.example.fr/resource/42")
                .put("pushMobile", true)
                .put("mobileTitle", "Titre mobile")
                .put("mobileBody", "Corps mobile");
    }

    // --- rejected requests ---

    @Test
    public void testMissingProjectId_BadRequest() throws Exception {
        final Response response = call(null, payload());
        assertEquals(400, response.status);
        assertEquals("Invalid projectId", new JsonObject(response.body).getString("error"));
        verifyNoInteractions(store);
    }

    @Test
    public void testEmptyProjectId_BadRequest() throws Exception {
        final Response response = call("", payload());
        assertEquals(400, response.status);
        assertEquals("Invalid projectId", new JsonObject(response.body).getString("error"));
        verifyNoInteractions(store);
    }

    @Test
    public void testEmptyRecipients_BadRequest() throws Exception {
        final Response response = call("elea", payload().put("recipients", new JsonArray()));
        assertEquals(400, response.status);
        assertEquals("Invalid recipients or body", new JsonObject(response.body).getString("error"));
        verifyNoInteractions(store);
    }

    @Test
    public void testMissingBody_BadRequest() throws Exception {
        final JsonObject payload = payload();
        payload.remove("body");
        final Response response = call("elea", payload);
        assertEquals(400, response.status);
        assertEquals("Invalid recipients or body", new JsonObject(response.body).getString("error"));
        verifyNoInteractions(store);
    }

    @Test
    public void testUnknownTemplate_BadRequest() throws Exception {
        final Response response = call("unknown", payload());
        assertEquals(400, response.status);
        assertEquals("Invalid notification name: unknown.external_notification",
                new JsonObject(response.body).getString("error"));
        verifyNoInteractions(store);
    }

    // --- published notification ---

    @Test
    public void testFullPayload_OkAndStoredEvent() throws Exception {
        final Response response = call("elea", payload());
        assertEquals(200, response.status);
        assertNull(response.body);
        assertEquals(1, storedEvents.size());

        final JsonObject event = storedEvents.get(0);
        assertEquals("add", event.getString("action"));
        assertEquals("ELEA", event.getString("type"));
        assertEquals("EXTERNAL_NOTIFICATION", event.getString("event-type"));
        assertEquals(new JsonArray()
                .add(new JsonObject().put("userId", "user-1").put("unread", 1))
                .add(new JsonObject().put("userId", "user-2").put("unread", 1)), event.getJsonArray("recipients"));
        assertEquals(new JsonArray().add("user-1").add("user-2"), event.getJsonArray("recipientsIds"));
        assertTrue(event.getString("resource").matches("\\d+external_notification"));
        assertEquals("sender-1", event.getString("sender"));
        assertTrue(event.getBoolean("disableAntiFlood"));
        assertEquals(new JsonObject().put("title", "Titre mobile").put("body", "Corps mobile"),
                event.getJsonObject("pushNotif"));
        assertEquals(new JsonObject()
                .put("body", "Corps de la notification")
                .put("username", "Jean Martin")
                .put("uri", "/userbook/annuaire#sender-1#")
                .put("resourceUri", "https://partner.example.fr/resource/42")
                .put("subject", "Sujet")
                .put("projectId", "elea"), event.getJsonObject("params"));
        assertEquals("elea.external_notification", event.getString("notificationName"));
        assertEquals(ELEA_TEMPLATE, event.getJsonObject("notification"));
        assertEquals(new JsonObject()
                .put("Host", "ent.example.fr")
                .put("X-Forwarded-Proto", "https")
                .put("Accept-Language", "fr"), event.getJsonObject("request").getJsonObject("headers"));
    }

    @Test
    public void testFullPayload_ImmediateDispatchToAllRecipients() throws Exception {
        call("elea", payload());
        verify(notificationHelper).resolveUsersAndMarkDeferredRecipients(
                eq(new JsonArray().add("user-1").add("user-2")), any(), any());
        verify(notificationHelper, times(1)).sendImmediateNotifications(any(), any(), any(), any());
    }

    @Test
    public void testCustomResource_TemplateNameLowerCasedAndProjectIdKept() throws Exception {
        final Response response = call("WorkSpace", payload().put("resource", "share-external"));
        assertEquals(200, response.status);
        final JsonObject event = storedEvents.get(0);
        assertEquals("workspace.share-external", event.getString("notificationName"));
        assertEquals("WORKSPACE", event.getString("type"));
        assertTrue(event.getString("resource").matches("\\d+share-external"));
        assertEquals("WorkSpace", event.getJsonObject("params").getString("projectId"));
    }

    @Test
    public void testMinimalPayload_NoSenderNoSubjectNoPush() throws Exception {
        final JsonObject payload = new JsonObject()
                .put("recipients", new JsonArray().add("user-1"))
                .put("body", "Corps")
                // username without senderId and push without title are both ignored
                .put("username", "Jean Martin")
                .put("pushMobile", true)
                .put("mobileBody", "Corps mobile");
        final Response response = call("elea", payload);
        assertEquals(200, response.status);

        final JsonObject event = storedEvents.get(0);
        assertEquals("", event.getString("sender"));
        assertNull(event.getValue("pushNotif"));
        assertEquals(new JsonObject().put("body", "Corps").put("projectId", "elea"), event.getJsonObject("params"));
    }

    @Test
    public void testAllRecipientsMuted_StoredWithoutDispatch() throws Exception {
        muters.add("user-1");
        muters.add("user-2");
        final Response response = call("elea", payload());
        assertEquals(200, response.status);
        assertEquals(1, storedEvents.size());
        assertEquals(new JsonArray(), storedEvents.get(0).getJsonArray("recipientsIds"));
        verify(notificationHelper, never()).resolveUsersAndMarkDeferredRecipients(any(), any(), any());
        verify(notificationHelper, never()).sendImmediateNotifications(any(), any(), any(), any());
    }

    @Test
    public void testStoreError_StillOk() throws Exception {
        storeResult = new JsonObject().put("status", "error").put("message", "Invalid arguments.");
        final Response response = call("elea", payload());
        assertEquals(200, response.status);
        assertNull(response.body);
    }

    private Response call(final String projectId, final JsonObject body) throws Exception {
        final CompletableFuture<Response> done = new CompletableFuture<>();
        final Response response = new Response();

        final HttpServerResponse httpResponse = mock(HttpServerResponse.class, RETURNS_SELF);
        doAnswer(invocation -> {
            response.status = invocation.getArgument(0);
            return httpResponse;
        }).when(httpResponse).setStatusCode(anyInt());
        doAnswer(invocation -> {
            done.complete(response);
            return null;
        }).when(httpResponse).end();
        doAnswer(invocation -> {
            response.body = invocation.getArgument(0);
            done.complete(response);
            return null;
        }).when(httpResponse).end(anyString());

        final HttpServerRequest request = mock(HttpServerRequest.class);
        final MultiMap params = MultiMap.caseInsensitiveMultiMap();
        if (projectId != null) {
            params.add("projectId", projectId);
        }
        when(request.params()).thenReturn(params);
        when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap()
                .add("Host", "ent.example.fr")
                .add("X-Forwarded-Proto", "https")
                .add("Accept-Language", "fr"));
        when(request.response()).thenReturn(httpResponse);
        doAnswer(invocation -> {
            invocation.<Handler<Buffer>>getArgument(0).handle(Buffer.buffer(body.encode()));
            return request;
        }).when(request).bodyHandler(any());

        controller.externalNotifications(request);
        return done.get(5, TimeUnit.SECONDS);
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void setStatic(final Class<?> type, final String name, final Object value) throws Exception {
        final Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
