package org.entcore.feeder.aaf;

import org.entcore.feeder.dictionary.structures.Importer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.FileInputStream;
import java.util.Arrays;

public abstract class BaseImportProcessing implements ImportProcessing {

	protected static final Logger log = LoggerFactory.getLogger(BaseImportProcessing.class);
	protected final String path;
	protected final Vertx vertx;
	protected final Importer importer = Importer.getInstance();

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
						InputSource in = new InputSource(new FileInputStream(file));
						AAFHandler sh = new AAFHandler(BaseImportProcessing.this);
						XMLReader xr = XMLReaderFactory.createXMLReader();
						xr.setContentHandler(sh);
						xr.parse(in);
						importer.flush(new Handler<Message<JsonObject>>() {
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
