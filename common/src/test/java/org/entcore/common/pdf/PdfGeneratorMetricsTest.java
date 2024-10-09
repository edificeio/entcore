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

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.pdf.metrics.PdfMetricsContext;
import org.entcore.common.pdf.metrics.PdfMetricsRecorder;
import org.entcore.common.pdf.metrics.PdfMetricsRecorderFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(VertxUnitRunner.class)
public class PdfGeneratorMetricsTest {
	private static final FakePdfMetricsRecorder recorder = new FakePdfMetricsRecorder();
	private static PdfFactory pdfFactory;
	private static Vertx vertx;
	private static HttpServer server;
	private static FakeRequestHandler requestHandler;

	@BeforeClass
	public static void beforeAll(TestContext context) {
		vertx = Vertx.vertx();
		PdfMetricsRecorderFactory.setPdfMetricsRecorder(recorder);
		requestHandler = new FakeRequestHandler();
		server = vertx.createHttpServer().requestHandler(requestHandler::handleRequest).listen();
		final String url = String.format("http://localhost:%s", server.actualPort());
		final JsonObject pdfConfig = new JsonObject().put("url", url).put("pdf-connector-id", "exportpdf").put("auth", "aa==");
		final JsonObject config = new JsonObject().put("node-pdf-generator", pdfConfig);
		pdfFactory = new PdfFactory(vertx, config);
	}

	@AfterClass
	public static void afterAll(TestContext context) {
		server.close();
	}

	@Before
	public void beforeEach(TestContext context) {
		recorder.clear();
	}

	/**
	 * <u>Goal : </u> Check wether metrics are recorded when a preview operation succeed
	 * @param context
	 * @throws Exception
	 */
	@Test
	public void shouldRecordMetricsWhenPreviewSucceed(TestContext context) throws Exception {
		requestHandler.setExpected(Event.Succeed);
		pdfFactory.getPdfGenerator().convertToPdfFromBuffer(PdfGenerator.SourceKind.document, Buffer.buffer(), context.asyncAssertSuccess(res -> {
			context.assertEquals(2, recorder.stack.size());
			// first entry should be pending conversion
			final FakePdfMetricsRecorderEntry firstEntry = recorder.stack.get(0);
			context.assertEquals(Event.Start, firstEntry.event);
			context.assertEquals(PdfGenerator.SourceKind.document, firstEntry.context.getSourceKind());
			context.assertEquals(0, firstEntry.context.getFileContent().get().length());
			context.assertEquals(PdfMetricsRecorder.TaskKind.Preview, firstEntry.context.getTaskKind());
			// second entry should be succeed event
			final FakePdfMetricsRecorderEntry secondEntry = recorder.stack.get(1);
			context.assertEquals(Event.Succeed, secondEntry.event);
			context.assertEquals(PdfGenerator.SourceKind.document, secondEntry.context.getSourceKind());
			context.assertEquals(0, secondEntry.context.getFileContent().get().length());
			context.assertEquals(PdfMetricsRecorder.TaskKind.Preview, secondEntry.context.getTaskKind());
			context.assertEquals(200, secondEntry.statusCode);
			context.assertNull(secondEntry.errorKind);
			context.assertNull(secondEntry.phase);
			context.assertTrue(secondEntry.context.getWatch().elapsedTimeMillis() > 0);
		}));
	}


	/**
	 * <u>Goal : </u> Check wether metrics are recorded when a preview operation failed
	 * @param context
	 * @throws Exception
	 */
	@Test
	public void shouldRecordMetricsWhenPreviewFailed(TestContext context) throws Exception {
		requestHandler.setExpected(Event.Failed);
		pdfFactory.getPdfGenerator().convertToPdfFromBuffer(PdfGenerator.SourceKind.document, Buffer.buffer(), context.asyncAssertFailure(res -> {
			context.assertEquals(2, recorder.stack.size());
			// first entry should be pending conversion
			final FakePdfMetricsRecorderEntry firstEntry = recorder.stack.get(0);
			context.assertEquals(Event.Start, firstEntry.event);
			context.assertEquals(PdfGenerator.SourceKind.document, firstEntry.context.getSourceKind());
			context.assertEquals(0, firstEntry.context.getFileContent().get().length());
			context.assertEquals(PdfMetricsRecorder.TaskKind.Preview, firstEntry.context.getTaskKind());
			// second entry should be failed event
			final FakePdfMetricsRecorderEntry secondEntry = recorder.stack.get(1);
			context.assertEquals(Event.Failed, secondEntry.event);
			context.assertEquals(PdfGenerator.SourceKind.document, secondEntry.context.getSourceKind());
			context.assertEquals(0, secondEntry.context.getFileContent().get().length());
			context.assertEquals(PdfMetricsRecorder.TaskKind.Preview, secondEntry.context.getTaskKind());
			context.assertEquals(500, secondEntry.statusCode);
			context.assertNull(secondEntry.errorKind);
			context.assertNull(secondEntry.phase);
			context.assertTrue(secondEntry.context.getWatch().elapsedTimeMillis() > 0);
		}));
	}

