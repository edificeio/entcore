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

import java.util.Date;

import org.entcore.common.aggregation.filters.dbbuilders.MongoDBBuilder;

import static org.entcore.common.aggregation.MongoConstants.*;

/**
 * Filters traces by a date interval.
 */
public class DateFilter extends IndicatorFilterMongoImpl {
	
	private Date from, to;

	/**
	 * Creates a new DateFilter which will filter traces based on the two Date arguments.
	 * @param from : Lower bound (inclusive)
	 * @param to : Higher bound (not inclusive)
	 */
	public DateFilter(Date from, Date to) {
		this.from = from;
		this.to = to;
	}

	public void filter(MongoDBBuilder builder) {
		builder.and(TRACE_FIELD_DATE).greaterThanEquals(from.getTime()).lessThan(to.getTime());
	}

}
