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
