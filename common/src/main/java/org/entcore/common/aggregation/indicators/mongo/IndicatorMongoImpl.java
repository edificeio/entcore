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

package org.entcore.common.aggregation.indicators.mongo;

import static org.entcore.common.aggregation.MongoConstants.STATS_FIELD_DATE;
import static org.entcore.common.aggregation.MongoConstants.STATS_FIELD_GROUPBY;
import static org.entcore.common.aggregation.MongoConstants.TRACE_FIELD_TYPE;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.entcore.common.aggregation.MongoConstants.COLLECTIONS;
import org.entcore.common.aggregation.filters.IndicatorFilter;
import org.entcore.common.aggregation.filters.dbbuilders.MongoDBBuilder;
import org.entcore.common.aggregation.filters.mongo.IndicatorFilterMongoImpl;
import org.entcore.common.aggregation.groups.IndicatorGroup;
import org.entcore.common.aggregation.indicators.Indicator;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;

public class IndicatorMongoImpl extends Indicator{

	//MongoDB instance
	protected final MongoDb mongo;

	//Output key
	private String writtenIndicatorKey;

	//Logger
	private Logger log = LoggerFactory.getLogger(IndicatorMongoImpl.class);

	/**
	 * Creates a new Indicator without filters or groups.<br>
	 * @param key : Traces will be filtered using this String and an equality check against the type of trace.
	 */
	public IndicatorMongoImpl(String key){
		super(key);
		this.mongo = MongoDb.getInstance();
		this.writtenIndicatorKey = key;
	}
	/**
	 * Creates a new Indicator with filters and / or groups.<br>
	 * @param key : Traces will be filtered using this String and an equality check against the type of trace.
	 * @param filters : Filters are used to filter traces further than the default type filtering.
	 * @param groups : Groups are used to group results by several keys or combination of keys.
	 */
	public IndicatorMongoImpl(String key, Collection<IndicatorFilterMongoImpl> filters, Collection<IndicatorGroup> groups){
		super(key, filters, groups);
		this.mongo = MongoDb.getInstance();
		this.writtenIndicatorKey = key;
	}

	/* WRITE TO DB */

	/**
	 * Changes the key used when writing the aggregated amount in the Mongo collection.
	 * If left unchanged the indicator key will be used.
	 * @param key : The new key
	 */
	public void setWriteKey(String key){
		this.writtenIndicatorKey = key;
	}

	/**
	 * Returns the key used when writing the aggregated amount in the Mongo collection.
	 * @return : String key
	 */
	public String getWriteKey(){
		return this.writtenIndicatorKey;
	}

