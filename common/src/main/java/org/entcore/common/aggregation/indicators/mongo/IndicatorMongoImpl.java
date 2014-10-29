package org.entcore.common.aggregation.indicators.mongo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import org.entcore.common.aggregation.AggregationTools.HandlerChainer;
import org.entcore.common.aggregation.filters.IndicatorFilter;
import org.entcore.common.aggregation.filters.dbbuilders.MongoDBBuilder;
import org.entcore.common.aggregation.filters.mongo.IndicatorFilterMongoImpl;
import org.entcore.common.aggregation.groups.IndicatorGroup;
import org.entcore.common.aggregation.indicators.Indicator;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;

import org.entcore.common.aggregation.AggregationTools;

import static org.entcore.common.aggregation.MongoConstants.*;

/**
 * MongoDB implementation of the Indicator class.
 */
public class IndicatorMongoImpl extends Indicator{
	
	//MongoDB instance
	protected final MongoDb mongo;
	
	private String writtenIndicatorKey;
	
	//Memoization map, used to buffer distinct values from Mongo and avoiding unnecessary database queries.
	private final HashMap<String, JsonArray> memoizeDistincts = new  HashMap<>();
	
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
	 * Changes the key used when writing the aggregated amount in the mongo statistics collection.
	 * If left unchanged the indicator key will be used.
	 * @param key : The new key
	 */
	public void setWriteKey(String key){
		this.writtenIndicatorKey = key;
	}
	
	//Callback to the writeStats function
	private Handler<Integer> writeCallback(final Handler<Message<JsonObject>> callBack){
		return writeCallback(null, null, callBack);
	}
	private Handler<Integer> writeCallback(final IndicatorGroup group, final HashMap<String, String> groupIds, final Handler<Message<JsonObject>> callBack){
		return new Handler<Integer>(){
			public void handle(Integer countResult) {
				writeStats(countResult, group, groupIds, callBack);
			}
		};
	}
	
	//Write aggregated data to the database, using data from a Mongo count
	private void writeStats(int countResult, final IndicatorGroup group, final HashMap<String, String> groupIds, final Handler<Message<JsonObject>> callBack){
					
		//If no documents found, write nothing
		if(countResult == 0){
			callBack.handle(null);
			return;
		}
		
		//Document date, default to today's date at midnight
		Date today = AggregationTools.setToMidnight(Calendar.getInstance());
		
		MongoDBBuilder criteriaQuery = new MongoDBBuilder();
		
		if(group == null){
			//When not using groups, set groupedBy specifically to not exists
			criteriaQuery.put(STATS_FIELD_DATE).is(MongoDb.formatDate(today)).and(STATS_FIELD_GROUPBY).exists(false);
		} else {
			//Retrieve all the group chain keys and append them into groupedBy.
			LinkedList<String> groupedByKeys = new LinkedList<>();
			groupedByKeys.add(group.getKey());
			
			IndicatorGroup parent = group.getParent();
			while(parent != null){
				groupedByKeys.addFirst(parent.getKey()+"/");
				parent = parent.getParent();
			}
			StringBuilder groupedByString = new StringBuilder();
			for(String groupKey : groupedByKeys){
				groupedByString.append(groupKey);
			}
			criteriaQuery
				.put(STATS_FIELD_DATE).is(MongoDb.formatDate(today))
				.and(STATS_FIELD_GROUPBY).is(groupedByString.toString());
			
			//For the group and its ancestors, retrieve each group id and add it to the document.
			for(Entry<String, String> groupId : groupIds.entrySet()){
				criteriaQuery.and(groupId.getKey()+"_id").is(groupId.getValue());
			}
		}
		
		MongoUpdateBuilder objNewQuery = new MongoUpdateBuilder();
		//Increment the counter
		objNewQuery.inc(writtenIndicatorKey, countResult);
		
		//Write the document (increments if it already exists, else creates it) 
		mongo.update(COLLECTIONS.stats.name(), MongoQueryBuilder.build(criteriaQuery), objNewQuery.build(), true, true, callBack);
	
	}
	
	/* AGGREGATE */
	
