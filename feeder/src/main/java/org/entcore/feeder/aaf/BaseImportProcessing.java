/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.feeder.aaf;

import org.apache.commons.lang3.text.translate.*;
import org.entcore.feeder.dictionary.structures.Importer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public abstract class BaseImportProcessing implements ImportProcessing {

	protected static final Logger log = LoggerFactory.getLogger(BaseImportProcessing.class);
	protected final String path;
	protected final Vertx vertx;
	protected final Importer importer = Importer.getInstance();
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
		final String [] files = vertx.fileSystem()
				.readDirSync(path, getFileRegex());
		final VoidHandler[] handlers = new VoidHandler[files.length + 1];
		handlers[handlers.length -1] = new VoidHandler() {
			@Override
			protected void handle() {
				next(handler, importProcessing);
			}
		};
		Arrays.sort(files);
		for (int i = files.length - 1; i >= 0; i--) {
			final int j = i;
			handlers[i] = new VoidHandler() {
				@Override
				protected void handle() {
					try {
						String file = files[j];
						log.info("Parsing file : " + file);
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


}
