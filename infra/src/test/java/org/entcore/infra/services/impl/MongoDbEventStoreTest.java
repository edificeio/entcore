/*
 * Copyright © "Open Digital Education", 2024
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

package org.entcore.infra.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.data.FileResolver;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for MongoDbEventStore focusing on event validation and storage in correct collections.
 * Tests validate that properly formatted events go to the "events" collection,
 * while malformed events go to "events_format_error" collection.
 * 
 * These tests use a mock MongoDB backend to avoid Docker dependencies.
 */
@RunWith(VertxUnitRunner.class)
public class MongoDbEventStoreTest {
    
    private static Vertx vertx;
    private static MongoDbEventStore eventStore;
    private static final String EVENTS_COLLECTION = "events";
    private static final String FORMAT_ERROR_COLLECTION = "events_format_error";
    
    // Track which collection events are stored in
    private static final List<StoredEvent> storedEvents = new ArrayList<>();
    
    private static class StoredEvent {
        String collection;
        JsonObject event;
        
        StoredEvent(String collection, JsonObject event) {
            this.collection = collection;
            this.event = event;
        }
    }
    
    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        vertx = Vertx.vertx();

        MongoDb.getInstance().init(vertx.eventBus(), "mongodb.save");
        
        // Configure FileResolver to point to the resources directory
        // The schema file is now in the common module (mutualized dependency)
        String projectBasePath = new File("").getAbsolutePath();
        String resourcesPath = new File(projectBasePath + "/../common/src/main/resources/").getAbsolutePath() + "/";
        FileResolver.getInstance().setBasePath(resourcesPath);
        
        // Mock MongoDB eventbus consumer to capture save operations
        vertx.eventBus().consumer("mongodb.save", (Handler<Message<JsonObject>>) message -> {
            JsonObject body = message.body();
            String collection = body.getString("collection");
            JsonObject document = body.getJsonObject("document");
            
            // Track which collection the event was stored in
            storedEvents.add(new StoredEvent(collection, document));
            
            // Simulate successful save
            message.reply(new JsonObject().put("status", "ok"));
        });
        
        // Initialize event store
        eventStore = new MongoDbEventStore(vertx);
        
