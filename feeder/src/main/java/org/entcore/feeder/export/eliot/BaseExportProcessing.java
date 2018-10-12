/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.feeder.export.eliot;

import org.entcore.feeder.utils.AAFUtil;
import org.entcore.feeder.utils.JsonUtil;
import org.entcore.feeder.utils.ResultMessage;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.FileWriter;
import java.io.IOException;

public abstract class BaseExportProcessing implements ExportProcessing {

	protected static final Logger log = LoggerFactory.getLogger(BaseExportProcessing.class);
	private final int nbByFile;
	protected final JsonObject exportMapping;
	protected final String path;
	protected final boolean concat;
	private XMLEventWriter xmlEventWriter;
	private XMLEventFactory xmlEventFactory;

	protected BaseExportProcessing(String exportMapping, int nbByFile, String path, boolean concat) {
		this.path = path;
		this.exportMapping = JsonUtil.loadFromResource(exportMapping);
		this.nbByFile = nbByFile;
		this.concat = concat;
	}

	protected void export(final Handler<Message<JsonObject>> handler, final ExportProcessing exportProcessing) {
		count(new Handler<Integer>() {
			@Override
			public void handle(Integer nb) {
				if (nb == null) {
					error("invalid.count", handler);
					return;
				}
				final int nbHandlers = (nb % nbByFile == 0) ? nb / nbByFile : nb / nbByFile + 1;
				final Handler[] handlers = new Handler[nbHandlers + 1];
				handlers[handlers.length - 1] = new Handler<Void>() {
					@Override
					public void handle(Void v) {
						if (concat && xmlEventWriter != null) {
							try {
								closeDocument();
							} catch (IOException | XMLStreamException e) {
								error(e, handler);
								return;
							}
						}
						if (exportProcessing != null) {
							exportProcessing.start(handler);
						} else {
							handler.handle(new ResultMessage());
						}
					}
				};
				for (int i = handlers.length - 2; i >= 0; i--) {
					final int j = i;
					handlers[i] = new Handler<Void>() {
						@Override
						public void handle(Void v) {
							list(j*nbByFile, nbByFile, new Handler<JsonArray>() {
								@Override
								public void handle(JsonArray objects) {
									if (objects == null) {
										error("list.return.null.array", handler);
										return;
									}
									if (objects.size() > 0) {
										try {
											if (!concat) {
												writeDocument(j, objects);
											} else {
												if (xmlEventWriter == null) {
													xmlEventFactory = XMLEventFactory.newInstance();
													openDocument();
												}
												for (Object o : objects) {
													if (!(o instanceof JsonObject)) continue;
													writeElement(xmlEventWriter, xmlEventFactory, (JsonObject) o);
												}
											}
										} catch (IOException | XMLStreamException e) {
											error(e, handler);
											return;
										}
									}
									handlers[j + 1].handle(null);
								}
							});

						}
					};
				}
				handlers[0].handle(null);
			}
		});
	}

	private void openDocument() throws IOException, XMLStreamException {
		final String p = path + "0000.xml";
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		xmlEventWriter = outputFactory.createXMLEventWriter(new FileWriter(p));
		xmlEventWriter.add(xmlEventFactory.createStartDocument());
		xmlEventWriter.add(xmlEventFactory.createDTD("\n<!DOCTYPE ficAlimMENESR SYSTEM \"ficAlimMENESR.dtd\">\n"));
		xmlEventWriter.add(xmlEventFactory.createStartElement("", "", "ficAlimMENESR"));
		xmlEventWriter.add(xmlEventFactory.createDTD("\n\n"));
	}


	private void closeDocument() throws IOException, XMLStreamException {
		xmlEventWriter.add(xmlEventFactory.createEndElement("", "", "ficAlimMENESR"));
		xmlEventWriter.add(xmlEventFactory.createEndDocument());
		xmlEventWriter.flush();
		xmlEventWriter.close();
	}

	private void writeDocument(int j, JsonArray objects) throws IOException, XMLStreamException {
		final String p = path + String.format("%04d", j) + ".xml";
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		XMLEventWriter writer = outputFactory.createXMLEventWriter(new FileWriter(p));
		XMLEventFactory eventFactory = XMLEventFactory.newInstance();
		writer.add(eventFactory.createStartDocument());
		writer.add(eventFactory.createDTD("\n<!DOCTYPE ficAlimMENESR SYSTEM \"ficAlimMENESR.dtd\">\n"));
		writer.add(eventFactory.createStartElement("", "", "ficAlimMENESR"));
		writer.add(eventFactory.createDTD("\n\n"));
		for (Object o : objects) {
			if (!(o instanceof JsonObject)) continue;
			writeElement(writer, eventFactory, (JsonObject) o);
		}
		writer.add(eventFactory.createEndElement("", "", "ficAlimMENESR"));
		writer.add(eventFactory.createEndDocument());
		writer.flush();
		writer.close();
	}

