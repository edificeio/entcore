package org.entcore.feeder.csv;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.utils.Hash;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static org.entcore.feeder.utils.Validator.sanitize;

/**
 * Deterministic externalId of a CSV student or staff member without one (referential POC, step 3b):
 * sha1("structure|profile|lastName|firstName|birthDate|rank"), names sanitized like the search fields, birth date
 * "yyyy-MM-dd". The rank (1, 2...) separates strict homonyms of a file, in order of appearance: one instance per
 * file. Same specification in referential, checked by the shared vectors csv-external-id-vectors.json.
 */
public final class CsvExternalIds {

	private final Map<String, Integer> ranks = new HashMap<>();

	/** Sets the externalId of {@code user} if absent, counting it among the homonyms of the file. */
	public void generateIfAbsent(JsonObject user, String structureExternalId, String profile) {
		final String externalId = user.getString("externalId");
		if (externalId != null && !externalId.trim().isEmpty()) {
			return;
		}
		final String lastName = sanitize(user.getString("lastName", ""));
		final String firstName = sanitize(user.getString("firstName", ""));
		final String birthDate = user.getString("birthDate", "");
		final int rank = ranks.merge(lastName + '|' + firstName + '|' + birthDate, 1, Integer::sum);
		user.put("externalId", hash(structureExternalId, profile, lastName, firstName, birthDate, rank));
	}

	/** Hash of already sanitized names. */
	static String hash(String structureExternalId, String profile, String lastName, String firstName,
			String birthDate, int rank) {
		try {
			return Hash.sha1((structureExternalId + '|' + profile + '|' + lastName + '|' + firstName + '|'
					+ birthDate + '|' + rank).getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
