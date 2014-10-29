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
