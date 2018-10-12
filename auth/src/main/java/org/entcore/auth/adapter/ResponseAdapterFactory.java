/* Copyright © "Open Digital Education", 2014
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
