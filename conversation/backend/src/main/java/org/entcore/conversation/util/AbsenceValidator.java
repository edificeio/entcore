package org.entcore.conversation.util;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Validation of the absence message payloads.
 */
public final class AbsenceValidator {

	private AbsenceValidator() {
	}

	/**
	 * Validates the payload of PUT /conversation/absence.
	 * A start date in the past is explicitly allowed, so only the ordering of the bounds is checked.
	 * @param body the request payload
	 * @return the error key, or null when the payload is valid
	 */
	public static String validatePayload(final JsonObject body) {
		if (body == null) {
			return "conversation.absence.invalid.payload";
		}
		final Object bodyJson = body.getValue("bodyJson");
		if (!(bodyJson instanceof JsonObject)) {
			return "conversation.absence.invalid.body";
		}
		if (Boolean.TRUE.equals(body.getBoolean("enabled"))
				&& ((JsonObject) bodyJson).getJsonArray("content", new JsonArray()).isEmpty()) {
			// A text is required to activate, an empty tiptap document is not one.
			return "conversation.absence.empty.body";
		}
		final String rawStartAt = body.getString("startAt");
		final String rawEndAt = body.getString("endAt");
		if (rawStartAt == null || rawEndAt == null) {
			return "conversation.absence.invalid.dates";
		}
		final Instant startAt;
		final Instant endAt;
		try {
			startAt = Instant.parse(rawStartAt);
			endAt = Instant.parse(rawEndAt);
		} catch (DateTimeParseException e) {
			return "conversation.absence.invalid.dates";
		}
		if (endAt.isBefore(startAt)) {
			return "conversation.absence.end.before.start";
		}
		return null;
	}
}
