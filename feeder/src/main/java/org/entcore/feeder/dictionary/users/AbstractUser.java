/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.feeder.dictionary.users;

import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.feeder.utils.JsonUtil;
import org.entcore.feeder.utils.Report;
import org.entcore.feeder.utils.TransactionHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.entcore.feeder.utils.AAFUtil.convertDate;

public abstract class AbstractUser {

	protected static final Logger log = LoggerFactory.getLogger(AbstractUser.class);
	protected TransactionHelper transactionHelper;
	private final Map<String, String> externalIdMapping;
	protected final Set<String> userImportedExternalId;
	protected final Report report;
	protected final String currentSource;
	private JsonObject mapping;

	protected AbstractUser(TransactionHelper transactionHelper, Report report, String currentSource) {
		this(transactionHelper, null, null, report, currentSource);
	}

	protected AbstractUser(TransactionHelper transactionHelper, Map<String, String> externalIdMapping, Set<String> userImportedExternalId, Report report, String currentSource) {
		this.transactionHelper = transactionHelper;
		this.externalIdMapping = externalIdMapping;
		this.userImportedExternalId = userImportedExternalId;
		this.report = report;
		this.currentSource = currentSource;
	}

	public void checkUpdateEmail(JsonObject object) {
		checkUpdateEmail(object, transactionHelper);
	}

	public static void checkUpdateEmail(JsonObject object, TransactionHelper transactionHelper) {
		if (object.containsKey("email")) {
			final String queryUpdateEmail =
					"MATCH (u:User {externalId: {externalId}}) " +
					"WHERE NOT(HAS(u.email)) OR (HAS(u.activationCode) AND u.email <> {email}) " +
					"SET u.email = {email}";
			transactionHelper.add(queryUpdateEmail, object);
		}
	}

	public JsonArray getMappingStructures(JsonArray structures) {
		if (externalIdMapping != null) {
			return getUserMappingStructures(structures, externalIdMapping);
		} else {
			return structures;
		}
	}

	public static JsonArray getUserMappingStructures(JsonArray structures, Map<String, String> externalIdMapping) {
		if (structures != null) {
			JsonArray ms = new fr.wseduc.webutils.collections.JsonArray();
			for (Object s: structures) {
				String externalId = externalIdMapping.get(s.toString());
				ms.add(((externalId != null && !externalId.trim().isEmpty()) ? externalId : s));
			}
			return ms;
		}
		return null;
	}

	public void setMapping(String mappingFile) {
		this.mapping = JsonUtil.loadFromResource(mappingFile);
	}

	public JsonObject applyMapping(JsonObject object) throws ValidationException {
		if (mapping != null) {
			final JsonObject res = new JsonObject();
			for (String attr : object.fieldNames()) {
				if (mapping.containsKey(attr)) {
					String s = object.getString(attr);
					JsonObject j = mapping.getJsonObject(attr);
					if (j == null) {
						throw new ValidationException("Unknown attribute " + attr);
					}
					// TODO implement types management
					//String type = j.getString("type");
					String attribute = j.getString("attribute");
					if ("birthDate".equals(attribute)) {
						s = convertDate(s);
					} else if ("deprecated".equals(attribute)) {
						continue;
					}
					res.put(attribute, s);
				}
			}
			return res;
		}
		return null;
	}

	public void setTransactionHelper(TransactionHelper transactionHelper) {
		this.transactionHelper = transactionHelper;
	}

}
