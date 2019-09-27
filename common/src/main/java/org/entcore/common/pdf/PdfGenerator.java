/*
 * Copyright © "Open Digital Education", 2019
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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.pdf;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import org.entcore.common.user.UserInfos;


public interface PdfGenerator {
	enum SourceKind{
		document, presentation, spreadsheet
	}
	void generatePdfFromTemplate(String name, String template, Handler<AsyncResult<Pdf>> handler);

	void generatePdfFromUrl(String name, String url, Handler<AsyncResult<Pdf>> handler);

	void generatePdfFromTemplate(UserInfos user, String name, String template, Handler<AsyncResult<Pdf>> handler);

	void convertToPdfFromBuffer(SourceKind kind, Buffer file, Handler<AsyncResult<Pdf>> handler);

	void generatePdfFromUrl(UserInfos user, String name, String url, Handler<AsyncResult<Pdf>> handler);

	Future<Pdf> generatePdfFromTemplate(String name, String template);

	Future<Pdf> generatePdfFromTemplate(String name, String template, String token);

	Future<Pdf> generatePdfFromUrl(String name, String url);

	Future<Pdf> generatePdfFromUrl(String name, String url, String token);

	String createToken(UserInfos user) throws Exception;

}
