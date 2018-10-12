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

package org.entcore.feeder.aaf;

import org.apache.commons.lang3.text.translate.*;
import org.entcore.feeder.dictionary.structures.Importer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public abstract class BaseImportProcessing implements ImportProcessing {

	protected static final Logger log = LoggerFactory.getLogger(BaseImportProcessing.class);
	protected final String path;
	protected final Vertx vertx;
	protected final Importer importer = Importer.getInstance();
	private String academyPrefix;
	private static final String[][] OTHER_UNESCAPE = {{"&quot;", "\""}};
	public static final CharSequenceTranslator UNESCAPE_AAF =
			new AggregateTranslator(
					new LookupTranslator(OTHER_UNESCAPE),
					new LookupTranslator(EntityArrays.APOS_UNESCAPE()),
					new LookupTranslator(EntityArrays.ISO8859_1_UNESCAPE()),
					new LookupTranslator(EntityArrays.HTML40_EXTENDED_UNESCAPE()),
					new NumericEntityUnescaper()
			);

	protected BaseImportProcessing(String path, Vertx vertx) {
		this.path = path;
		this.vertx = vertx;
	}

	protected void parse(final Handler<Message<JsonObject>> handler, final ImportProcessing importProcessing) {
		initAcademyPrefix(path);
		final List<String> files = vertx.fileSystem()
				.readDirBlocking(path, getFileRegex());
		final Handler[] handlers = new Handler[files.size() + 1];
		handlers[handlers.length -1] = new Handler<Void>() {
			@Override
			public void handle(Void v) {
				next(handler, importProcessing);
			}
		};
		Collections.sort(files);
		for (int i = files.size() - 1; i >= 0; i--) {
			final int j = i;
			handlers[i] = new Handler<Void>() {
				@Override
				public void handle(Void v) {
					try {
						String file = files.get(j);
						log.info("Parsing file : " + file);
						importer.getReport().loadedFile(file);
						byte[] encoded = Files.readAllBytes(Paths.get(file));
						String content = UNESCAPE_AAF.translate(new String(encoded, "UTF-8"));
						InputSource in = new InputSource(new StringReader(content));
						AAFHandler sh = new AAFHandler(BaseImportProcessing.this);
						XMLReader xr = XMLReaderFactory.createXMLReader();
						xr.setContentHandler(sh);
						xr.setEntityResolver(new EntityResolver2() {
							@Override
							public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
								return null;
							}

							@Override
							public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
								return resolveEntity(publicId, systemId);
							}

							@Override
							public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
								if (systemId.equals("ficAlimMENESR.dtd")) {
									Reader reader = new FileReader(path + File.separator + "ficAlimMENESR.dtd");
									return new InputSource(reader);
								} else {
									return null;
								}
							}
						});
						xr.parse(in);
						importer.persist(new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> message) {
								if ("ok".equals(message.body().getString("status"))) {
									handlers[j + 1].handle(null);
								} else {
									error(message, handler);
								}
							}
						});
					} catch (Exception e) {
						error(e, handler);
					} catch (OutOfMemoryError err) { // badly catch Error to unlock importer
						log.error("OOM reading import files", err);
						error(new Exception("OOM"), handler);
					}
				}
			};
		}
		handlers[0].handle(null);
	}

	protected void next(final Handler<Message<JsonObject>> handler, final ImportProcessing importProcessing) {
		preCommit();
		if (importProcessing != null) {
			importer.persist(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					if ("ok".equals(message.body().getString("status"))) {
						importProcessing.start(handler);
					} else {
						error(message, handler);
					}
				}
			});
		} else {
			importer.persist(handler);
		}
	}

	protected void preCommit() {}

	protected void error(Exception e, Handler<Message<JsonObject>> handler) {
		log.error(e.getMessage(), e);
		if (handler != null) {
			handler.handle(null); // TODO return error message
		}
	}

	protected void error(Message<JsonObject> message, Handler<Message<JsonObject>> handler) {
		log.error(message.body().getString("message"));
		if (handler != null) {
			handler.handle(message);
		}
	}

	protected abstract String getFileRegex();

	protected void initAcademyPrefix(String file) {
		if (academyPrefix != null) return;
		if (file != null && file.contains(File.separator) && vertx.fileSystem()
				.existsBlocking(new File(file).getParent() + File.separator + AafFeeder.IMPORT_DIRECTORIES_JSON)) {
			if (file.endsWith(File.separator)) {
				file = file.substring(0, file.length() - 1);
			}
			try {
				JsonArray importDirectories = new fr.wseduc.webutils.collections.JsonArray(vertx.fileSystem()
						.readFileBlocking(new File(file).getParent() + File.separator + AafFeeder.IMPORT_DIRECTORIES_JSON).toString());
				final String[] a = file.split(File.separator);
				final String dirName = a[a.length - 1];
				if (a.length > 1 && importDirectories.contains(dirName)) {
					academyPrefix =  dirName + "-";
				} else {
					academyPrefix = "";
				}
			} catch (RuntimeException e) {
				log.error("Invalid import directories files.", e);
				academyPrefix = "";
			}
		} else {
			academyPrefix = "";
		}
	}

	public String getAcademyPrefix() {
		return academyPrefix;
	}

}
