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

package org.entcore.auth.adapter;

import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Iterator;

public class UserInfoAdapterOpenIdConnectV1_0 implements UserInfoAdapter
{
	@Override
	public JsonObject getInfo(JsonObject info, String clientId)
	{
		JsonObject openIdInfos = new JsonObject();

		//cf. https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
		openIdInfos.put("sub",					info.getString("externalId"));
		openIdInfos.put("name",					info.getString("username"));
		openIdInfos.put("given_name",			info.getString("firstName"));
		openIdInfos.put("family_name",			info.getString("lastName"));
		openIdInfos.put("middle_name",			(String) null);
		openIdInfos.put("nickname",				(String) null);
		openIdInfos.put("preferred_username",	info.getString("username"));
		openIdInfos.put("profile",				(String) null);
		openIdInfos.put("picture",				(String) null);
		openIdInfos.put("website",				(String) null);
		openIdInfos.put("email",				info.getString("email"));
		openIdInfos.put("email_verified",		false);
		openIdInfos.put("gender",				(String) null);
		openIdInfos.put("birthdate",			info.getString("birthDate"));
		openIdInfos.put("zoneinfo",				(String) null);
		openIdInfos.put("locale",				info.getJsonObject("cache", new JsonObject()).getString("language"));
		openIdInfos.put("phone_number",			info.getString("phone"));
		openIdInfos.put("phone_number_verified",false);
		openIdInfos.put("address",				(JsonObject) null);
		openIdInfos.put("updated_at",			(Integer) null);

		// Remove null claims as per spec
		for (Iterator<Map.Entry<String, Object>> it = openIdInfos.iterator(); it.hasNext();)
		{
			Map.Entry<String, Object> entry = it.next();
			if (entry.getValue() == null)
				it.remove();
		}

		return openIdInfos;
	}
}
