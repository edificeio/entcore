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
 * "Name contains" search, case and accent insensitive, used when Mongo full text search is disabled.
 * Examples: 
 * - {@code "eleve"} matches "Liste des ÉLÈVES.xlsx"
 * - {@code "d'ecran"} matches "Capture d’écran.png",
 * - {@code "a.b"} matches "a.b.txt" but not "axb.txt".
 */
public final class DocumentNameSearch {

	/** Searched text is truncated beyond this length. */
	public static final int MAX_LENGTH = 100;
	/** 
	 * Combining accents, for names stored decomposed
	 * Example: "e" followed by U+0301 for "é". 
	 */
	private static final String COMBINING_MARKS = "[\u0300-\u036f]*";
	/** 
	 * Letter to its accented variants, upper cases included 
	 * Example: ({@code 'e'} → {@code [eèéêëÈÉÊË]}) + apostrophes. 
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
	 * Builds the regex: accents and case normalized, then special chars escaped
	 * Example: {@code "copie (1)"} → {@code c[oò…]pie\ \(1\)}.
	 *
	 * @param search the text typed by the user
	 * @return the regex, or null when the search is blank
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
	 * Builds the mongo filter for the {@code name} field: 
	 * {"$regex": <toRegex(search)>, "$options": "i"}
	 *
	 * @param search the text typed by the user
	 * @return the filter, or null when the search is blank
	 */
	public static Document toNameFilter(final String search) {
		final String regex = toRegex(search);
		if (regex == null) {
			return null;
		}
		return new Document("$regex", regex).append("$options", "i");
	}
}