	/**
	 * <u>Goal : </u> Check wether metrics are recorded when a preview operation never finished because of closed connexion
	 * @param context
	 * @throws Exception
	 */
	@Test
	public void shouldRecordMetricsWhenPreviewUnfinished(TestContext context) throws Exception {
		requestHandler.setExpected(Event.Unfinished);
		pdfFactory.getPdfGenerator().convertToPdfFromBuffer(PdfGenerator.SourceKind.document, Buffer.buffer(), context.asyncAssertFailure(res -> {
			context.assertEquals(2, recorder.stack.size());
			// first entry should be pending conversion
			final FakePdfMetricsRecorderEntry firstEntry = recorder.stack.get(0);
			context.assertEquals(Event.Start, firstEntry.event);
			context.assertEquals(PdfGenerator.SourceKind.document, firstEntry.context.getSourceKind());
			context.assertEquals(0, firstEntry.context.getFileContent().get().length());
			context.assertEquals(PdfMetricsRecorder.TaskKind.Preview, firstEntry.context.getTaskKind());
			// second entry should be failed event
			final FakePdfMetricsRecorderEntry secondEntry = recorder.stack.get(1);
			context.assertEquals(Event.Unfinished, secondEntry.event);
			context.assertEquals(PdfGenerator.SourceKind.document, secondEntry.context.getSourceKind());
			context.assertEquals(0, secondEntry.context.getFileContent().get().length());
			context.assertEquals(PdfMetricsRecorder.TaskKind.Preview, secondEntry.context.getTaskKind());
			context.assertNull(secondEntry.statusCode);
			context.assertEquals("Connection was closed", secondEntry.errorKind);
			context.assertEquals(PdfMetricsRecorder.Phase.Request, secondEntry.phase);
			context.assertTrue(secondEntry.context.getWatch().elapsedTimeMillis() > 0);
		}));
	}

	/**
	 * <u>Goal : </u> Check wether metrics are recorded when a print operation succeed
	 * @param context
	 * @throws Exception
	 */
	@Test
	public void shouldRecordMetricsWhenPrintSucceed(TestContext context) throws Exception {
		requestHandler.setExpected(Event.Succeed);
		pdfFactory.getPdfGenerator().generatePdfFromTemplate("NAME", "TEMPLATE", context.asyncAssertSuccess(res -> {
			context.assertEquals(2, recorder.stack.size());
			// first entry should be pending conversion
			final FakePdfMetricsRecorderEntry firstEntry = recorder.stack.get(0);
			context.assertEquals(Event.Start, firstEntry.event);
			context.assertEquals("html", firstEntry.context.getSourceKind().name());
			context.assertTrue(firstEntry.context.getFileContent().get().length() > 0);
			context.assertEquals(PdfMetricsRecorder.TaskKind.Print, firstEntry.context.getTaskKind());
			// second entry should be succeed event
			final FakePdfMetricsRecorderEntry secondEntry = recorder.stack.get(1);
			context.assertEquals(Event.Succeed, secondEntry.event);
			context.assertEquals("html", secondEntry.context.getSourceKind().name());
			context.assertTrue(secondEntry.context.getFileContent().get().length() > 0);
			context.assertEquals(PdfMetricsRecorder.TaskKind.Print, secondEntry.context.getTaskKind());
			context.assertEquals(200, secondEntry.statusCode);
			context.assertNull(secondEntry.errorKind);
			context.assertNull(secondEntry.phase);
			context.assertTrue(secondEntry.context.getWatch().elapsedTimeMillis() > 0);
		}));
	}


	/**
	 * <u>Goal : </u> Check wether metrics are recorded when a print operation failed
	 * @param context
	 * @throws Exception
	 */
	@Test
	public void shouldRecordMetricsWhenPrintFailed(TestContext context) throws Exception {
		requestHandler.setExpected(Event.Failed);
		pdfFactory.getPdfGenerator().generatePdfFromTemplate("NAME", "TEMPLATE", context.asyncAssertFailure(res -> {
			context.assertEquals(2, recorder.stack.size());
			// first entry should be pending conversion
			final FakePdfMetricsRecorderEntry firstEntry = recorder.stack.get(0);
			context.assertEquals(Event.Start, firstEntry.event);
			context.assertEquals("html", firstEntry.context.getSourceKind().name());
			context.assertTrue(firstEntry.context.getFileContent().get().length() > 0);
			context.assertEquals(PdfMetricsRecorder.TaskKind.Print, firstEntry.context.getTaskKind());
			// second entry should be failed event
			final FakePdfMetricsRecorderEntry secondEntry = recorder.stack.get(1);
			context.assertEquals(Event.Failed, secondEntry.event);
			context.assertEquals("html", secondEntry.context.getSourceKind().name());
			context.assertTrue(secondEntry.context.getFileContent().get().length() > 0);
			context.assertEquals(PdfMetricsRecorder.TaskKind.Print, secondEntry.context.getTaskKind());
			context.assertEquals(500, secondEntry.statusCode);
			context.assertNull(secondEntry.errorKind);
			context.assertNull(secondEntry.phase);
			context.assertTrue(secondEntry.context.getWatch().elapsedTimeMillis() > 0);
		}));
	}


