/*
 * Copyright © "Open Digital Education", 2016
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
