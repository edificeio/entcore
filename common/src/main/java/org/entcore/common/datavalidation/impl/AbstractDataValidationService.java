/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.common.datavalidation.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.AES128CBC;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.entcore.common.datavalidation.DataValidationService;
import org.entcore.common.datavalidation.utils.DataStateUtils;
import org.entcore.common.http.renders.TemplatedEmailRenders;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.utils.StringUtils;

import static org.entcore.common.datavalidation.utils.DataStateUtils.*;
import static org.entcore.common.neo4j.Neo4jResult.validEmpty;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResult;

public abstract class AbstractDataValidationService extends TemplatedEmailRenders implements DataValidationService {
	protected final Neo4j neo = Neo4j.getInstance();
	protected final String field;
	protected final String stateField;
	protected final String encryptKey;

	protected AbstractDataValidationService(final String field, final String stateField, io.vertx.core.Vertx vertx
		, io.vertx.core.json.JsonObject config, io.vertx.core.json.JsonObject params) {
		super(vertx, config);
		this.field = field;
		this.stateField = stateField;
		this.encryptKey = params.getString("encryptKey");
	}

	/** 
	 * @return {
	 * 	field: String|null, stateField: JsonObject|null,
	 *  firstName:string, lastName:string, displayName:string
	 * }
	 */
	protected Future<JsonObject> retrieveFullState(String userId) {
		final Promise<JsonObject> promise = Promise.promise();
		String query =
				"MATCH (u:`User` { id : {id}}) " +
				"RETURN COALESCE(u."+field+", null) as "+field+", COALESCE(u."+stateField+", null) as "+stateField+", " + 
					   "u.firstName as firstName, u.lastName as lastName, u.displayName as displayName ";
		JsonObject params = new JsonObject().put("id", userId);
		neo.execute(query, params, m -> {
			Either<String, JsonObject> r = validUniqueResult(m);
			if (r.isRight()) {
				final JsonObject result = r.right().getValue();
				result.put(stateField, fromRaw(result.getString(stateField)));
				promise.complete( result );
			} else {
				promise.fail(r.left().getValue());
			}
		});
		return promise.future();
	}

	/**
	 * @return updated state
	 */
	protected Future<JsonObject> updateState(String userId, final JsonObject state) {
		final Promise<JsonObject> promise = Promise.promise();
		StringBuilder query = new StringBuilder(
			"MATCH (u:`User` { id : {id}}) " +
			"SET u."+stateField+" = {state} "
		);
		JsonObject params = new JsonObject()
			.put("id", userId)
			.put("state", toRaw(state));
		if( DataStateUtils.getState(state) == DataStateUtils.VALID 
				&& !StringUtils.isEmpty(DataStateUtils.getValid(state)) ) {
			// We are going to update a user's session data => TODO propagate it
			query.append(", u."+field+" = {value} ");
			params.put("value", DataStateUtils.getValid(state));

			if( "email".equals(field) ) {
				query.append(", u.emailSearchField=LOWER({value}) ");
			}
		}
		neo.execute(query.toString(), params, m -> {
			Either<String, JsonObject> r = validEmpty(m);
			if (r.isRight()) {
				promise.complete(state);
			} else {
				promise.fail(r.left().getValue());
			}
		});
		return promise.future();
	}

    /**
     * Since Neo4j does not allow JSON objects to be node properties, stateField is stored as a JSON string
	 * => serialize it
     * @param state as JSON object
     * @return state as JSON string
     */
    protected String toRaw(final JsonObject state) {
        if( state==null ) return null;

		// WB-1700 This field should be obfuscated in DB, if encryption key is available.
		String code = getKey(state);
		try {
			// Cipher the key put in raw format...
			setKey( state, AES128CBC.encrypt(code, encryptKey) );
			String retValue = state.encode();
			// ... but do not alter the original state JsonObject
			setKey( state, code );
			return retValue;
		} catch( Exception e ) {
			// As a fallback, keep the code as-is and let the workflow continue.
			log.warn( "Unable to encrypt the data validation key in DB", e);
			return state.encode();
		}
    }

    /**
     * Since Neo4j does not allow JSON objects to be node properties, stateField is stored as a JSON string
	 * => deserialize it
     * @param state as JSON string
     * @return state as JSON object
     */
    protected JsonObject fromRaw(final String state) {
        if( state==null ) return null;
		final JsonObject json = new JsonObject(state);

		// WB-1700 This field was obfuscated in DB.
		String code = getKey(json);
		try {
			if( !StringUtils.isEmpty(code) ) {
				setKey( json, AES128CBC.decrypt(code, encryptKey) );
			}
		} catch( Exception e ) {
			// Keep the code as-is, it may be in clear if generated before migration.
			log.warn( "Unable to decrypt the data validation key "+code, e);
		}

		return json;
    }

