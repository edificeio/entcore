/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.directory.profils;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.collections.Joiner;

import org.entcore.common.neo4j.Neo;

public class DefaultProfils implements Profils {

	private final Neo neo;

	public DefaultProfils(Neo neo) {
		this.neo = neo;
	}

	@Override
	public void listGroupsProfils(Object [] typeFilter, String schoolId,
			final Handler<JsonObject> handler) {
		Map<String, Object> params = new HashMap<>();
		String typesProfileGroup;
		if (typeFilter != null && typeFilter.length > 0) {
			typesProfileGroup =  "n:" + Joiner.on(" OR n:").join(typeFilter);
		} else {
			typesProfileGroup = "n:ProfileGroup";
		}
		String query;
		if (schoolId != null && !schoolId.trim().isEmpty()) {
			query = "MATCH n-[:DEPENDS*1..2]->(m:School) " +
					"WHERE (" + typesProfileGroup + ") AND m.id = {schoolId} ";
			params.put("schoolId", schoolId);
		} else {
			query = "MATCH n-[:DEPENDS*1..2]->(m:School) " +
					"WHERE (" + typesProfileGroup + ") AND m.id = {schoolId} ";
		}
		query += "RETURN distinct n.name as name, n.id as id, " +
				"HEAD(filter(x IN labels(m) WHERE x <> 'Visible' AND x <> 'ProfileGroup' " +
				"AND x <> 'ClassProfileGroup' AND x <> 'SchoolProfileGroup')) as type";
		neo.send(query, params, new Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					handler.handle(res.body());
				}
			}
		);
	}

}
