package org.entcore.timeline.util;

import fr.wseduc.webutils.security.XSSUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.StringUtils;

/**
 * Translation of the ELEA notification contract onto the payload of POST /timeline/external/notification/:projectId.
 * ELEA imposes its own format ({school, recipient, sender, subject, message, context}) and response envelope,
 * so the ENT adapts to it instead of asking ELEA to use the generic external API.
 */
public final class EleaNotificationAdapter {

	/** Project id of the elea.external_notification template. */
	public static final String ELEA_PROJECT_ID = "elea";
	/** Title of the mobile push. */
	public static final String ELEA_DISPLAY_NAME = "ELEA";
	/** Message returned to ELEA on success. */
	public static final String ELEA_SUCCESS_MESSAGE = "Notification envoyée avec succès.";

	/** Normalized errors of the ELEA contract: HTTP status, retry hint and message sent back to ELEA. */
	public enum EleaError {
		INVALID_PAYLOAD(400, false, "Le corps de la requête n'est pas un objet JSON valide."),
		INVALID_RECIPIENT(400, false, "L'identifiant du destinataire 'recipient' est introuvable ou invalide."),
		EMPTY_NOTIFICATION(400, false, "La notification ne contient ni sujet 'subject' ni message 'message'."),
		SERVICE_UNAVAILABLE(503, true, "Le service est momentanément indisponible."),
		INTERNAL_ERROR(500, true, "Erreur interne lors de l'envoi de la notification.");

		private final int status;
		private final boolean retry;
		private final String message;

		EleaError(final int status, final boolean retry, final String message) {
			this.status = status;
			this.retry = retry;
			this.message = message;
		}

		public int getStatus() {
			return status;
		}

		public boolean isRetry() {
			return retry;
		}

		public JsonObject toJson() {
			return new JsonObject()
					.put("success", false)
					.put("retry", retry)
					.put("error", new JsonObject().put("code", name()).put("message", message));
		}
	}

	private EleaNotificationAdapter() {
	}

	/**
	 * Reads the raw body of an ELEA request, with the same XSS sanitization as RequestUtils.bodyToJson.
	 * @param raw the request body
	 * @return the payload, or null when the body is not a JSON object
	 */
	public static JsonObject parse(final String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return null;
		}
		try {
			return new JsonObject(XSSUtils.stripXSS(raw));
		} catch (RuntimeException e) {
			return null;
		}
	}

	/**
	 * Only the recipient and some content are required: any other field, known or future, must never block ELEA.
	 * @param payload the parsed payload
	 * @return the error to send back, or null when the payload is valid
	 */
	public static EleaError validate(final JsonObject payload) {
		if (payload == null) {
			return EleaError.INVALID_PAYLOAD;
		}
		if (getRecipient(payload) == null) {
			return EleaError.INVALID_RECIPIENT;
		}
		if (getText(payload, "subject").isEmpty() && getText(payload, "message").isEmpty()) {
			return EleaError.EMPTY_NOTIFICATION;
		}
		return null;
	}

	/**
	 * @param payload the parsed payload
	 * @return the AAF externalId of the recipient, or null when it is missing or not a non-blank string
	 */
	public static String getRecipient(final JsonObject payload) {
		final String recipient = getText(payload, "recipient");
		return recipient.isEmpty() ? null : recipient;
	}

	/**
	 * The sender is optional: a missing or unknown sender never blocks the notification.
	 * @param payload the parsed payload
	 * @return the AAF externalId of the sender, or null when it is missing or not a non-blank string
	 */
	public static String getSender(final JsonObject payload) {
		final String sender = getText(payload, "sender");
		return sender.isEmpty() ? null : sender;
	}

	/**
	 * Builds the payload expected by the generic external notification flow.
	 * A resolved sender becomes the senderId/username pair, displayed with a link to its profile.
	 * school and context have no counterpart there.
	 * @param payload a payload accepted by {@link #validate(JsonObject)}
	 * @param recipientId the ENT id resolved from the recipient externalId
	 * @param sender the user resolved from the sender externalId, as {id, displayName}, or null when unknown
	 * @return the generic external notification payload
	 */
	public static JsonObject toExternalNotification(final JsonObject payload, final String recipientId,
			final JsonObject sender) {
		final String subject = getText(payload, "subject");
		final String message = getText(payload, "message");
		final JsonObject external = new JsonObject()
				.put("recipients", new JsonArray().add(recipientId))
				.put("subject", subject)
				.put("body", message)
				.put("pushMobile", true)
				.put("mobileTitle", ELEA_DISPLAY_NAME)
				.put("mobileBody", subject.isEmpty() ? StringUtils.stripHtmlTag(message) : subject);
		if (sender != null && !getText(sender, "id").isEmpty() && !getText(sender, "displayName").isEmpty()) {
			external.put("senderId", getText(sender, "id"))
					.put("username", getText(sender, "displayName"));
		}
		return external;
	}

	public static JsonObject success() {
		return new JsonObject()
				.put("success", true)
				.put("message", ELEA_SUCCESS_MESSAGE);
	}

	/** Tolerant read: a missing, null or non-string value is an empty text, never an error. */
	private static String getText(final JsonObject payload, final String field) {
		final Object value = payload.getValue(field);
		return value instanceof String ? ((String) value).trim() : "";
	}
}
