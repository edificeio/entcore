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

package org.entcore.common.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileInfos {

	private String id;
	private String name;
	private String application;
	private String owner;
	private String contentType;
	private Integer size;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public JsonObject toJsonExcludeEmpty() {
		return toJsonExcludeEmpty(null);
	}

	public JsonObject toJsonExcludeEmpty(JsonObject mapping) {
		if (mapping == null) {
			mapping = new JsonObject();
		}
		final JsonObject j = new JsonObject();
		if (isNotEmpty(id)) {
			j.put(mapping.getString("id", "id"), id);
		}
		if (isNotEmpty(name)) {
			j.put(mapping.getString("name", "name"), name);
		}
		if (isNotEmpty(application)) {
			j.put(mapping.getString("application", "application"), application);
		}
		if (isNotEmpty(owner)) {
			j.put(mapping.getString("owner", "owner"), owner);
		}
		if (isNotEmpty(contentType)) {
			j.put(mapping.getString("contentType", "contentType"), contentType);
		}
		if (size != null) {
			j.put(mapping.getString("size", "size"), size);
		}
		return j;
	}

}
