/*
 * Copyright © "Open Digital Education", 2018
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

package org.entcore.common.validation;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;


public class QuotaFileSizeValidation extends FileValidator {

	protected QuotaFileSizeValidation() {}

	@Override
	protected void validate(JsonObject metadata, JsonObject context, Handler<AsyncResult<Void>> handler) {
		Long maxSize = context.getLong("maxSize");
		if (maxSize != null && maxSize < metadata.getLong("size", 0l)) {
			handler.handle(new DefaultAsyncResult<Void>(new ValidationException("file.too.large")));
		} else {
			handler.handle(new DefaultAsyncResult<Void>((Void) null));
		}
	}

}