	//Write aggregated data to the database, using data from a Mongo count
	private void writeStats(JsonArray results, final IndicatorGroup group, final Handler<JsonObject> callBack){

		//If no documents found, write nothing
		if(results.size() == 0){
			callBack.handle(new JsonObject());
			return;
		}

		//Document date
		Date writeDate = this.writeDate;

		final MongoDBBuilder criteriaQuery = new MongoDBBuilder();

		//Synchronization handler
		final AtomicInteger countDown = new AtomicInteger(results.size());
		Handler<Message<JsonObject>> synchroHandler = new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> message) {
				if (!"ok".equals(message.body().getString("status"))){
					String groupstr = group == null ? "Global" : group.toString();
					log.error("[Aggregation][Error]{"+writtenIndicatorKey+"} ("+ groupstr +") writeStats : "+message.body().toString());
					//log.info(criteriaQuery.toString());
				}

				if(countDown.decrementAndGet() == 0){
					callBack.handle(new JsonObject());
				}
			}
		};
		//For each aggregated result
		for(Object obj: results){
			JsonObject result = (JsonObject) obj;

			if(group == null){
				//When not using groups, set groupedBy specifically to not exists
				criteriaQuery
					.put(STATS_FIELD_DATE).is(MongoDb.formatDate(writeDate))
					.and(STATS_FIELD_GROUPBY).exists(false);
			} else {
				//Adding date & group by to the criterias.
				criteriaQuery
					.put(STATS_FIELD_DATE).is(MongoDb.formatDate(writeDate))
					.and(STATS_FIELD_GROUPBY).is(group.toString());

				//Adding the group ids values
				IndicatorGroup g = group;
				while(g != null){
					criteriaQuery.and(g.getKey()+"_id").is(result.getJsonObject("_id").getString(g.getKey()));
					g = g.getParent();
				}
			}

			//Perform write action
			writeAction(criteriaQuery, result.getInteger("count"), synchroHandler);
		}
	}

	/**
	 * <em><b>You may override this method in order to perform a custom write action.</b></em><br>
	 * Default write action performed on each aggregated result,
	 * increments the MongoDB collection entry with the results count.
	 *
	 * @param criteriaQuery : Already built query, containing the write date, the aggregated values and the group label.
	 * @param resultsCount : Aggregation count.
	 * @param handler : Synchronization handler, which must be called as a continuation.
	 */
	protected void writeAction(MongoDBBuilder criteriaQuery, int resultsCount, Handler<Message<JsonObject>> handler){
		mongo.update(COLLECTIONS.stats.name(),
				MongoQueryBuilder.build(criteriaQuery),
				new MongoUpdateBuilder().inc(writtenIndicatorKey, resultsCount).build(),
				true,
				true,
				handler);
	}

	//Unwind clauses of the aggregation pipeline - useful for flattening arrays
	private void addUnwindPipeline(JsonArray pipeline, IndicatorGroup group){
		if(group == null)
			return;

		addUnwindPipeline(pipeline, group.getParent());
		if(group.isArray())
			pipeline.add(new JsonObject().put("$unwind", "$"+group.getKey()));
	}

	//Building the $group _id object
	private JsonObject getGroupByObject(JsonObject accumulator, IndicatorGroup group){
		if(group == null)
			return accumulator;

		return getGroupByObject(accumulator, group.getParent()).put(group.getKey(), "$"+group.getKey());
	}

	/**
	 * Override this function to modify the $group stage before the aggregation query.
	 * @param groupBy $group stage object
	 */
	protected void customizeGroupBy(JsonObject groupBy){}

	/**
	 * Override this function to modify the pipeline before the aggregation query.
	 * @param pipeline pipeline object containing $match, $unwind and $group stages.
	 */
	protected void customizePipeline(JsonArray pipeline){}

	//Builds and executes an entire aggregation pipeline query for a given group, and recurse for each child.
	private void executeAggregationQuery(final IndicatorGroup group, final Handler<JsonObject> finalHandler){
		//Filter by trace type + custom filters
		final MongoDBBuilder filteringQuery = (MongoDBBuilder) new MongoDBBuilder().put(TRACE_FIELD_TYPE).is(indicatorKey);
		for(IndicatorFilter filter : filters){
			filter.filter(filteringQuery);
		}

		final JsonObject aggregation = new JsonObject();
		JsonArray pipeline = new JsonArray();
		aggregation
			.put("aggregate", COLLECTIONS.events.name())
			.put("allowDiskUse", true)
			.put("pipeline", pipeline);

		pipeline.add(new JsonObject().put("$match", MongoQueryBuilder.build(filteringQuery)));
		addUnwindPipeline(pipeline, group);
		JsonObject groupBy = new JsonObject().put("$group", new JsonObject()
			.put("_id", getGroupByObject(new JsonObject(), group))
			.put("count", new JsonObject().put("$sum", 1)));
		pipeline.add(groupBy);

		//Customize the request if needed
		customizeGroupBy(groupBy);
		customizePipeline(pipeline);

		mongo.command(aggregation.toString(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				if ("ok".equals(message.body().getString("status")) && message.body().getJsonObject("result", new JsonObject()).getInteger("ok") == 1){
					JsonArray result = message.body().getJsonObject("result").getJsonArray("result");
					writeStats(result, group, finalHandler);
				} else {
					String groupstr = group == null ? "Global" : group.toString();
					log.error("[Aggregation][Error]{"+writtenIndicatorKey+"} ("+ groupstr +") executeAggregationQuery : "+message.body().toString());
					log.info(aggregation.toString());
					finalHandler.handle(new JsonObject());
				}
			}
		});

		//Recurse
		if(group != null)
			for(IndicatorGroup child : group.getChildren()){
				executeAggregationQuery(child, finalHandler);
			}
	}

	/**
	 * Launch the aggregation process which consists of :
	 * <ul>
	 * 	<li>Filter the traces based on the default filtering and IndicatorFilters if the collection of filters is not empty.</li>
	 *  <li>Count the number of traces.</li>
	 *  <li>Write to the database this aggregated number.</li>
	 *  <li>For each IndicatorGroup, repeat the process recursively.</li>
	 * </ul>
	 * @param callBack : Handler called when processing is over.
	 */
	public void aggregate(final Handler<JsonObject> callBack){
		final Date start = new Date();

		//Filtering by trace type + custom filters.
		MongoDBBuilder filteringQuery = (MongoDBBuilder) new MongoDBBuilder().put(TRACE_FIELD_TYPE).is(indicatorKey);
		for(IndicatorFilter filter : filters){
			filter.filter(filteringQuery);
		}

		final AtomicInteger totalCalls = new AtomicInteger(1);
		for(IndicatorGroup group: groups){
			totalCalls.addAndGet(group.getTotalChildren());
		}

		final Handler<JsonObject> finalHandler = new Handler<JsonObject>(){
			public void handle(JsonObject event) {
				if(totalCalls.decrementAndGet() == 0){
					final Date end = new Date();
					log.info("[Aggregation]{"+writtenIndicatorKey+"} Took ["+(end.getTime() - start.getTime())+"] ms");
					callBack.handle(new JsonObject().put("status", "ok"));
				}
			}
		};

		//Count the total number of traces
		executeAggregationQuery(null, finalHandler);

		//Process for each registered group
		for(IndicatorGroup group : groups){
			executeAggregationQuery(group, finalHandler);
		}

	}

}
