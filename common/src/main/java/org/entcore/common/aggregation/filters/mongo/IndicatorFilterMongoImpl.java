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

package org.entcore.common.aggregation.filters.mongo;


import org.entcore.common.aggregation.filters.IndicatorFilter;
import org.entcore.common.aggregation.filters.dbbuilders.DBBuilder;
import org.entcore.common.aggregation.filters.dbbuilders.MongoDBBuilder;

/**
 * MongoDB implementation of the IndicatorFilter class.
 */
public abstract class IndicatorFilterMongoImpl implements IndicatorFilter{

	/**
	 * Add to an existing Mongo query builder filtering clauses.
	 * @param builder : Existing and already initialized query builder.
	 */
	public abstract void filter(MongoDBBuilder builder);
	public void filter(DBBuilder builder){
		filter((MongoDBBuilder) builder);
	}

}
