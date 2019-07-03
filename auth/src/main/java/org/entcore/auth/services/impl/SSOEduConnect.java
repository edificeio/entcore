/*
 * Copyright © "Open Digital Education", 2015
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

 */

package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.opensaml.saml2.core.Assertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class SSOEduConnect extends AbstractSSOProvider {

	private final boolean noPrefix = Vertx.currentContext().config().getBoolean("not-prefix-educonnect", false);

	private static final Map<String, String> academiesMapping = Collections.unmodifiableMap(new HashMap<String, String>() {{
		put("00","ETRANGER");
		put("01","PARIS");
		put("02","AIX-MARSEILLE");
		put("03","BESANCON");
		put("04","BORDEAUX");
		put("05","CAEN");
		put("06","CLERMONT-FERRAND");
		put("07","DIJON");
		put("08","GRENOBLE");
		put("09","LILLE");
		put("10","LYON");
		put("11","MONTPELLIER");
		put("12","NANCY-METZ");
		put("13","POITIERS");
		put("14","RENNES");
		put("15","STRASBOURG");
		put("16","TOULOUSE");
		put("17","NANTES");
		put("18","ORLEANS-TOURS");
		put("19","REIMS");
		put("20","AMIENS");
		put("21","ROUEN");
		put("22","LIMOGES");
		put("23","NICE");
		put("24","CRETEIL");
		put("25","VERSAILLES");
		put("26","ANTILLES-GUYANE");
		put("27","CORSE");
		put("28","LA_REUNION");
		put("31","MARTINIQUE");
		put("32","GUADELOUPE");
		put("33","GUYANE");
		put("40","NOUVELLE_CALEDONIE");
		put("41","POLYNESIE_FRANCAISE");
		put("42","WALLIS_ET_FUTUNA");
		put("43","MAYOTTE");
		put("44","ST_PIERRE_ET_MIQUELON");
		put("54","SIEC_ILE_DE_FRANCE");
		put("61","FRANCE_METRO");
		put("62","DOM");
		put("63","COM");
		put("66","FRANCE_METRO_+_DOM");
		put("67","FRANCE_METRO_+_DOM_+_COM");
		put("70","NORMANDIE");
		put("91","UNION_EUROPEENNE");
		put("98","AGRICOLE");
		put("99","NON_DEFINI_OU_SANS_OBJET");
	}});

	@Override
	public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
		if (!validConditions(assertion, handler)) return;

		List<String> vectors = getAttributes(assertion, "FrEduCtRefId")
				.stream().filter(v -> v.startsWith("{AAF}")).collect(Collectors.toList());
		if (vectors.isEmpty()) {
			handler.handle(new Either.Left<>("invalid.vector"));
			return;
		}

		if (vectors.size() > 1) {
			JsonArray joinKeys = new fr.wseduc.webutils.collections.JsonArray();
			for (String vector : vectors) {
				final String joinKey = getJoinKey(vector);
				if (isNotEmpty(joinKey)) {
					joinKeys.add(joinKey);
				}
			}
			if (joinKeys.isEmpty()) {
				handler.handle(new Either.Left<>("invalid.joinKey"));
				return;
			}
			String query =
					"MATCH (u:User) " +
					"WHERE u.externalId IN {joinKeys} AND NOT(HAS(u.mergedWith)) ";
			JsonObject params = new JsonObject()
					.put("joinKeys", joinKeys);
			executeMultiVectorQuery(query, params, assertion, handler);
		} else {
			final String joinKey = getJoinKey(vectors.get(0));
			if (isNotEmpty(joinKey)) {
				executeQuery("MATCH (u:User {externalId:{joinKey}}) ", new JsonObject().put("joinKey", joinKey),
						assertion, handler);
			} else {
				handler.handle(new Either.Left<>("invalid.joinKey"));
			}
		}
	}

	private String getJoinKey(String vector) {
		final String [] values  = vector.substring(5).split("\\|");
		if (values.length != 3) return null;
		if (noPrefix) {
			return values[2];
		} else {
			final String academy = academiesMapping.get(values[0]);
			if (isNotEmpty(academy)) {
				return academy + "-" + values[2];
			}
		}
		return null;
	}

}
