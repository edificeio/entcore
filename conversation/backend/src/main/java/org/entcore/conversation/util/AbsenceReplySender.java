package org.entcore.conversation.util;

import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.user.UserInfos;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

/**
 * Builds what an absence reply is sent and notified with, in place of the session and the request of the absent
 * user, who is not connected : the reply must read, in the timeline of its recipient, exactly as if the absent
 * user had answered by hand.
 */
public final class AbsenceReplySender {

	private AbsenceReplySender() {
	}

	/**
	 * Builds the sender of the reply from its resolved identity.
	 * The timeline notification reads the display name (title of the push) and the profile (directory link)
	 * on it : left empty, they would come out as {@code null} and {@code #null}.
	 * @param userId the absent user
	 * @param identity {@code {username, type}} as resolved from the directory, possibly null
	 * @return the sender, holding at least the user id
	 */
	public static UserInfos absentSender(final String userId, final JsonObject identity) {
		final UserInfos sender = new UserInfos();
		sender.setUserId(userId);
		if (identity != null) {
			sender.setUsername(identity.getString("username"));
			sender.setType(identity.getString("type"));
		}
		return sender;
	}

	/**
	 * Builds the request the reply is sent and notified with.
	 * <ul>
	 *   <li>{@code X-Forwarded-For} is not optional : the send records a transformation event, whose event store
	 *   resolves the caller ip, and {@code Renders.getIp} throws without it — before the transformation promise
	 *   completes, stalling the whole chain with no error surfaced.</li>
	 *   <li>{@code Host} and {@code X-Forwarded-Proto} are copied by the timeline into the links of the
	 *   notification mails ; without the scheme they would fall back to {@code http}.</li>
	 *   <li>{@code Accept-Language} is what the browser of the absent user would have sent.</li>
	 * </ul>
	 * @param work the dispatched payload, holding {@code host} and {@code proto} of the original send
	 * @param language language of the absent user
	 * @return the fabricated request
	 */
	public static HttpServerRequest replyRequest(final JsonObject work, final String language) {
		final JsonObject headers = new JsonObject()
				.put("X-Forwarded-For", "127.0.0.1")
				.put("Host", work.getString("host", ""))
				.put("Accept-Language", language);
		// Absent from payloads dispatched before this field existed : the scheme then falls back as before.
		if (work.getString("proto") != null) {
			headers.put("X-Forwarded-Proto", work.getString("proto"));
		}
		return new JsonHttpServerRequest(new JsonObject().put("method", "POST").put("headers", headers));
	}
}
