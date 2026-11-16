/*
 * Copyright © "Open Digital Education", 2026
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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.workspace.util;

import org.bson.Document;
import org.entcore.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the name filter used by the documents search when Mongo full text search is disabled.
 * The filter is a "contains" regex, insensitive to case and accents, evaluated by Mongo after
 * the rights indexes (owner, inheritedShares): its cost is bounded by the documents the user can see.
 */
public final class DocumentNameSearch {

	/** Searched text is truncated beyond this length to keep the regex cheap. */
	public static final int MAX_LENGTH = 100;
	/** Optional combining diacritical marks, for names stored in decomposed form (NFD). */
	private static final String COMBINING_MARKS = "[̀-ͯ]*";
	/**
	 * Character classes matching a letter with or without its accents, and both apostrophes.
	 * Accented upper cases are explicit: the case insensitive option is only guaranteed for ASCII.
	 */
	private static final Map<Character, String> EQUIVALENTS = new HashMap<>();

	static {
		EQUIVALENTS.put('a', "[aàáâãäåÀÁÂÃÄÅ]" + COMBINING_MARKS);
		EQUIVALENTS.put('c', "[cçÇ]" + COMBINING_MARKS);
		EQUIVALENTS.put('e', "[eèéêëÈÉÊË]" + COMBINING_MARKS);
		EQUIVALENTS.put('i', "[iìíîïÌÍÎÏ]" + COMBINING_MARKS);
		EQUIVALENTS.put('n', "[nñÑ]" + COMBINING_MARKS);
		EQUIVALENTS.put('o', "[oòóôõöÒÓÔÕÖ]" + COMBINING_MARKS);
		EQUIVALENTS.put('u', "[uùúûüÙÚÛÜ]" + COMBINING_MARKS);
		EQUIVALENTS.put('y', "[yýÿÝŸ]" + COMBINING_MARKS);
		EQUIVALENTS.put('\'', "['’]");
		EQUIVALENTS.put('’', "['’]");
	}

	private DocumentNameSearch() {
	}

	/**
	 * Builds a Mongo regex matching names that contain the searched text, ignoring case and accents.
	 *
	 * @param search the text typed by the user
	 * @return the regex pattern, or null when there is nothing to search
	 */
	public static String toRegex(final String search) {
		if (StringUtils.isEmpty(search)) {
			return null;
		}
		final String normalized = StringUtils.stripAccentsToLowerCase(search.trim());
		if (normalized.isEmpty()) {
			return null;
		}
		final StringBuilder regex = new StringBuilder();
		normalized.codePoints().limit(MAX_LENGTH).forEach(codePoint -> {
			final String equivalent = Character.isBmpCodePoint(codePoint) ? EQUIVALENTS.get((char) codePoint) : null;
			if (equivalent != null) {
				regex.append(equivalent);
			} else {
				// a backslash before a non alphanumeric char always means the literal char
				if (!Character.isLetterOrDigit(codePoint)) {
					regex.append('\\');
				}
				regex.appendCodePoint(codePoint);
			}
		});
		return regex.toString();
	}

	/**
	 * Builds the Mongo filter value to apply on the document name.
	 *
	 * @param search the text typed by the user
	 * @return the {@code $regex} filter, or null when there is nothing to search
	 */
	public static Document toNameFilter(final String search) {
		final String regex = toRegex(search);
		if (regex == null) {
			return null;
		}
		return new Document("$regex", regex).append("$options", "i");
	}
}