	/** Generate a pseudo-random code of 6 digits length. */
	protected String generateRandomCode() {
		return String.format("%06d", Math.round(Math.random()*999999D));
	}

	protected JsonObject formatAsResponse(final int state, final String valid, final Integer tries, final Long ttl) {
		final JsonObject o = new JsonObject()
		.put("state", stateToString(state))
		.put("valid", (valid!=null) ? valid : "");
		if( state==PENDING || state==OUTDATED ) {
			if(tries!=null) o.put("tries", tries);
			if(ttl!=null) o.put("ttl", ttlToRemainingSeconds(ttl.longValue()));
		}
		return o;
	}

	@Override
	public Future<JsonObject> startUpdate(String userId, String value, final long validDurationS, final int triesLimit) {
		return retrieveFullState(userId)
		.compose( j -> {
			final JsonObject originalState = j.getJsonObject(stateField, new JsonObject());
			String key = generateRandomCode();

			// Check if data validation was already started
			if( getState(originalState) == PENDING ) {
				final Long ttl = getTtl(originalState);
				if( ttl!=null && ttlToRemainingSeconds(ttl.longValue()) > 0 ) {	// Is it still pending ?
					String oldCode = getKey(originalState);							// Yes => keep the same code
					if( !StringUtils.isEmpty(oldCode) ) {
						key = oldCode;
					}
				}
			}

			// Reset the stateField to a pending state
			setState(originalState, PENDING);
			// Valid data must stay unchanged if not null, otherwise initialize to an empty string.
			if( getValid(originalState) == null ) {
				setValid(originalState, "");
			}
			setPending(originalState, value);
			setKey(originalState, key);
			setTtl(originalState, System.currentTimeMillis() + validDurationS * 1000l);
			setTries(originalState, triesLimit);

			return updateState(userId, originalState);
		});
	}

	@Override
	public Future<JsonObject> hasValid(String userId) {
		return retrieveFullState(userId)
		.map( j -> {
			Integer state = null;
			String value = j.getString(field);
			JsonObject valueState = j.getJsonObject(stateField);

			if (StringUtils.isEmpty(value) || valueState == null) {
				state = UNCHECKED;
			} else if( !value.equalsIgnoreCase( getValid(valueState) )) {
				// Case where the field was first validated and then changed.
				state = getState(valueState);
				if( state == VALID ) {
					state = UNCHECKED;
				}
			} else {
				// If mobile===valid, then state must be valid
				state = VALID; // /!\ do not replace by   state = getState(valueState); /!\
			}
			return formatAsResponse(state, getValid(valueState), null, null);
		});
	}

	@Override
	public Future<JsonObject> tryValidate(String userId, String code) {
		return retrieveFullState(userId)
		.compose( j -> {
			JsonObject state = j.getJsonObject(stateField);

			// Check business rules
			do {
				if( state == null ) {
					// Unexpected data, should never happen
					state = new JsonObject();
					j.put(stateField, state);
					setState(state, OUTDATED);
					break;
				}
				// if TTL or max tries reached, then don't check code
				if (getState(state) == OUTDATED) {
					break;
				}
				// Check code
				String key = StringUtils.trimToNull( getKey(state) );
				if( key == null || !key.equals(StringUtils.trimToNull(code)) ) {
					// Invalid
					Integer tries = getTries(state);
					if(tries==null) {
						tries = 0;
					} else {
						tries = Math.max(0, tries.intValue() - 1 );
					}
					if( tries <= 0 ) {
						setState(state, OUTDATED);
					}
					setTries(state, tries);
					break;
				}
				// Check time to live
				Long ttl = getTtl(state);
				if( ttl==null || ttl.compareTo(System.currentTimeMillis()) < 0 ) {
					// TTL reached
					setState(state, OUTDATED);
					break;
				}
				// Check pending mail address
				String pending = StringUtils.trimToNull( getPending(state) );
				if( pending == null) {
					// This should never happen, but treat it like TTL was reached
					setState(state, OUTDATED);
					break;
				}
				// ---Validation succeeded---
				setState(state, VALID);
				setValid(state, pending);
				setPending(state, null);
				setKey(state, null);
				setTtl(state, null);
				setTries(state, null);
			} while(false);

			// ---Validation results---
			return updateState(userId, state)
			.map( newState -> {
				return formatAsResponse(getState(newState), getValid(newState), getTries(newState), getTtl(newState));
			});
		});
	}

	@Override
	public Future<JsonObject> getCurrentState(String userId) {
		return retrieveFullState(userId);
	}
}