	//Action to be performed for each distinct value of the group key in the traces
	private void distinctAction(JsonArray distinctValues, final IndicatorGroup group, final HashMap<String, String> groupIds, final Handler<Message<JsonObject>> callBack){
		
		HandlerChainer<String, Message<JsonObject>> distinctChainer = new HandlerChainer<String, Message<JsonObject>>(){
			protected void executeItem(final String groupValue, final Handler<Message<JsonObject>> nextCallback) {
				//For the group and for each parent group, adding the filter on the group key value in the trace.
				Collection<IndicatorFilter> groupFilters = new ArrayList<>();
				groupFilters.addAll(filters);
				for(final Entry<String, String> groupId : groupIds.entrySet()){
					groupFilters.add(new IndicatorFilterMongoImpl() {
						public void filter(MongoDBBuilder builder) {
							builder.and(groupId.getKey()).is(groupId.getValue());
						}
					});
				}
				groupFilters.add(new IndicatorFilterMongoImpl() {
					public void filter(MongoDBBuilder builder) {
						builder.and(group.getKey()).is(groupValue);
					}
				});
				
				//Filter by trace type + custom filters + group & parent groups ids
				final MongoDBBuilder filteringQuery = (MongoDBBuilder) new MongoDBBuilder().put(TRACE_FIELD_TYPE).is(indicatorKey);
				for(IndicatorFilter filter : groupFilters){
					filter.filter(filteringQuery);
				}
				
				//Cloning the map and adding this group id.
				final HashMap<String, String> newGroupIds = new HashMap<>();
				newGroupIds.putAll(groupIds);
				newGroupIds.put(group.getKey(), groupValue);
				
				//Aggregation process for each child group.
				Handler<Message<JsonObject>> childrenHandler = new Handler<Message<JsonObject>>(){
					public void handle(Message<JsonObject> event) {
						HandlerChainer<IndicatorGroup, Message<JsonObject>> chainer = new HandlerChainer<IndicatorGroup, Message<JsonObject>>(){
							protected void executeItem(IndicatorGroup child, Handler<Message<JsonObject>> next) {
								aggregateGroup(child, newGroupIds, next);
							}
						};
						
						for(IndicatorGroup child : group.getChildren()){
							chainer.chainItem(child);
						}
						chainer.executeChain(nextCallback);
					}
				};
				
				//Count the traces, then write the document & recurse for each child.
				countTraces(filteringQuery, writeCallback(group, newGroupIds, childrenHandler));
			}
		};
		
		for(final Object distinctValue : distinctValues){
			distinctChainer.chainItem((String) distinctValue);
		}
		
		distinctChainer.executeChain(callBack);
	}
	
	//Aggregation process for a single group, if the group has children this function is recursively called for each child.
	//The groupIds HashMap is an accumulator used to store the parent group(s) trace value(s) (id) which will be written in the Mongo document.
	private void aggregateGroup(final IndicatorGroup group, final HashMap<String, String> groupIds, final Handler<Message<JsonObject>> callBack){

		//If the memoization map already contains the group distinct key values.
		if(memoizeDistincts.containsKey(group.getKey())){
			JsonArray distinctValues = memoizeDistincts.get(group.getKey());
			distinctAction(distinctValues, group, groupIds, callBack);
		} else {
			//Filtering by trace type + custom filters.
			final MongoDBBuilder filteringQuery = (MongoDBBuilder) new MongoDBBuilder().put(TRACE_FIELD_TYPE).is(indicatorKey);
			for(IndicatorFilter filter : filters){
				filter.filter(filteringQuery);
			}
			//Counting distinct values of group keys in the traces.
			mongo.distinct(COLLECTIONS.events.name(), group.getKey(), MongoQueryBuilder.build(filteringQuery), new Handler<Message<JsonObject>>(){
				public void handle(Message<JsonObject> message) {
					if ("ok".equals(message.body().getString("status"))) {
						JsonArray distinctValues = message.body().getArray("values", new JsonArray());
						memoizeDistincts.put(group.getKey(), distinctValues);
						distinctAction(distinctValues, group, groupIds, callBack);
					} else {
						//TODO : better error handling
						callBack.handle(message);
					}
				}
			});
		}
		
	}
	
	/**
	 * Method called to aggregate traces.
	 * @param filteringQuery : the query which is used to filter traces.
	 * @param callBack : Handler called when the operation completes.
	 */
	protected void countTraces(MongoDBBuilder filteringQuery, final Handler<Integer> callBack){
		mongo.count(COLLECTIONS.events.name(), MongoQueryBuilder.build(filteringQuery), new Handler<Message<JsonObject>>(){
			public void handle(Message<JsonObject> message){
				if ("ok".equals(message.body().getString("status"))) {
					//Retrieve Mongo count result
					int countResult = message.body().getInteger("count");
					callBack.handle(countResult);
				} else {
					callBack.handle(0);
				}
			}
		});
	}
	
	/* MAIN AGGREGATION */
	
	/**
	 * Launch the aggregation process which consists of :
	 * <ul>
	 * 	<li>Filter the traces based on the default filtering and IndicatorFilters if the collection of filters is not empty.</li>
	 *  <li>Count the number of traces.</li>
	 *  <li>Write to the database this aggregated number.</li>
	 *  <li>For each IndicatorGroup, repeat the process recursively.</li>
	 * <ul>
	 * @param callBack : Handler called when processing is over.
	 */
	public void aggregate(final Handler<Message<JsonObject>> callBack){
		//Filtering by trace type + custom filters.
		MongoDBBuilder filteringQuery = (MongoDBBuilder) new MongoDBBuilder().put(TRACE_FIELD_TYPE).is(indicatorKey);
		for(IndicatorFilter filter : filters){
			filter.filter(filteringQuery);
		}
		
		//Aggregation process for each group.
		Handler<Message<JsonObject>> groupsHandler = new Handler<Message<JsonObject>>(){
			public void handle(Message<JsonObject> event) {
				HandlerChainer<IndicatorGroup, Message<JsonObject>> chainer = new HandlerChainer<IndicatorGroup, Message<JsonObject>>(){
					protected void executeItem(IndicatorGroup item, Handler<Message<JsonObject>> nextCallback) {
						aggregateGroup(item, new HashMap<String, String>(), nextCallback);
					}
				};
				
				for(IndicatorGroup group : groups){
					chainer.chainItem(group);
				}
				chainer.executeChain(callBack);
			}
		};
		
		//Counting the results & writing down the aggregation.
		countTraces(filteringQuery, writeCallback(groupsHandler));
		
		
	}
	
}
