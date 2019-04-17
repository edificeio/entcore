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
		put("AIX","AIX-MARSEILLE");
		put("AMI","AMIENS");
		put("BES","BESANCON");
		put("BRD","BORDEAUX");
		put("CAE","CAEN");
		put("CMF","CLERMONT-FERRAND");
		put("CRS","CORSE");
		put("CRE","CRETEIL");
		put("DIJ","DIJON");
		put("GRE","GRENOBLE");
		put("GUA","GUADELOUPE");
		put("GUY","GUYANE");
		put("LIL","LILLE");
		put("LIM","LIMOGES");
		put("LYO","LYON");
		put("MAR","MARTINIQUE");
		put("MAY","MAYOTTE");
		put("MON","MONTPELLIER");
		put("NCY","NANCY-METZ");
		put("NAN","NANTES");
		put("NIC","NICE");
		put("NCL","NOUVELLE_CALEDONIE");
		put("ORL","ORLEANS-TOURS");
		put("PAR","PARIS");
		put("POI","POITIERS");
		put("POL","POLYNESIE_FRANCAISE");
		put("REI","REIMS");
		put("REN","RENNES");
		put("REU","LA_REUNION");
		put("ROU","ROUEN");
		put("SPM","ST_PIERRE_ET_MIQUELON");
		put("STR","STRASBOURG");
		put("TLS","TOULOUSE");
		put("VRS","VERSAILLES");
		put("WAF","WALLIS_ET_FUTUNA");
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