        // Wait for validator to be loaded (async initialization)
        final Async async = context.async();
        vertx.setTimer(1500, id -> async.complete());
        async.await();
    }
    
    @AfterClass
    public static void tearDown(TestContext context) {
        if (vertx != null) {
            vertx.close(context.asyncAssertSuccess());
        }
    }
    
    /**
     * Clear all stored events before each test to ensure test isolation.
     * This prevents test interference by starting each test with a clean slate.
     */
    @Before
    public void clearStoredEvents() {
        storedEvents.clear();
    }
    
    /**
     * Helper method to create a valid event with all required fields
     */
    private JsonObject createValidEvent() {
        return new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("module", "auth")
            .put("date", System.currentTimeMillis());
    }
    
    /**
     * Helper to wait and find events stored in a specific collection
     */
    private List<StoredEvent> getEventsInCollection(String collection) {
        List<StoredEvent> result = new ArrayList<>();
        for (StoredEvent se : storedEvents) {
            if (collection.equals(se.collection)) {
                result.add(se);
            }
        }
        return result;
    }
    
    // ==================== VALID EVENT TESTS ====================
    
    @Test
    public void testStoreValidEventWithAllRequiredFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject validEvent = createValidEvent();
        
        eventStore.store(validEvent, result -> {
            context.assertTrue(result.isRight(), "Valid event should be stored successfully");
            
            // Verify event was stored in correct collection
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, "Event should be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreValidEventWithOptionalFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject validEvent = createValidEvent()
            .put("userId", "user123")
            .put("profil", "Teacher")
            .put("structures", new JsonArray().add("struct1"))
            .put("classes", new JsonArray().add("class1"))
            .put("groups", new JsonArray().add("group1"))
            .put("ua", "Mozilla/5.0")
            .put("ip", "192.168.1.1")
            .put("sessionId", 12345.0)
            .put("referer", "https://example.com");
        
        eventStore.store(validEvent, result -> {
            context.assertTrue(result.isRight(), "Valid event with optional fields should be stored");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, "Event with optional fields should be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreMultipleValidEvents(TestContext context) {
        final Async async = context.async();
        
        final AtomicInteger successCount = new AtomicInteger(0);
        final int totalEvents = 5;
        
        for (int i = 0; i < totalEvents; i++) {
            JsonObject event = createValidEvent();
            eventStore.store(event, result -> {
                context.assertTrue(result.isRight());
                if (successCount.incrementAndGet() == totalEvents) {
                    // Verify all events were stored
                    vertx.setTimer(500, id -> {
                        List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                        context.assertTrue(events.size() >= totalEvents, 
                            "All " + totalEvents + " events should be stored");
                        async.complete();
                    });
                }
            });
        }
    }
    
    // ==================== INVALID EVENT TESTS - MISSING REQUIRED FIELDS ====================
    
    @Test
    public void testStoreEventMissingEventType(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("module", "auth")
            .put("date", System.currentTimeMillis());
        // Missing "event-type"
        
        eventStore.store(invalidEvent, result -> {
            // Store should still succeed but event goes to error collection
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            // Verify event was stored in error collection
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, "Event should be in format error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventMissingModule(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("date", System.currentTimeMillis());
        // Missing "module"
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, "Event missing module should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventMissingDate(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("module", "auth");
        // Missing "date"
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, "Event missing date should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }

    
    @Test
    public void testStoreEventMissingAllRequiredFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = new JsonObject()
            .put("userId", "user123")
            .put("custom-field", "value");
        // Missing all required fields: _id, event-type, module, date
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event missing all required fields should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    // ==================== INVALID EVENT TESTS - WRONG DATA TYPES ====================
    
    @Test
    public void testStoreEventWithWrongTypeForEventType(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", 12345) // Should be string, not number
            .put("module", "auth")
            .put("date", System.currentTimeMillis());
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with wrong type for event-type should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithWrongTypeForModule(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("module", new JsonObject().put("name", "auth")) // Should be string, not object
            .put("date", System.currentTimeMillis());
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with wrong type for module should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithWrongTypeForDate(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("module", "auth")
            .put("date", "2024-01-01"); // Should be long/number, not string
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with wrong type for date should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithWrongTypeForId(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = new JsonObject()
            .put("_id", 12345) // Should be string, not number
            .put("event-type", "LOGIN")
            .put("module", "auth")
            .put("date", System.currentTimeMillis());
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with wrong type for _id should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithWrongTypeForSessionId(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = createValidEvent()
            .put("sessionId", "not-a-number"); // Should be double/number
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with wrong type for sessionId should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithObjectInsteadOfString(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = createValidEvent()
            .put("userId", new JsonObject().put("id", "user1")); // Should be string, not object
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with object instead of string should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithArrayInsteadOfString(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = createValidEvent()
            .put("profil", new JsonArray().add("Teacher").add("Admin")); // Should be string, not array
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with array instead of string should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithBooleanInsteadOfDouble(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = createValidEvent()
            .put("nb_attachments", true); // Should be double, not boolean
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with boolean instead of double should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    // ==================== EDGE CASES AND BOUNDARY CONDITIONS ====================
    
    @Test
    public void testStoreEmptyEvent(TestContext context) {
        final Async async = context.async();
        
        JsonObject emptyEvent = new JsonObject();
        
        eventStore.store(emptyEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, "Empty event should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithNullValues(TestContext context) {
        final Async async = context.async();
        
        JsonObject eventWithNulls = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .putNull("event-type") // Explicit null value
            .put("module", "auth")
            .put("date", System.currentTimeMillis());
        
        eventStore.store(eventWithNulls, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with null required field should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithEmptyStringValues(TestContext context) {
        final Async async = context.async();
        
        JsonObject eventWithEmptyStrings = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "") // Empty string but technically valid type
            .put("module", "")
            .put("date", System.currentTimeMillis());
        
        eventStore.store(eventWithEmptyStrings, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            // Empty strings are valid strings from a type perspective
            // The validator checks types, not content validity
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, 
                    "Event with empty strings should be in events collection (type is valid)");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithExtraUnknownFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject eventWithExtraFields = createValidEvent()
            .put("unknownField1", "value1")
            .put("unknownField2", 12345)
            .put("unknownField3", new JsonObject().put("nested", "value"));
        
        eventStore.store(eventWithExtraFields, result -> {
            context.assertTrue(result.isRight(), "Event with extra fields should be stored");
            
            // Schema has "additionalProperties": true, so extra fields are allowed
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, 
                    "Event with extra fields should be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithVeryLargeDate(TestContext context) {
        final Async async = context.async();
        
        JsonObject eventWithLargeDate = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("module", "auth")
            .put("date", Long.MAX_VALUE);
        
        eventStore.store(eventWithLargeDate, result -> {
            context.assertTrue(result.isRight(), "Event with large date should be stored");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, 
                    "Event with large date should be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithNegativeDate(TestContext context) {
        final Async async = context.async();
        
        JsonObject eventWithNegativeDate = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("module", "auth")
            .put("date", -1L);
        
        eventStore.store(eventWithNegativeDate, result -> {
            context.assertTrue(result.isRight(), "Event with negative date should be stored");
            
            // Schema doesn't enforce positive numbers
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, 
                    "Event with negative date should be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithZeroDate(TestContext context) {
        final Async async = context.async();
        
        JsonObject eventWithZeroDate = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("module", "auth")
            .put("date", 0L);
        
        eventStore.store(eventWithZeroDate, result -> {
            context.assertTrue(result.isRight(), "Event with zero date should be stored");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, 
                    "Event with zero date should be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithNegativeNumericValues(TestContext context) {
        final Async async = context.async();
        
        JsonObject eventWithNegativeValues = createValidEvent()
            .put("nb_attachments", -5.0)
            .put("nb_videos", -10.0);
        
        eventStore.store(eventWithNegativeValues, result -> {
            context.assertTrue(result.isRight(), "Event with negative counts should be stored");
            
            // Schema doesn't enforce positive numbers for counts
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, 
                    "Event with negative numeric values should be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreEventWithStringInsteadOfDouble(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = createValidEvent()
            .put("duration", "not a number"); // Should be double
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with string instead of double should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    @Test
    public void testStoreMixOfValidAndInvalidEvents(TestContext context) {
        final Async async = context.async();
        
        final AtomicInteger completedCount = new AtomicInteger(0);
        final int totalEvents = 4;
        
        // Valid event 1
        eventStore.store(createValidEvent(), result -> {
            context.assertTrue(result.isRight());
            if (completedCount.incrementAndGet() == totalEvents) {
                verifyMixedResults(context, async);
            }
        });
        
        // Invalid event 1 - missing event-type
        eventStore.store(new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("module", "auth")
            .put("date", System.currentTimeMillis()), result -> {
            context.assertTrue(result.isRight());
            if (completedCount.incrementAndGet() == totalEvents) {
                verifyMixedResults(context, async);
            }
        });
        
        // Valid event 2
        eventStore.store(createValidEvent(), result -> {
            context.assertTrue(result.isRight());
            if (completedCount.incrementAndGet() == totalEvents) {
                verifyMixedResults(context, async);
            }
        });
        
        // Invalid event 2 - wrong type
        eventStore.store(new JsonObject()
            .put("_id", 12345)
            .put("event-type", "LOGIN")
            .put("module", "auth")
            .put("date", System.currentTimeMillis()), result -> {
            context.assertTrue(result.isRight());
            if (completedCount.incrementAndGet() == totalEvents) {
                verifyMixedResults(context, async);
            }
        });
    }
    
    private void verifyMixedResults(TestContext context, Async async) {
        vertx.setTimer(500, id -> {
            List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
            List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
            
            context.assertTrue(validEvents.size() >= 2, 
                "Should have at least 2 valid events, got " + validEvents.size());
            context.assertTrue(errorEvents.size() >= 2, 
                "Should have at least 2 error events, got " + errorEvents.size());
            async.complete();
        });
    }
    
    // ==================== COMPREHENSIVE VALIDATION SCENARIOS ====================
    
    /**
     * Test multiple invalid type scenarios at once
     */
    @Test
    public void testStoreEventWithMultipleInvalidTypes(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = new JsonObject()
            .put("_id", 999) // Wrong type (number instead of string)
            .put("event-type", true) // Wrong type (boolean instead of string)
            .put("module", new JsonArray()) // Wrong type (array instead of string)
            .put("date", "not-a-date"); // Wrong type (string instead of long)
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with multiple type errors should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    /**
     * Test that only one required field missing still causes validation failure
     */
    @Test
    public void testStoreEventWithOnlyOneRequiredFieldMissing(TestContext context) {
        final Async async = context.async();
        final AtomicInteger completedCount = new AtomicInteger(0);
        final int totalTests = 3;
        
        // Test each required field independently
        
        // Missing _id
        eventStore.store(new JsonObject()
            .put("event-type", "LOGIN")
            .put("module", "auth")
            .put("date", System.currentTimeMillis()), result -> {
            context.assertTrue(result.isRight());
            if (completedCount.incrementAndGet() == totalTests) {
                verifyAllInError(context, async, totalTests);
            }
        });
        
        // Missing event-type
        eventStore.store(new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("module", "auth")
            .put("date", System.currentTimeMillis()), result -> {
            context.assertTrue(result.isRight());
            if (completedCount.incrementAndGet() == totalTests) {
                verifyAllInError(context, async, totalTests);
            }
        });
        
        // Missing module
        eventStore.store(new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("date", System.currentTimeMillis()), result -> {
            context.assertTrue(result.isRight());
            if (completedCount.incrementAndGet() == totalTests) {
                verifyAllInError(context, async, totalTests);
            }
        });
        
        // Missing date
        eventStore.store(new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("module", "auth"), result -> {
            context.assertTrue(result.isRight());
            if (completedCount.incrementAndGet() == totalTests) {
                verifyAllInError(context, async, totalTests);
            }
        });
    }
    
    private void verifyAllInError(TestContext context, Async async, int expectedCount) {
        vertx.setTimer(500, id -> {
            List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
            context.assertTrue(errorEvents.size() >= expectedCount, 
                "Should have at least " + expectedCount + " error events, got " + errorEvents.size());
            async.complete();
        });
    }
    
    /**
     * Test complex nested structures in optional fields
     */
    @Test
    public void testStoreEventWithComplexNestedStructures(TestContext context) {
        final Async async = context.async();
        
        JsonObject eventWithComplexStructures = createValidEvent()
            .put("shares", new JsonObject()
                .put("userId1", new JsonObject().put("right", "read"))
                .put("userId2", new JsonObject().put("right", "write"))
                .encode()) // shares should be string with JSON format
            .put("resource_type", "document")
            .put("resource_id", "doc123");
        
        eventStore.store(eventWithComplexStructures, result -> {
            context.assertTrue(result.isRight(), "Event with complex structures should be stored");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, 
                    "Event with complex nested structures should be in events collection");
                async.complete();
            });
        });
    }
    
    /**
     * Test event with all optional fields properly populated
     */
    @Test
    public void testStoreEventWithAllFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject fullEvent = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "RESOURCE_ACCESS")
            .put("module", "workspace")
            .put("date", System.currentTimeMillis())
            .put("userId", "user-abc-123")
            .put("profil", "Teacher")
            .put("structures", new JsonArray().add("struct1"))
            .put("classes", new JsonArray().add("class1"))
            .put("groups", new JsonArray().add("group1"))
            .put("ua", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .put("ip", "192.168.1.100")
            .put("service", "workspace-service")
            .put("connector-type", "SAML")
            .put("cas-type", "ENT")
            .put("referer", "https://example.com/previous")
            .put("sessionId", 987654.0)
            .put("resource_type", "document")
            .put("resource_id", "doc-456")
            .put("nb_attachments", 3.0)
            .put("nb_sounds", 1.0)
            .put("nb_external_links", 2.0)
            .put("nb_images", 5.0)
            .put("nb_formulae", 0.0)
            .put("nb_embedded", 1.0)
            .put("nb_videos", 2.0)
            .put("nb_internal_links", 4.0)
            .put("resource-type", "doc")
            .put("useradmin", "true")
            .put("shares", "{\"user1\":{\"right\":\"read\"}}")
            .put("share_profiles", "Teacher")
            .put("share_rights", "read,write")
            .put("sms_id", "sms-123")
            .put("phone", "+33612345678")
            .put("adapter", "default-adapter")
            .put("video_id", "video-789")
            .put("duration", 120.5)
            .put("weight", 0.85)
            .put("device_type", "mobile")
            .put("source", "web")
            .put("function", "download")
            .put("answer", "Success");
        
        eventStore.store(fullEvent, result -> {
            context.assertTrue(result.isRight(), "Event with all fields should be stored");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, 
                    "Event with all fields should be in events collection");
                // Verify the stored event has all fields
                if (!events.isEmpty()) {
                    JsonObject stored = events.get(events.size() - 1).event;
                    context.assertEquals("RESOURCE_ACCESS", stored.getString("event-type"));
                    context.assertEquals("workspace", stored.getString("module"));
                    context.assertEquals(3.0, stored.getDouble("nb_attachments"));
                }
                async.complete();
            });
        });
    }
    
    /**
     * Test invalid format for special string formats
     */
    @Test
    public void testStoreEventWithInvalidUriFormat(TestContext context) {
        final Async async = context.async();
        
        // Note: The schema defines format but Vert.x validator may not strictly enforce custom formats
        // This test documents the behavior
        JsonObject eventWithBadUri = createValidEvent()
            .put("referer", "not a valid uri!!!"); // Invalid URI format
        
        eventStore.store(eventWithBadUri, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            // Format validation might be lenient depending on validator configuration
            async.complete();
        });
    }
    
    /**
     * Test event with mixed valid and invalid optional fields
     */
    @Test
    public void testStoreEventWithMixedValidAndInvalidOptionalFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject mixedEvent = createValidEvent()
            .put("userId", "valid-user-id") // Valid
            .put("profil", 12345) // Invalid: should be string
            .put("structures", "valid-structure") // Valid
            .put("sessionId", "invalid-session"); // Invalid: should be double
        
        eventStore.store(mixedEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with invalid optional field types should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    /**
     * Test that validation catches boolean where double is expected
     */
    @Test
    public void testStoreEventWithBooleanForNumericFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = createValidEvent()
            .put("nb_images", false) // Should be double
            .put("weight", true); // Should be double
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with boolean for numeric fields should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    /**
     * Test that validation catches arrays where strings are expected
     */
    @Test
    public void testStoreEventWithArraysForStringFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = createValidEvent()
            .put("userId", new JsonArray().add("user1").add("user2")) // Should be string
            .put("device_type", new JsonArray().add("mobile")); // Should be string
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with arrays for string fields should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    /**
     * Test that validation catches objects where strings are expected
     */
    @Test
    public void testStoreEventWithObjectsForStringFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = createValidEvent()
            .put("service", new JsonObject().put("name", "my-service")) // Should be string
            .put("source", new JsonObject().put("type", "web")); // Should be string
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with objects for string fields should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    /**
     * Test that validation catches strings where numbers are expected
     */
    @Test
    public void testStoreEventWithStringsForNumericFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = createValidEvent()
            .put("nb_videos", "five") // Should be double
            .put("duration", "two minutes"); // Should be double
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with strings for numeric fields should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    /**
     * Test special case: date as integer vs long
     */
    @Test
    public void testStoreEventWithIntegerDate(TestContext context) {
        final Async async = context.async();
        
        JsonObject eventWithIntDate = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("event-type", "LOGIN")
            .put("module", "auth")
            .put("date", 1234567890); // Integer instead of Long
        
        eventStore.store(eventWithIntDate, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            // Integer should be accepted as a numeric type
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, 
                    "Event with integer date should be in events collection");
                async.complete();
            });
        });
    }
    
    /**
     * Test that partial required fields scenario
     */
    @Test
    public void testStoreEventWithTwoOutOfFourRequiredFields(TestContext context) {
        final Async async = context.async();
        
        JsonObject invalidEvent = new JsonObject()
            .put("_id", UUID.randomUUID().toString())
            .put("module", "auth");
        // Missing event-type and date
        
        eventStore.store(invalidEvent, result -> {
            context.assertTrue(result.isRight(), "Store operation should succeed");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> errorEvents = getEventsInCollection(FORMAT_ERROR_COLLECTION);
                List<StoredEvent> validEvents = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(errorEvents.size() >= 1, 
                    "Event with only 2/4 required fields should be in error collection");
                context.assertEquals(0, validEvents.size(), "Invalid event should NOT be in events collection");
                async.complete();
            });
        });
    }
    
    /**
     * Test validation with numeric overflow scenarios
     */
    @Test
    public void testStoreEventWithExtremeNumericValues(TestContext context) {
        final Async async = context.async();
        
        JsonObject eventWithExtremeValues = createValidEvent()
            .put("sessionId", Double.MAX_VALUE)
            .put("nb_attachments", Double.MIN_VALUE)
            .put("weight", 0.0000000001);
        
        eventStore.store(eventWithExtremeValues, result -> {
            context.assertTrue(result.isRight(), "Event with extreme numeric values should be stored");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(events.size() >= 1, 
                    "Event with extreme numeric values should be in events collection");
                async.complete();
            });
        });
    }
    
    /**
     * Test validation doesn't fail on valid events that look suspicious
     */
    @Test
    public void testStoreEventWithValidButUnusualValues(TestContext context) {
        final Async async = context.async();
        
        // Create a very long string without using String.repeat() (Java 11+)
        StringBuilder longString = new StringBuilder("VERY_LONG_STRUCTURE_NAME_");
        for (int i = 0; i < 100; i++) {
            longString.append("X");
        }
        final List<String> structures = new ArrayList<>();
        for(int i = 0; i < 1000; i++) {
            structures.add(longString.toString());
        }
        
        JsonObject unusualButValid = new JsonObject("{\"event-type\":\"ACCESS\",\"module\":\"Timeline\",\"date\":1774621065942,\"userId\":\"9876\",\"structures\":[],\"classes\":[],\"groups\":[],\"ua\":\"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:149.0) Gecko/20100101 Firefox/149.0\",\"osName\":\"Ubuntu\",\"osVersion\":\"Unknown\",\"deviceType\":\"desktop\",\"deviceName\":\"Unknown\",\"ip\":\"192.168.192.1\"}");

        eventStore.store(unusualButValid, result -> {
            context.assertTrue(result.isRight(), "Event with unusual but valid values should be stored");
            
            vertx.setTimer(300, id -> {
                List<StoredEvent> events = getEventsInCollection(EVENTS_COLLECTION);
                context.assertTrue(!events.isEmpty(),
                    "Event with unusual but valid values should be in events collection");
                async.complete();
            });
        });
    }
}
