/*
 * Copyright © WebServices pour l'Éducation, 2016
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

package org.entcore.directory.pojo;

import static fr.wseduc.webutils.Utils.*;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.collections.Joiner;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.file.FileSystem;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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

	public void validate(final boolean isAdmc, final Vertx vertx, final Handler<AsyncResult<String>> handler) {
		if (!isAdmc && isEmpty(structureId)) {
			handler.handle(new DefaultAsyncResult<>("invalid.structure.id"));
		} else if (isEmpty(structureName)) {
			handler.handle(new DefaultAsyncResult<>("invalid.structure.name"));
		} else if (ImportType.CSV == type) {
			final FileSystem fs = vertx.fileSystem();
			fs.readDir(path, new Handler<AsyncResult<String[]>>() {
				@Override
				public void handle(AsyncResult<String[]> list) {
					if (list.succeeded()) {
						if (list.result().length > 0) {
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

	private void moveFiles(final String [] l, final FileSystem fs, final Handler<AsyncResult<String>> handler) {
		final String p = path + File.separator + structureName +
				(isNotEmpty(structureExternalId) ? "@" + structureExternalId: "") + "_" +
				(isNotEmpty(UAI) ? UAI : "") + "_" + (isNotEmpty(overrideClass) ? overrideClass : "");
		fs.mkdir(p, new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					final AtomicInteger count = new AtomicInteger(l.length);
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
