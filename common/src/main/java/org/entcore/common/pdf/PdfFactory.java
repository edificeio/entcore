/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.common.pdf;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

public class PdfFactory {

	private final Vertx vertx;
	private JsonObject node;

	public PdfFactory(Vertx vertx) {
		this(vertx, null);
	}

	public PdfFactory(Vertx vertx, JsonObject config) {
		this.vertx = vertx;
		LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		String s = (String) server.get("node-pdf-generator");
		if (s != null) {
			this.node = new JsonObject(s);
		}

		if (config != null && config.getJsonObject("node-pdf-generator") != null) {
			this.node = config.getJsonObject("node-pdf-generator");
		}
	}

	public PdfGenerator getPdfGenerator() throws Exception {
		PdfGenerator pdfGenerator = null;
		if (node != null) {
			pdfGenerator = new NodePdfClient(vertx, node);
		} else {
			throw new PdfException("no.pdf.generator.found");
		}
		return pdfGenerator;
	}

}
