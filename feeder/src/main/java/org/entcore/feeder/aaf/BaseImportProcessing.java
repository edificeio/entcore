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
import fr.wseduc.webutils.eventbus.ResultMessage;
import org.entcore.feeder.FeederLogger;
import org.entcore.feeder.dictionary.structures.Importer;
import io.vertx.core.Future;
import io.vertx.core.Promise;
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
	private final String validationFilePath;

	protected BaseImportProcessing(String path, Vertx vertx) {
		this.path = path;
		this.vertx = vertx;
		this.validationFilePath = vertx.getOrCreateContext().config().getString("validation-file-path", null);
		log = new FeederLogger(e-> getTag(), e-> "academy: "+ academyPrefix);
	}

	protected String getTag(){
		return getClass().getSimpleName();
	}


	protected void parse(final Handler<Message<JsonObject>> handler, final ImportProcessing importProcessing) {
		log.info(e -> "START parsing directory : " + path);
		initAcademyPrefix(path);
		try {
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
										Reader reader;
										if (validationFilePath != null && !validationFilePath.isEmpty()) {
											reader = new FileReader(validationFilePath);
										} else {
											reader = new FileReader(path + File.separator + "ficAlimMENESR.dtd");
										}
										return new InputSource(reader);
									} else {
										return null;
									}
								}
							});
							// Parse the raw file directly and unescape only each markup value in
							// AAFHandler : avoids copying the whole file just to unescape it up-front.
							try (InputStream is = new BufferedInputStream(Files.newInputStream(Paths.get(file)))) {
								InputSource in = new InputSource(is);
								in.setEncoding("UTF-8");
								xr.parse(in);
							}
							log.info(e -> "START peristing file : " + file);
							importer.persist(message -> {
                                if ("ok".equals(message.body().getString("status"))) {
                                    log.info(e -> "SUCCEED persist successfully for file : " + file);
									handlers[j] = null;
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
                            });
						} catch (Exception e) {
							error(e, handler);
							log.error(t -> "FAILED parsing file : " + file, e);
						}
					}
				};
			}
			handlers[0].handle(0);
		} catch (Exception e) {
			log.error(t -> "FAILED parsing directory : " + path, e);
			error(e, handler);
		}
	}

	protected void next(final Handler<Message<JsonObject>> handler, final ImportProcessing importProcessing) {
		log.info(t -> "START precommit");
		// Enclosing-step finalization, linearized : stage (preCommit) -> commit the staged work
		// once (persist) -> run the step's own bounded/batched transactions (postCommit) -> hand
		// over to the next processing step (or forward the result to the terminal handler).
		preCommit()
			.compose(v -> {
				log.info(t -> "START precommit persist....");
				final Promise<Message<JsonObject>> promise = Promise.promise();
				importer.persist(promise::complete);
				return promise.future();
			})
			.compose(message -> {
				// A null message means the persist transaction was empty (preCommit staged
				// nothing) : success, not a failure. Normalize it to a real "ok" message.
				final Message<JsonObject> m = (message != null) ? message : new ResultMessage();
				if ("ok".equals(m.body().getString("status"))) {
					log.info(t -> "SUCCEED precommit persist" + (message == null ? " (empty transaction)" : ""));
					// postCommit owns the enclosing step's bounded transactions ; carry the
					// persist message through so the terminal step can forward it downstream.
					return postCommit().map(v -> m);
				}
				log.error(t -> "FAILED precommit persist : " + m.body().encode());
				return Future.failedFuture(new PersistFailure(m));
			})
			.onComplete(ar -> {
				if (ar.succeeded()) {
					if (importProcessing != null) {
						importProcessing.start(handler);
					} else {
						handler.handle(ar.result());
					}
				} else if (ar.cause() instanceof PersistFailure) {
					error(((PersistFailure) ar.cause()).message, handler);
				} else {
					log.error(t -> "FAILED precommit/postcommit", ar.cause());
					error(new Exception(ar.cause()), handler);
				}
			});
	}

	// Staging hook : add queries to the current transaction ; the persist() that follows in
	// next() commits them once. Implementations must NOT commit here.
	protected Future<Void> preCommit() {
		return Future.succeededFuture();
	}

	// Finalization hook run after persist() : owns its own (possibly batched) commits for the
	// enclosing step. Kept separate from preCommit so a step that drives its own bounded
	// transactions does not distort the staging contract of preCommit.
	protected Future<Void> postCommit() {
		return Future.succeededFuture();
	}

	// Carries a failing persist message through the Future chain so the terminal onComplete can
	// forward it (error(message, handler)) instead of collapsing it to a bare cause.
	private static final class PersistFailure extends Exception {
		private final Message<JsonObject> message;
		private PersistFailure(Message<JsonObject> message) {
			this.message = message;
		}
	}


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
