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
import org.entcore.feeder.FeederLogger;
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
import java.util.function.Function;
import java.util.regex.Pattern;

public abstract class BaseImportProcessing implements ImportProcessing {

	protected final FeederLogger log;
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
	private static final int MAX_DEADLOCK_RETRIES = 1;
	private static final Pattern DEADLOCK_PATTERN = Pattern.compile("(deadlock|EntityNotFound)", Pattern.CASE_INSENSITIVE);

	protected BaseImportProcessing(String path, Vertx vertx) {
		this.path = path;
		this.vertx = vertx;
		log = new FeederLogger(e-> getTag(), e-> "academy: "+ academyPrefix);
	}

	protected String getTag(){
		return getClass().getSimpleName();
	}


	protected void parse(final Handler<Message<JsonObject>> handler, final ImportProcessing importProcessing) {
		log.info(e -> "START parsing directory : " + path);
		initAcademyPrefix(path);
		final List<String> files = vertx.fileSystem()
				.readDirBlocking(path, getFileRegex());
		final Handler[] handlers = new Handler[files.size() + 1];
		handlers[handlers.length -1] = new Handler<Integer>() {
			@Override
			public void handle(Integer v) {
				log.info(e -> "SUCCEED parsing directory : " + path);
				next(handler, importProcessing);
			}
		};
		Collections.sort(files);
		for (int i = files.size() - 1; i >= 0; i--) {
			final int j = i;
			handlers[i] = new Handler<Integer>() {
				@Override
				public void handle(Integer nbRetries) {
					final String file = files.get(j);
					try {
						log.info(e -> "START parsing file : " + file, true);
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
						log.info(e -> "START peristing file : " + file);
						importer.persist(new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> message) {
								if ("ok".equals(message.body().getString("status"))) {
									log.info(e -> "SUCCEED persist successfully for file : " + file);
									handlers[j + 1].handle(0);
								} else {
									log.error(e -> "FAILED persist for file : " + file);

									String msg = message.body().getString("message", "");
									if(nbRetries < MAX_DEADLOCK_RETRIES && DEADLOCK_PATTERN.matcher(msg).find() == true)
									{
										log.info(e -> "RETRY persist for file : " + file);
										handlers[j].handle(nbRetries + 1);
									}
									else
									{
										error(message, handler);
									}
								}
							}
						});
					} catch (Exception e) {
						error(e, handler);
						log.error(t -> "FAILED parsing file : " + file, e);
					} catch (OutOfMemoryError err) { // badly catch Error to unlock importer
						log.error(t -> "FAILED parsing file (OOM) : " + file, err);
						error(new Exception("OOM"), handler);
					}
				}
			};
		}
		handlers[0].handle(0);
	}

	protected void next(final Handler<Message<JsonObject>> handler, final ImportProcessing importProcessing) {
		log.info(t -> "START precommit");
		preCommit();
		if (importProcessing != null) {
			log.info(t -> "START precommit persist....");
			importer.persist(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					if ("ok".equals(message.body().getString("status"))) {
						log.info(t -> "SUCCEED precommit persist");
						importProcessing.start(handler);
					} else {
						log.error(t -> "FAILED precommit persist : "+ message.body().encode());
						error(message, handler);
					}
				}
			});
		} else {
			log.info(t -> "START precommit persist....");
			importer.persist(e->{
				//log
				if ("ok".equals(e.body().getString("status"))) {
					log.info(t -> "SUCCEED precommit persist");
				} else {
					log.error(t -> "FAILED precommit persist : "+ e.body().encode());
				}
				handler.handle(e);
			});
		}
	}

	protected void preCommit() {}

	protected void error(Exception e, Handler<Message<JsonObject>> handler) {
		log.error(t -> e.getMessage(), e);
		if (handler != null) {
			handler.handle(null); // TODO return error message
		}
	}

	protected void error(Message<JsonObject> message, Handler<Message<JsonObject>> handler) {
		log.error(t -> message.body().getString("message"));
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
				JsonArray importDirectories = new JsonArray(vertx.fileSystem()
						.readFileBlocking(new File(file).getParent() + File.separator + AafFeeder.IMPORT_DIRECTORIES_JSON).toString());
				final String[] a = file.split(File.separator);
				final String dirName = a[a.length - 1];
				if (a.length > 1 && importDirectories.contains(dirName)) {
					academyPrefix =  dirName + "-";
				} else {
					academyPrefix = "";
				}
			} catch (RuntimeException e) {
				log.error(t-> "Invalid import directories files.", e);
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
