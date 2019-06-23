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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(VertxUnitRunner.class)
public class PdfGeneratorTest {

	private static final String IP_ENT = "172.17.0.6";
	private static final String IP_PDF_GEN = "localhost";

	private static final String template =
			"<html><head>test</head><body><h1>mon titre</h1>Image 1 : <img src=\"http://" + IP_ENT + ":8090/workspace/document/f3b12102-11d3-46a4-9fba-f397a5fcf547?thumbnail=120x120&v=48\" /><br />" +
			"Image 2 : <img src=\"http://" + IP_ENT + ":8090/workspace/document/db253ce0-8fe2-456e-9792-f761b82e80b5?thumbnail=120x120&v=70\" /></body></html>";
	private static final String t2 =
			"<html><head><title>Vous Etes Perdu ?</title></head><body><h1>Perdu sur l'Internet ?</h1><h2>Pas de panique, on va vous aider</h2><strong><pre>    * <----- vous &ecirc;tes ici</pre></strong></body></html>\n";
	private Vertx vertx;
	private PdfFactory pdfFactory;
	private UserInfos user;

	@Before
	public void setUp(TestContext context) {
		Vertx vertx = Vertx.vertx();
		final LocalMap<Object, Object> serverMap = vertx.sharedData().getLocalMap("server");
		serverMap.put("signKey", "test-secret");
		pdfFactory = new PdfFactory(vertx, new JsonObject().put("node-pdf-generator", new JsonObject()
			.put("url", "http://" + IP_PDF_GEN + ":3000")
			.put("pdf-connector-id", "exportpdf")
			.put("auth", "bm9wOm5vcA==")
		));
		user = new UserInfos();
		user.setUserId("d04e780b-9588-4884-a720-c02646a8b819");
		this.vertx = vertx;
	}

	@Test
	public void testMultiple(TestContext context) throws Exception {
		Async async = context.async();
		PdfGenerator pdfGenerator = pdfFactory.getPdfGenerator();
		final String token = pdfGenerator.createToken(user);
		Buffer bufTemplate = vertx.fileSystem().readFileBlocking("/tmp/example-pdf-xhtml.xhtml");
		List<Future> futures = new ArrayList<>();
		futures.add(pdfGenerator.generatePdfFromUrl("adminv1",
				"http://" + IP_ENT + ":8090/directory/admin-console", token));
		futures.add(pdfGenerator.generatePdfFromUrl("adminv2",
				"http://" + IP_ENT + ":8090/admin", token));
		futures.add(pdfGenerator.generatePdfFromTemplate("template", template, token));
		futures.add(pdfGenerator.generatePdfFromTemplate("template2", t2, token));
		futures.add(pdfGenerator.generatePdfFromTemplate("template3", bufTemplate.toString()));
		futures.add(pdfGenerator.generatePdfFromUrl("perdu","https://perdu.com"));
		//futures.add(pdfGenerator.generatePdfFromUrl("ONE","https://one1d.fr"));
		futures.add(pdfGenerator.generatePdfFromUrl("ONE","https://one.opendigitaleducation.com"));
		AtomicInteger count = new AtomicInteger(futures.size());
		CompositeFuture.all(futures).setHandler(ar -> {
			if (ar.succeeded()) {
				ar.result().list().forEach(pdf -> vertx.fileSystem().writeFile(
						"/tmp/test-pdf/" + ((Pdf) pdf).getName(), ((Pdf) pdf).getContent(), r -> {
					if (count.decrementAndGet() == 0) {
						async.complete();
					}
				}));
			} else {
				context.fail();
			}
		});
	}

}
