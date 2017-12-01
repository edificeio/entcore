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

package org.entcore.auth.adapter;

import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;

import static fr.wseduc.webutils.Utils.isEmpty;

public class ResponseAdapterFactory {

	public static UserInfoAdapter getUserInfoAdapter(HttpServerRequest request) {
		String version = RequestUtils.acceptVersion(request);
		if (isEmpty(version)) {
			version = Utils.getOrElse(request.params().get("version"), "");
		}
		switch (version) {
			case "1.0":
			case "v1.0":
				return new UserInfoAdapterV1_0Json();
			case "1.1":
			case "v1.1":
				return new UserInfoAdapterV1_1Json();
			case "2.0":
			case "v2.0":
				return new UserInfoAdapterV2_0Json();
			case "2.1":
			case "v2.1":
				return new UserInfoAdapterV2_1Json();
			default:
				return new UserInfoAdapterV1_0Json();
		}
	}

}
