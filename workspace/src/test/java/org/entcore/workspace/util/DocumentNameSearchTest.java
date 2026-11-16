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

import io.vertx.core.json.JsonObject;
import org.bson.Document;
import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.impl.QueryHelper.DocumentQueryBuilder;
import org.junit.Test;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DocumentNameSearchTest {

	private static boolean matches(final String search, final String name) {
		// same semantics as the Mongo $options "i" on a UTF-8 string
		return Pattern.compile(DocumentNameSearch.toRegex(search), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
				.matcher(name).find();
	}

	@Test
	public void testNothingToSearch() {
		assertNull(DocumentNameSearch.toRegex(null));
		assertNull(DocumentNameSearch.toRegex(""));
		assertNull(DocumentNameSearch.toRegex("   "));
		assertNull(DocumentNameSearch.toNameFilter("  "));
	}

	@Test
	public void testContainsIgnoringCaseAndAccents() {
		assertTrue(matches("gaspillage", "Le gaspillage alimentaire.pdf"));
		assertTrue(matches("GASPILLAGE", "Le gaspillage alimentaire.pdf"));
		assertTrue(matches("gaspi", "Le gaspillage alimentaire.pdf"));
		assertTrue(matches("eleve", "Liste des élèves.xlsx"));
		assertTrue(matches("élève", "Liste des eleves.xlsx"));
		assertTrue(matches("ELEVES", "Liste des ÉLÈVES.xlsx"));
		assertTrue(matches("francais", "Cours de Français"));
		assertTrue(matches("d'ecran", "Capture d’écran 2023-05-10.png"));
		assertFalse(matches("gaspillage", "Le compost.pdf"));
	}

	@Test
	public void testDecomposedName() {
		// macOS may store file names in decomposed form: "e" followed by a combining acute accent
		assertTrue(matches("ecran", "Capture d’écran.png"));
		assertTrue(matches("écran", "Capture d’écran.png"));
	}

	@Test
	public void testRegexMetacharactersAreLiteral() {
		assertTrue(matches("a.b", "fichier a.b.txt"));
		assertFalse(matches("a.b", "fichier axb.txt"));
		assertTrue(matches("(1)", "copie (1).png"));
		assertTrue(matches("[draft]*", "doc [draft]*.odt"));
		assertTrue(matches("\\", "dossier\\fichier"));
		assertTrue(matches("^$|+?{}", "x^$|+?{}y"));
		assertFalse(matches(".*", "nom quelconque"));
		assertTrue(matches("photo 😀", "ma photo 😀.png"));
	}

	@Test
	public void testSearchIsTruncated() {
		final String longSearch = new String(new char[500]).replace('\0', 'b');
		final String regex = DocumentNameSearch.toRegex(longSearch);
		assertEquals(DocumentNameSearch.MAX_LENGTH, regex.length());
	}

	@Test
	public void testGeneratedMongoQuery() {
		final ElementQuery query = new ElementQuery(false);
		query.getParams().put("name", DocumentNameSearch.toNameFilter("Élève"));
		final JsonObject mongoQuery = new JsonObject(
				DocumentQueryBuilder.fromElementQuery(query, Optional.empty()).build().toBsonDocument().toJson());
		final JsonObject nameFilter = findNameFilter(mongoQuery);
		assertEquals(DocumentNameSearch.toRegex("Élève"), nameFilter.getString("$regex"));
		assertEquals("i", nameFilter.getString("$options"));
		assertFalse(mongoQuery.encode().contains("$text"));
	}

	private static JsonObject findNameFilter(final JsonObject mongoQuery) {
		if (mongoQuery.getValue("name") instanceof JsonObject) {
			return mongoQuery.getJsonObject("name");
		}
		if (mongoQuery.getJsonArray("$and") == null) {
			return null;
		}
		for (final Object and : mongoQuery.getJsonArray("$and")) {
			final JsonObject found = findNameFilter((JsonObject) and);
			if (found != null) {
				return found;
			}
		}
		return null;
	}
}
