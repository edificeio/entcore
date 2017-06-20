/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.directory.pojo;

import static fr.wseduc.webutils.Utils.*;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.collections.Joiner;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportInfos {

	private static final Logger log = LoggerFactory.getLogger(ImportInfos.class);

	private String UAI;
	private String structureName;
	enum ImportType { CSV }
	private ImportType type;
	private boolean preDelete = false;
	private boolean transition = false;
	private String path;
	private String id;
	private String structureId;
	private String structureExternalId;
	private String overrideClass;
	private Map<String, Object> mappings;
	private Map<String, Object> classesMapping;
	private String language;

	public String getFeeder() {
		return type.name();
	}

	public void setFeeder(String type) {
		this.type = ImportType.valueOf(type);
	}

	public boolean isPreDelete() {
		return preDelete;
	}

	public void setPreDelete(boolean preDelete) {
		this.preDelete = preDelete;
	}

	public boolean isTransition() {
		return transition;
	}

	public void setTransition(boolean transition) {
		this.transition = transition;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStructureId() {
		return structureId;
	}

	public void setStructureId(String structureId) {
		this.structureId = structureId;
	}

	public void setUAI(String UAI) {
		this.UAI = UAI;
	}

	public void setStructureName(String structureName) {
		this.structureName = structureName;
	}

	public String getStructureName() {
		return structureName;
	}

	public String getUAI() {
		return UAI;
	}

	public String getStructureExternalId() {
		return structureExternalId;
	}

	public void setStructureExternalId(String structureExternalId) {
		this.structureExternalId = structureExternalId;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getOverrideClass() {
		return overrideClass;
	}

	public void setOverrideClass(String overrideClass) {
		this.overrideClass = overrideClass;
	}

	public Map<String, Object> getMappings() {
		return mappings;
	}

	public void setMappings(JsonObject mappings) {
		this.mappings = (mappings != null) ? mappings.getMap() : null;
	}

	public Map<String, Object> getClassesMapping() {
		return classesMapping;
	}

	public void setClassesMapping(JsonObject classesMapping) {
		this.classesMapping = (classesMapping != null) ? classesMapping.getMap() : null;
	}

	public void validate(final boolean isAdmc, final Vertx vertx, final Handler<AsyncResult<String>> handler) {
		if (!isAdmc && isEmpty(structureId)) {
			handler.handle(new DefaultAsyncResult<>("invalid.structure.id"));
		} else if (isEmpty(structureName)) {
			handler.handle(new DefaultAsyncResult<>("invalid.structure.name"));
		} else if (ImportType.CSV == type) {
			final FileSystem fs = vertx.fileSystem();
			fs.readDir(path, new Handler<AsyncResult<List<String>>>() {
				@Override
				public void handle(AsyncResult<List<String>> list) {
					if (list.succeeded()) {
						if (list.result().size() > 0) {
							moveFiles(list.result(), fs, handler);
						} else {
							handler.handle(new DefaultAsyncResult<>("missing.csv.files"));
						}
					} else {
						handler.handle(new DefaultAsyncResult<String>(list.cause()));
					}
				}
			});
		} else {
			handler.handle(new DefaultAsyncResult<>("invalid.import.type"));
		}
	}

	private void moveFiles(final List<String> l, final FileSystem fs, final Handler<AsyncResult<String>> handler) {
		final String p = path + File.separator + structureName +
				(isNotEmpty(structureExternalId) ? "@" + structureExternalId: "") + "_" +
				(isNotEmpty(UAI) ? UAI : "") + "_" + (isNotEmpty(overrideClass) ? overrideClass : "");
		fs.mkdir(p, new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					final AtomicInteger count = new AtomicInteger(l.size());
					for (String f: l) {
						fs.move(f, p + File.separator + f.substring(path.length() + 1), new Handler<AsyncResult<Void>>() {
							@Override
							public void handle(AsyncResult<Void> event2) {
								if (event2.succeeded()) {
									if (count.decrementAndGet() == 0) {
										handler.handle(new DefaultAsyncResult<>((String) null));
									}
								} else {
									count.set(-1);
									handler.handle(new DefaultAsyncResult<String>(event2.cause()));
								}
							}
						});
					}
				} else {
					handler.handle(new DefaultAsyncResult<String>(event.cause()));
				}
			}
		});
	}

}
