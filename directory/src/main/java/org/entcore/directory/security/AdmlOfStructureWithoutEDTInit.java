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

package org.entcore.directory.security;

import org.entcore.common.http.filter.AdmlOfStructure;
import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.http.HttpServerRequest;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AdmlOfStructureWithoutEDTInit extends AdmlOfStructure
{
	private final Neo4j neo4j = Neo4j.getInstance();
	private static final String query =
			"MATCH (s:Structure {id: {structureId}}) " +
			"RETURN HAS(s.timetable) AS alreadyInit";

	@Override
	public void authorizeAdml(HttpServerRequest resourceRequest, Binding binding,
			UserInfos user, UserInfos.Function adminLocal, Handler<Boolean> handler)
	{
		if(user.isADMC())
			handler.handle(true);
		else
		{
			String structureId = resourceRequest.params().get("structureId");
			super.authorizeAdml(resourceRequest, binding, user, adminLocal, new Handler<Boolean>()
			{
				@Override
				public void handle(Boolean isAdmlOfStruct)
				{
					if(isAdmlOfStruct.booleanValue() == false)
						handler.handle(false);
					else
					{
            resourceRequest.pause();
						neo4j.execute(query, new JsonObject().put("structureId", structureId), event ->
						{
              resourceRequest.resume();
              JsonObject res = event.body();
              if(res.getString("status").equals("ok"))
              {
                handler.handle(res.getJsonArray("result").getJsonObject(0).getBoolean("alreadyInit").equals(false));
              }
              else
								handler.handle(false);
						});
					}
				}
			});
		}
	}
}