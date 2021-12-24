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

	public void exportFiles(final Handler<AsyncResult<String>> handler)
	{
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
		FileUtils.deleteImportPath(vertx, originalPath, resDel ->
		{
			String basePath;

			if(resDel.failed())
			{			
				log.error("[CsvReport] could not clean path before exporting: "+ resDel.cause().getMessage());

				//#30406 use another folder
				basePath = originalPath + File.separator + "exported";
				result.put("path", basePath);

				log.info("[CsvReport] change exportDir to: "+ basePath);
			}
			else
				basePath = originalPath;

			final String dirPath = (basePath + File.separator + structureName +
					(isNotEmpty(structureExternalId) ? "@" + structureExternalId: "") +
					(isNotEmpty(UAI) ? "_" + UAI : ""));

			// Create the new directory
			fs.mkdirs(dirPath, new Handler<AsyncResult<Void>>()
			{
				@Override
				public void handle(AsyncResult<Void> event)
				{
					try {
						if (event.succeeded())
						{
							JsonArray writeErrors = new JsonArray();
							writeFiles(dirPath, writeErrors);

							if(writeErrors.size() == 0)
								handler.handle(new DefaultAsyncResult<>(basePath));
							else
								handler.handle(new DefaultAsyncResult<String>(new ValidationException("missing.file." + writeErrors.getString(0))));
						}
						else {
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

	private void writeFiles(String dirPath, JsonArray errors) throws IOException
	{
		final JsonObject headers = result.getJsonObject(HEADERS);
		final JsonObject files = result.getJsonObject(FILES);

		for (String file : files.fieldNames())
		{
			final JsonArray header = headers.getJsonArray(file);
			final JsonArray lines = files.getJsonArray(file);

			if (lines == null || lines.size() == 0 || header == null || header.size() == 0)
			{
				errors.add(file);
				return;
			}

			final CSVWriter fileOut = CSVUtil.getCsvWriter(dirPath + File.separator + file, "UTF-8");

			final String[] headerStrings = new ArrayList<String>(header.getList()).toArray(new String[header.size()]);
			final List<String> columns = new ArrayList<>();

			// Write the header to the new file
			fileOut.writeNext(headerStrings);

			columnsMapper.getMappedColumsNames(file, headerStrings, columns);
			
			for (Object o : lines)
			{
				if (!(o instanceof JsonObject)) continue;

				final JsonObject line = (JsonObject) o;
				if (Report.State.DELETED.name().equals(line.getString("oState"))) {
					continue;
				}

				fileOut.writeNext(jsonLineToFile(line, headerStrings.length, columns));
			}
			fileOut.close();
		}
	}

	private String[] jsonLineToFile(JsonObject line, int lineLength, List<String> columns)
	{
		final Map<String, Integer> columnCount = new HashMap<>();
		final String [] lineValues = new String[lineLength];
		int i = 0;

		for (String column : columns)
		{
			Object value = line.getValue(column);
			Integer count = getOrElse(columnCount.get(column), 0);

			if (value instanceof String)
			{
				// In case there are multiple columns with the same name, fill the first one and leave others empty
				if (count == 0) {
					//if (column.startsWith("child")) {
						lineValues[i] = cleanStructure((String) value);
//					} else {
//						lineValues[i] = (String) value;
//					}
				}
			}
			else if (value instanceof JsonArray)
			{
				if (((JsonArray) value).size() > count) {
					//if (column.startsWith("child")) {
						lineValues[i] = cleanStructure(((JsonArray) value).getString(count));
//					} else {
//						lineValues[i] = ((JsonArray) value).<String>get(count);
//					}
				}
			}
			else if (value instanceof  Boolean) {
				lineValues[i] = String.valueOf(value);
			}
			else {
				lineValues[i] = "";
			}

			columnCount.put(column, ++count);
			i++;
		}

		return lineValues;
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