	private void writeElement(XMLEventWriter writer, XMLEventFactory eventFactory,
			JsonObject element) throws XMLStreamException {
		if (element.getJsonArray("joinKey") == null && element.getString("externalId") != null) {
			element.put("joinKey", new fr.wseduc.webutils.collections.JsonArray().add(element.getString("externalId")));
		}
		writer.add(eventFactory.createStartElement("", "", "addRequest"));
		writer.add(eventFactory.createDTD("\n"));
		writer.add(eventFactory.createStartElement("", "", "operationalAttributes"));
		writer.add(eventFactory.createStartElement("", "", "attr"));
		process(writer, eventFactory);
		writer.add(eventFactory.createEndElement("", "", "attr"));
		writer.add(eventFactory.createEndElement("", "", "operationalAttributes"));
		writer.add(eventFactory.createDTD("\n"));
		writer.add(eventFactory.createStartElement("", "", "identifier"));
		writer.add(eventFactory.createStartElement("", "", "id"));
		writer.add(eventFactory.createCharacters(element.getString("externalId")));
		writer.add(eventFactory.createEndElement("", "", "id"));
		writer.add(eventFactory.createEndElement("", "", "identifier"));
		writer.add(eventFactory.createDTD("\n"));
		writer.add(eventFactory.createStartElement("", "", "attributes"));
		writer.add(eventFactory.createDTD("\n"));
		for (String attr : exportMapping.fieldNames()) {
			Object mapping = exportMapping.getValue(attr);
			Object value = element.getValue(attr);
			if (mapping instanceof JsonObject) {
				addAttribute((JsonObject) mapping, value, writer, eventFactory);
			} else {
				for (Object o : ((JsonArray) mapping)) {
					if (!(o instanceof JsonObject)) continue;
					addAttribute((JsonObject) o, value, writer, eventFactory);
				}
			}
		}
		writer.add(eventFactory.createEndElement("", "", "attributes"));
		writer.add(eventFactory.createDTD("\n"));
		writer.add(eventFactory.createEndElement("", "", "addRequest"));
		writer.add(eventFactory.createDTD("\n\n"));
	}

	private void addAttribute(JsonObject object, Object value, XMLEventWriter writer,
			XMLEventFactory eventFactory) throws XMLStreamException {
		final Object v = AAFUtil.convert(value, object.getString("converter"));
		if (v instanceof JsonObject) {
			JsonObject j = (JsonObject) v;
			for (String attr : j.fieldNames()) {
				addSingleAttribute(attr, j.getValue(attr), writer, eventFactory);
			}
		} else {
			String attr = object.getString("attribute");
			if (attr != null) {
				addSingleAttribute(attr, v, writer, eventFactory);
			}
		}
	}

	private void addSingleAttribute(String key, Object v, XMLEventWriter writer, XMLEventFactory eventFactory)
			throws XMLStreamException {
		writer.add(eventFactory.createStartElement("", "", "attr"));
		writer.add(eventFactory.createAttribute("name", key));
		if (v instanceof JsonArray) {
			for (Object o : (JsonArray) v) {
				writer.add(eventFactory.createStartElement("", "", "value"));
				writer.add(eventFactory.createCharacters(o.toString()));
				writer.add(eventFactory.createEndElement("", "", "value"));
			}
		} else {
			writer.add(eventFactory.createStartElement("", "", "value"));
			writer.add(eventFactory.createCharacters(v.toString()));
			writer.add(eventFactory.createEndElement("", "", "value"));
		}
		writer.add(eventFactory.createEndElement("", "", "attr"));
		writer.add(eventFactory.createDTD("\n"));
	}

	protected abstract void process(XMLEventWriter writer, XMLEventFactory eventFactory) throws XMLStreamException;

	protected abstract void count(Handler<Integer> handler);

	protected abstract void list(Integer skip, Integer limit, Handler<JsonArray> entities);

	protected void error(String reason, Handler<Message<JsonObject>> handler) {
		log.error(reason);
		if (handler != null) {
			handler.handle(new ResultMessage().error(reason));
		}
	}

	protected void error(Exception e, Handler<Message<JsonObject>> handler) {
		log.error(e.getMessage(), e);
		if (handler != null) {
			handler.handle(new ResultMessage().error(e.getMessage()));
		}
	}

}
