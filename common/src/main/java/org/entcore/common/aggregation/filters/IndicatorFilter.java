package org.entcore.common.aggregation.filters;

import org.entcore.common.aggregation.filters.dbbuilders.DBBuilder;

/**
 * An IndicatorFilter is used by an Indicator to filter traces.
 */
public interface IndicatorFilter {

	/**
	 * Add to an existing query builder filtering clauses.
	 * @param builder : Already initialized filtering query builder, which can be appended with filtering clauses.
	 */
	public void filter(DBBuilder builder);
	
}