	/**
	 * <u>Goal : </u> Check wether metrics are recorded when a print operation never finished because the connexion has been closed
	 * @param context
	 * @throws Exception
	 */
	@Test
	public void shouldRecordMetricsWhenPrintUnfinished(TestContext context) throws Exception {
		requestHandler.setExpected(Event.Unfinished);
		pdfFactory.getPdfGenerator().generatePdfFromTemplate("NAME", "TEMPLATE", context.asyncAssertFailure(res -> {
			context.assertEquals(2, recorder.stack.size());
			// first entry should be pending conversion
			final FakePdfMetricsRecorderEntry firstEntry = recorder.stack.get(0);
			context.assertEquals(Event.Start, firstEntry.event);
			context.assertEquals("html", firstEntry.context.getSourceKind().name());
			context.assertTrue(firstEntry.context.getFileContent().get().length() > 0);
			context.assertEquals(PdfMetricsRecorder.TaskKind.Print, firstEntry.context.getTaskKind());
			// second entry should be failed event
			final FakePdfMetricsRecorderEntry secondEntry = recorder.stack.get(1);
			context.assertEquals(Event.Unfinished, secondEntry.event);
			context.assertEquals("html", secondEntry.context.getSourceKind().name());
			context.assertTrue(secondEntry.context.getFileContent().get().length() > 0);
			context.assertEquals(PdfMetricsRecorder.TaskKind.Print, secondEntry.context.getTaskKind());
			context.assertNull(secondEntry.statusCode);
			context.assertEquals("Connection was closed", secondEntry.errorKind);
			context.assertEquals(PdfMetricsRecorder.Phase.Request, secondEntry.phase);
			context.assertTrue(secondEntry.context.getWatch().elapsedTimeMillis() > 0);
		}));
	}

	static class FakePdfMetricsRecorder implements PdfMetricsRecorder {
		final List<FakePdfMetricsRecorderEntry> stack = new ArrayList<>();

		void clear(){
			this.stack.clear();
		}

		@Override
		public void onPdfGenerationStart(PdfMetricsContext context) {
			this.stack.add(new FakePdfMetricsRecorderEntry(Event.Start, context, null, null, null));
		}

		@Override
		public void onPdfGenerationSucceed(PdfMetricsContext context) {
			this.stack.add(new FakePdfMetricsRecorderEntry(Event.Succeed, context, 200, null, null));
		}

		@Override
		public void onPdfGenerationFailed(PdfMetricsContext context, int statusCode) {
			this.stack.add(new FakePdfMetricsRecorderEntry(Event.Failed, context, statusCode, null, null));
		}

		@Override
		public void onPdfGenerationUnfinished(PdfMetricsContext context, Phase phase, String errorKind) {
			this.stack.add(new FakePdfMetricsRecorderEntry(Event.Unfinished, context, null, phase, errorKind));
		}
	}
	static class FakePdfMetricsRecorderEntry   {
		final Event event;
		final PdfMetricsContext context;
		final Integer statusCode;
		final PdfMetricsRecorder.Phase phase;
		final String errorKind;

		public FakePdfMetricsRecorderEntry(Event event, PdfMetricsContext context, Integer statusCode, PdfMetricsRecorder.Phase phase, String errorKind) {
			this.event = event;
			this.context = context;
			this.statusCode = statusCode;
			this.phase = phase;
			this.errorKind = errorKind;
		}

		@Override
		public String toString() {
			return "FakePdfMetricsRecorderEntry{" +
					"event=" + event +
					", context=" + context +
					", statusCode=" + statusCode +
					", phase=" + phase +
					", errorKind='" + errorKind + '\'' +
					'}';
		}
	}
	enum Event{
		Start, Succeed, Failed, Unfinished
	}
	static class FakeRequestHandler {
		private Event expected = Event.Succeed;

		public void setExpected(Event expected) {
			this.expected = expected;
		}

		void handleRequest(HttpServerRequest request) {
			switch(expected)
			{
				case Failed: {
					request.response().setStatusCode(500).end();
					break;
				}
				case Succeed: {
					request.response().setStatusCode(200).end();
					break;
				}
				case Start:{
					//do nothing
					break;
				}
				case Unfinished:{
					request.connection().close();
					break;
				}
			}
		}
	}
}
