/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.common.sql;

public class SqlConf {

	private String table;
	private String schema;
	private String resourceIdLabel = "id";
	private SqlConf() {}

	private static class SqlConfHolder {
		private static final SqlConf instance = new SqlConf();
	}

	public static SqlConf getInstance() {
		return SqlConfHolder.instance;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = (schema != null && !schema.trim().isEmpty()) ? schema + "." : "";
	}

	public String getResourceIdLabel() {
		return resourceIdLabel;
	}

	public void setResourceIdLabel(String resourceIdLabel) {
		this.resourceIdLabel = resourceIdLabel;
	}

}
