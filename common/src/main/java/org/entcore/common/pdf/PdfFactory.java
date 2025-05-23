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
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import fr.wseduc.webutils.collections.SharedDataHelper;

import java.net.URISyntaxException;

import org.entcore.common.pdf.metrics.PdfMetricsRecorderFactory;

public class PdfFactory {

	private static final Logger log = LoggerFactory.getLogger(PdfFactory.class);
	private final PdfGenerator pdfGenerator;

	public PdfFactory(Vertx vertx) {
		this(vertx, null);
	}

	public PdfFactory(Vertx vertx, JsonObject config) {
		this.pdfGenerator = new NodePdfClient();
		SharedDataHelper.getInstance().<String, String>get("server", "node-pdf-generator").onSuccess(nodePdfConfig -> {
			JsonObject node = null;
			if (nodePdfConfig != null) {
				node = new JsonObject(nodePdfConfig);
			}

			if (config != null && config.getJsonObject("node-pdf-generator") != null) {
				node = config.getJsonObject("node-pdf-generator");
			}
			try {
				((NodePdfClient) pdfGenerator).init(vertx, node);
			} catch (URISyntaxException e) {
				log.error("Error when init node pdf generator client", e);
			}
			PdfMetricsRecorderFactory.init(vertx, config);
		}).onFailure(ex -> log.error("Error getting node-pdf-generator config in server map", ex));
	}

	public PdfGenerator getPdfGenerator() {
		return pdfGenerator;
	}

}
