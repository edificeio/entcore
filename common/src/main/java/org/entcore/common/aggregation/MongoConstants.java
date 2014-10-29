package org.entcore.common.aggregation;

/**
 * List of MongoDB constants needed for aggregation processing.
 */
public class MongoConstants {
	
	//COLLECTION NAMES
	public static enum COLLECTIONS{
		events,
		stats
	}

	//TRACE COLLECTION FIELD NAMES
	public static final String TRACE_FIELD_TYPE 		= "event-type";
	public static final String TRACE_FIELD_MODULE 		= "module";
	public static final String TRACE_FIELD_DATE 		= "date";
	public static final String TRACE_FIELD_USER 		= "userId";
	public static final String TRACE_FIELD_PROFILE 		= "profil";
	public static final String TRACE_FIELD_STRUCTURES 	= "structures";
	public static final String TRACE_FIELD_CLASSES 		= "classes";
	public static final String TRACE_FIELD_GROUPS 		= "groups";
	public static final String TRACE_FIELD_REFERER 		= "referer";
	public static final String TRACE_FIELD_SESSIONID 	= "sessionId";
	
	//EVENTS COLLECTION TYPES
	public static final String TRACE_TYPE_ACTIVATION	= "ACTIVATION";
	public static final String TRACE_TYPE_CONNEXION 	= "LOGIN";
	public static final String TRACE_TYPE_SVC_ACCESS 	= "ACCESS";
	public static final String TRACE_TYPE_RSC_ACCESS 	= "GET_RESOURCE";
	public static final String TRACE_TYPE_APT_ACCESS 	= "ACCESS_ADAPTER";
	public static final String TRACE_TYPE_IMPORT 		= "IMPORT";
	public static final String TRACE_TYPE_DELETE_USER 	= "DELETE_USER";
	public static final String TRACE_TYPE_CREATE_USER 	= "CREATE_USER";
	
	//STATS COLLECTION FIELD NAMES
	public static final String STATS_FIELD_DATE 		= "date";
	public static final String STATS_FIELD_GROUPBY 		= "groupedBy";
}
