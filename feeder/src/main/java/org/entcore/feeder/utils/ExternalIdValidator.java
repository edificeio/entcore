/* Copyright © "Open Digital Education", 2026
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
 */

package org.entcore.feeder.utils;

import java.util.regex.Pattern;

/**
 * Neutralise dans les externalId les caractères réservés du QueryParser Lucene.
 */
public final class ExternalIdValidator {

	/** Réservés où qu'ils soient dans la valeur. */
	private static final Pattern RESERVED_CHARS = Pattern.compile("[\\s/\"()\\[\\]{}:^~*?!\\\\]+");

	/** '-' et '+' ne sont réservés qu'en tête : ailleurs ce sont des caractères de terme ordinaires. */
	private static final Pattern LEADING_OPERATORS = Pattern.compile("^[-+]+");

	private ExternalIdValidator() {
	}

	/**
	 * Retourne l'externalId débarrassé de ses caractères réservés.
	 *
	 * @param externalId valeur d'origine, éventuellement nulle
	 * @return la valeur assainie, ou l'entrée telle quelle si elle est nulle ou vide
	 */
	public static String clean(String externalId) {
		if (externalId == null || externalId.isEmpty()) {
			return externalId;
		}
		final String withoutReservedChars = RESERVED_CHARS.matcher(externalId).replaceAll("");
		return LEADING_OPERATORS.matcher(withoutReservedChars).replaceAll("");
	}
}
