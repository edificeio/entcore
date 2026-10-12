/*
 * Copyright © "Edifice", 2026
 *
 * This program is published by "Edifice".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Edifice" with a reference to the website: https://edifice.io/.
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

package org.entcore.feeder.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static fr.wseduc.webutils.Utils.isNotEmpty;

/**
 * Groups from the AAF "OrgaPedago" attributes (SCOPE). For each structure, they replace
 * the legacy groups (SIECLE / STS) as soon as the user has at least one of them in it.
 */
public final class OrgaPedagoGroups {

	public static final String MEMBERSHIP_PERIOD_ATTRIBUTE = "groupsMembershipPeriod";
	public static final String GROUPS_ATTRIBUTE = "groups";
	// structure$group
	public static final int STUDENT_GROUP_SEGMENTS = 2;
	// structure$group$fieldOfStudy
	public static final int TEACHER_GROUP_SEGMENTS = 3;
	// structure$group$modality$start$end
	private static final int STUDENT_RAW_SEGMENTS = 5;
	// structure$group$?$?$fieldOfStudy$start$end
	private static final int TEACHER_RAW_SEGMENTS = 7;
	private static final int TEACHER_RAW_SUBJECT_INDEX = 4;

	private OrgaPedagoGroups() {
	}

	public static String toStudentGroup(String value) {
		final String[] items = split(value);
		if (items.length != STUDENT_RAW_SEGMENTS) {
			return value;
		}
		return items[0] + "$" + items[1];
	}

	public static String toTeacherGroup(String value, String prefix) {
		final String[] items = split(value);
		if (items.length != TEACHER_RAW_SEGMENTS) {
			return value;
		}
		final String subject = isNotEmpty(prefix) ? prefix + items[TEACHER_RAW_SUBJECT_INDEX] : items[TEACHER_RAW_SUBJECT_INDEX];
		return items[0] + "$" + items[1] + "$" + subject;
	}

	public static void applyTo(JsonObject user, int segments) {
		final JsonArray orgaPedagoGroups = user.getJsonArray(MEMBERSHIP_PERIOD_ATTRIBUTE);
		if (orgaPedagoGroups == null || orgaPedagoGroups.isEmpty()) {
			return;
		}
		user.put(GROUPS_ATTRIBUTE, prioritizeByStructure(user.getJsonArray(GROUPS_ATTRIBUTE), orgaPedagoGroups, segments));
	}

	public static JsonArray prioritizeByStructure(JsonArray legacyGroups, JsonArray orgaPedagoGroups, int segments) {
		final Set<String> coveredStructures = new HashSet<>();
		final Set<String> orgaPedago = new LinkedHashSet<>();
		if (orgaPedagoGroups != null) {
			for (Object o : orgaPedagoGroups) {
				if (!(o instanceof String)) continue;
				final String[] items = split((String) o);
				// malformed values are ignored and don't override legacy groups
				if (items.length == segments && !items[0].isEmpty() && !items[1].isEmpty()) {
					coveredStructures.add(items[0]);
					orgaPedago.add((String) o);
				}
			}
		}
		final Set<String> result = new LinkedHashSet<>();
		if (legacyGroups != null) {
			for (Object o : legacyGroups) {
				if (!(o instanceof String)) continue;
				if (!coveredStructures.contains(split((String) o)[0])) {
					result.add((String) o);
				}
			}
		}
		result.addAll(orgaPedago);
		final JsonArray groups = new JsonArray();
		result.forEach(groups::add);
		return groups;
	}

	private static String[] split(String value) {
		return value.split("\\$", -1);
	}

}
