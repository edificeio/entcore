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
