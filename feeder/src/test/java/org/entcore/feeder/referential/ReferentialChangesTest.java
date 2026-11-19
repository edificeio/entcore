package org.entcore.feeder.referential;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Entities notified to referential after a manual action (referential POC, step 4 in fallback mode B). */
public class ReferentialChangesTest {

	@Test
	public void idsOfTheMessageAndOfItsData() {
		final JsonObject body = new JsonObject().put("action", "manual-add-group-users").put("groupId", "g1")
				.put("userIds", new JsonArray().add("u1").add("u2"))
				.put("data", new JsonObject().put("structureId", "s1").put("classIds", new JsonArray().add("c1")));

		final EntityChangedDTO changes = ReferentialChanges.of("manual-add-group-users", body, ok());

		assertEquals(Arrays.asList("u1", "u2"), changes.getUserIds());
		assertEquals(Arrays.asList("g1", "c1"), changes.getGroupIds());
		assertEquals(Collections.singletonList("s1"), changes.getStructureIds());
		assertFalse(changes.isFunctions());
	}

	@Test
	public void createdIdIsReadInTheReply() {
		final JsonObject reply = ok().put("result", new JsonArray().add(new JsonObject().put("id", "c9")));

		final EntityChangedDTO changes = ReferentialChanges.of("manual-create-class",
				new JsonObject().put("structureId", "s1").put("data", new JsonObject().put("name", "6A")), reply);

		assertEquals(Collections.singletonList("c9"), changes.getGroupIds());
		assertEquals(Collections.singletonList("s1"), changes.getStructureIds());
	}

	@Test
	public void functionCatalogChanges() {
		assertTrue(ReferentialChanges.of("manual-create-function", new JsonObject(), ok()).isFunctions());
	}

	/** Contract with referential (EntityChangedDTO record): exactly these JSON fields. */
	@Test
	public void serializedFields() throws Exception {
		final String json = new com.fasterxml.jackson.databind.ObjectMapper()
				.writeValueAsString(new EntityChangedDTO("manual-add-user"));
		assertEquals(new java.util.TreeSet<>(Arrays.asList("action", "userIds", "structureIds", "structureExternalIds",
				"groupIds", "subjectIds", "functions")), new JsonObject(json).fieldNames());
	}

	@Test
	public void notifiedOnlyAfterSuccess() {
		final RecordingPublisher publisher = new RecordingPublisher();
		final JsonObject body = new JsonObject().put("userId", "u1");

		new ReferentialNotifyingMessage(new FakeMessage(body), publisher, "manual-update-user").reply(ok());
		new ReferentialNotifyingMessage(new FakeMessage(body), publisher, "manual-update-user")
				.reply(new JsonObject().put("status", "error"));
		new ReferentialNotifyingMessage(new FakeMessage(new JsonObject()), publisher, "manual-create-tenant")
				.reply(ok());

		assertEquals("one success with known entities", 1, publisher.published.size());
		assertEquals(Collections.singletonList("u1"), publisher.published.get(0).getUserIds());
	}

	private static JsonObject ok() {
		return new JsonObject().put("status", "ok");
	}

	private static class RecordingPublisher implements ReferentialPublisher {
		private final List<EntityChangedDTO> published = new ArrayList<>();

		@Override
		public Future<Void> entityChanged(EntityChangedDTO changes) {
			published.add(changes);
			return Future.succeededFuture();
		}
	}

	private static class FakeMessage implements Message<JsonObject> {
		private final JsonObject body;

		FakeMessage(JsonObject body) {
			this.body = body;
		}

		@Override
		public String address() {
			return "entcore.feeder";
		}

		@Override
		public MultiMap headers() {
			return MultiMap.caseInsensitiveMultiMap();
		}

		@Override
		public JsonObject body() {
			return body;
		}

		@Override
		public String replyAddress() {
			return null;
		}

		@Override
		public boolean isSend() {
			return true;
		}

		@Override
		public void reply(Object message, DeliveryOptions options) {
		}

		@Override
		public <R> Future<Message<R>> replyAndRequest(Object message, DeliveryOptions options) {
			return Future.failedFuture("unused");
		}
	}
}
