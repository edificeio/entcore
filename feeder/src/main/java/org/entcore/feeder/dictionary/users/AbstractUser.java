/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
			JsonArray ms = new JsonArray();
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
