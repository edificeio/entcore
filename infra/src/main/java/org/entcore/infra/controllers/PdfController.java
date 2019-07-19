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

package org.entcore.infra.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.pdf.PdfFactory;
import org.entcore.common.pdf.PdfGenerator;
import org.entcore.common.user.UserUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.entcore.common.utils.StringUtils.isEmpty;

public class PdfController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(PdfController.class);
	private PdfGenerator pdfGenerator;

	@Get("/pdf")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void pdf(HttpServerRequest request) {
		final String name = request.params().get("name");
		final String url = request.params().get("url");
		if (isEmpty(name) || isEmpty(url) || !url.startsWith("/")) {
			badRequest(request, "invalid.params");
			return;
		}
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				String host;
				try {
					host = config.getBoolean("docker", false) ?
							InetAddress.getLocalHost().getHostAddress() + ":8090" : getHost(request);
				} catch (UnknownHostException e) {
					host = getHost(request);
				}
				pdfGenerator.generatePdfFromUrl(user, name, getScheme(request) + "://" + host + url, ar -> {
					if (ar.succeeded()) {
						request.response().putHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
						request.response().end(ar.result().getContent());
					} else {
						log.error("Error generating pdf : " + name + " - " + url);
						renderJson(request, new JsonObject().put("error", ar.cause().getMessage()), 400);
					}
				});
			} else {
				unauthorized(request, "invalid.user");
			}
		});
	}

	public void setPdfGenerator(PdfFactory pdfFactory) throws Exception {
		this.pdfGenerator = pdfFactory.getPdfGenerator();
	}

}
