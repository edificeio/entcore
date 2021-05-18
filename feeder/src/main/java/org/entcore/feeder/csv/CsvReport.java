/*
 * Copyright © WebServices pour l'Éducation, 2017
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
 */

package org.entcore.feeder.csv;

import com.opencsv.CSVWriter;
import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.file.FileSystem;
import org.entcore.common.utils.FileUtils;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.feeder.utils.CSVUtil;
import org.entcore.feeder.utils.Report;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class CsvReport extends Report {

	public static final String MAPPINGS = "mappings";
	private static final String CLASSES_MAPPING = "classesMapping";
	private static final String HEADERS = "headers";
	private final Vertx vertx;
	protected final ProfileColumnsMapper columnsMapper;

	public CsvReport(Vertx vertx, JsonObject importInfos) {
		super(importInfos.getString("language", "fr"));
		final String importId = importInfos.getString("id");
		if (isNotEmpty(importId)) {
			importInfos.put("_id", importId);
			importInfos.remove("id");
		}
		result.mergeIn(importInfos);
		if (result.getBoolean(KEYS_CLEANED, false)) {
			uncleanKeys();
		}
		this.vertx = vertx;
		this.columnsMapper = new ProfileColumnsMapper(getMappings());
	}

	public void addHeader(String profile, JsonArray header) {
		JsonObject headers = result.getJsonObject(HEADERS);
		if (headers == null) {
			headers = new JsonObject();
			result.put(HEADERS, headers);
		}
		headers.put(profile, header);
	}

	public void addMapping(String profile, JsonObject mapping) {
		JsonObject mappings = result.getJsonObject(MAPPINGS);
		if (mappings == null) {
			mappings = new JsonObject();
			result.put(MAPPINGS, mappings);
		}
		mappings.put(profile, mapping);
	}

	public JsonObject getMappings() {
		return result.getJsonObject(MAPPINGS);
	}

	public void setMappings(JsonObject mappings) {
		if (mappings != null && mappings.size() > 0) {
			result.put(MAPPINGS, mappings);
		}
	}

	public void setClassesMapping(JsonObject mapping) {
		if (mapping != null && mapping.size() > 0) {
			result.put(CLASSES_MAPPING, mapping);
		}
	}

	public JsonObject getClassesMapping(String profile) {
		final JsonObject cm = result.getJsonObject(CLASSES_MAPPING);
		if (cm != null) {
			return cm.getJsonObject(profile);
		}
		return null;
	}

	public JsonObject getClassesMappings() {
		return result.getJsonObject(CLASSES_MAPPING);
	}

	public void exportFiles(final Handler<AsyncResult<String>> handler) {
		final String originalPath = result.getString("path");
		final String structureName = result.getString("structureName");
		final JsonObject headers = result.getJsonObject(HEADERS);
		final JsonObject files = result.getJsonObject(FILES);
		if (files == null || isEmpty(originalPath) || isEmpty(structureName) || headers == null) {
			handler.handle(new DefaultAsyncResult<String>(new ValidationException("missing.arguments")));
			return;
		}
		FileSystem fs = vertx.fileSystem();
		final String structureExternalId = result.getString("structureExternalId");
		final String UAI = result.getString("UAI");

		//clean directory if exists
		FileUtils.deleteImportPath(vertx, originalPath, resDel ->{
			if(resDel.failed()){
				log.error("[CsvReport] could not clean path before exporting: "+ resDel.cause().getMessage());
			}
			//#30406 use another folder
			final String path = resDel.failed()? originalPath + File.separator + "exported" : originalPath;
			if(!originalPath.equals(path)){
				log.info("[CsvReport] change exportDir to: "+ path);
				result.put("path", path);
			}
			final String p = (path + File.separator + structureName +
					(isNotEmpty(structureExternalId) ? "@" + structureExternalId: "") +
					(isNotEmpty(UAI) ? "_" + UAI : ""));
			fs.mkdirs(p, new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					try {
						if (event.succeeded()) {
							for (String file : files.fieldNames()) {
								final JsonArray header = headers.getJsonArray(file);
								final JsonArray lines = files.getJsonArray(file);
								if (lines == null || lines.size() == 0 || header == null || header.size() == 0) {
									handler.handle(new DefaultAsyncResult<String>(new ValidationException("missing.file." + file)));
									return;
								}
								final CSVWriter writer = CSVUtil.getCsvWriter(p + File.separator + file, "UTF-8");
								final String[] strings = new ArrayList<String>(header.getList()).toArray(new String[header.size()]);
								final List<String> columns = new ArrayList<>();
								writer.writeNext(strings);
								columnsMapper.getColumsNames(file, strings, columns);
								for (Object o : lines) {
									if (!(o instanceof JsonObject)) continue;
									final JsonObject line = (JsonObject) o;
									if (Report.State.DELETED.name().equals(line.getString("oState"))) {
										continue;
									}
									final Map<String, Integer> columnCount = new HashMap<>();
									final String [] l = new String[strings.length];
									int i = 0;
									for (String column : columns) {
										Object v = line.getValue(column);
										Integer count = getOrElse(columnCount.get(column), 0);
										if (v instanceof String) {
											if (count == 0) {
												//if (column.startsWith("child")) {
													l[i] = cleanStructure((String) v);
	//											} else {
	//												l[i] = (String) v;
	//											}
											}
										} else if (v instanceof JsonArray) {
											if (((JsonArray) v).size() > count) {
												//if (column.startsWith("child")) {
													l[i] = cleanStructure(((JsonArray) v).getString(count));
	//											} else {
	//												l[i] = ((JsonArray) v).<String>get(count);
	//											}
											}
										} else if (v instanceof  Boolean) {
											l[i] = String.valueOf(v);
										} else {
											l[i] = "";
										}
										columnCount.put(column, ++count);
										i++;
									}
									writer.writeNext(l);
								}
								writer.close();
							}
							handler.handle(new DefaultAsyncResult<>(path));
						} else {
							handler.handle(new DefaultAsyncResult<String>(event.cause()));
						}
					} catch (IOException e) {
						handler.handle(new DefaultAsyncResult<String>(e));
						// TODO delete directory
					}
				}
			});
		});
	}

	private String cleanStructure(String v) {
		if (v != null && v.contains("$")) {
			return v.substring(v.indexOf("$") + 1);
		}
		return v;
	}

	@Override
	public String getSource() {
		return "CSV";
	}

	public String getStructureExternalId() {
		return result.getString("structureExternalId");
	}

	@Override
	protected void cleanKeys() {
		int count = 0;
		count += cleanAttributeKeys(getClassesMappings());
		count += cleanAttributeKeys(getMappings());
		count += cleanAttributeKeys(result.getJsonObject("errors"));
		if (count > 0) {
			result.put(KEYS_CLEANED, true);
		}
	}

	@Override
	protected boolean updateCleanKeys() {
		return (cleanAttributeKeys(result.getJsonObject("errors")) + cleanAttributeKeys(result.getJsonObject("softErrors"))) > 0;
	}

	protected void uncleanKeys() {
		uncleanAttributeKeys(getClassesMappings());
		uncleanAttributeKeys(getMappings());
		uncleanAttributeKeys(result.getJsonObject("errors"));
		result.remove(KEYS_CLEANED);
	}

	protected void clearBeforeValidation() {
		result.put("errors", new JsonObject())
				.put(FILES, new JsonObject())
				.put("softErrors", new JsonObject());
	}

	protected void setSeed(long seed) {
		result.put("seed", seed);
	}

	protected Long getSeed() {
		return result.getLong("seed", new Random().nextLong());
	}

//	protected void setStructureExternalIdIfAbsent(String structureExternalId) {
//		if (isEmpty(result.getString("structureExternalId"))) {
//			result.put("structureExternalId", structureExternalId);
//		}
//	}

}
