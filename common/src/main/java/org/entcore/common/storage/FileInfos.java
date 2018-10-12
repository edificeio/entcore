/*
 * Copyright © "Open Digital Education", 2017
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
