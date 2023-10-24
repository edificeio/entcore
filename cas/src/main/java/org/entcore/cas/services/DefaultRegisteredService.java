/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.cas.services;

import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.ServiceTicket;
import fr.wseduc.cas.entities.User;
import org.entcore.cas.mapping.Mapping;
import org.entcore.cas.mapping.MappingService;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserInfos;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.aggregation.MongoConstants.TRACE_TYPE_CONNECTOR;

public class DefaultRegisteredService implements RegisteredService {
	protected final MappingService mappingService = MappingService.getInstance();
	protected final I18n i18n = I18n.getInstance();
	protected final List<Mapping> criterias = new ArrayList<>();
	private final Set<Mapping> confCriterias = new HashSet<>();
	protected EventBus eb;
	protected String principalAttributeName = "login";
	protected String directoryAction = "getUser";

	private EventStore eventStore = EventStoreFactory.getFactory().getEventStore(this.getClass().getSimpleName());

	protected static final Logger log = LoggerFactory.getLogger(DefaultRegisteredService.class);
	protected static final String CONF_PATTERNS = "patterns";
	protected static final String CONF_PRINCIPAL_ATTR_NAME = "principalAttributeName";

	@Override
	public void configure(final EventBus eb, final Map<String, Object> conf) {
		this.eb = eb;
		try {
			List<String> patterns = ((JsonArray) conf.get(CONF_PATTERNS)).getList();
			if (patterns != null && !patterns.isEmpty()) {
				addConfPatterns(patterns.toArray(new String[patterns.size()]));
			}
			this.principalAttributeName = String.valueOf(conf.get(CONF_PRINCIPAL_ATTR_NAME));
		}
		catch (Exception e) {
			log.error("Failed to parse configuration", e);
		}
	}

	@Override
	public boolean matches(final AuthCas authCas,final String serviceUri) {
		final boolean splitByStructure = mappingService.isSplitByStructure();
		for (Mapping criteria : criterias) {
			if (criteria.matches(authCas.getStructureIds(), serviceUri, splitByStructure)) {
				if (log.isDebugEnabled()) log.debug("service URI + |" + serviceUri + "| matches with pattern : " + criteria.pattern());
				return true;
			}
		}
		return false;
	}

	@Override
	public Optional<Mapping> findMatch(final AuthCas authCas, String serviceUri, boolean splitByStructure){
		for (Mapping criteria : criterias) {
			if (criteria.matches(authCas.getStructureIds(), serviceUri, splitByStructure)) {
				if (log.isDebugEnabled()) log.debug("service URI + |" + serviceUri + "| matches with pattern : " + criteria.pattern());
				return Optional.of(criteria);
			}
		}
		return Optional.empty();
	}

	@Override
	public void getUser(final AuthCas authCas, final String service, final Handler<User> userHandler) {
		final String userId = authCas.getUser();
		JsonObject jo = new JsonObject();
		jo.put("action", directoryAction).put("userId", userId);
		eb.request("directory", jo, handlerToAsyncHandler(new io.vertx.core.Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject res = event.body().getJsonObject("result");
				log.debug("res : " + res);
				if ("ok".equals(event.body().getString("status")) && res != null) {
					User user = new User();
					prepareUser(user, userId, service, res);
					userHandler.handle(user);
					createStatsEvent(authCas, res, service);
				} else {
					userHandler.handle(null);
				}
			}
		}));
	}

	void createStatsEvent(AuthCas authCas, JsonObject res, String service) {
		final String userId = authCas.getUser();
		final Optional<Mapping> mapping = foundMappingByService(authCas.getStructureIds(), service);
		UserInfos user = new UserInfos();
		user.setUserId(userId);
		JsonArray profiles = res.getJsonArray("profiles");
		if (profiles != null && profiles.size() > 0) {
			user.setType(profiles.getString(0));
		}
		JsonArray structureNodes = res.getJsonArray("structureNodes");
		if (structureNodes != null && structureNodes.size() > 0) {
			user.setStructures(structureNodes.stream().map(s -> ((JsonObject)s)
					.getString("id")).collect(Collectors.toList()));
		}
		final JsonObject event = new JsonObject().put("service", service).put("connector-type", "Cas");
		event.put("cas-type", mapping.map(e->e.getType()).orElse("unknown"));
		eventStore.createAndStoreEvent(TRACE_TYPE_CONNECTOR, user, event);
	}

	@Override
	public String formatService(String serviceUri, ServiceTicket st) {
		return serviceUri;
	}

	protected Future<Mapping> getMapping(Optional<String> structureId, String pattern, boolean canInherits, Optional<String> statCasType){
		final Promise<Mapping> future = Promise.promise();
		mappingService.getMappings().onComplete(r->{
			if(r.succeeded()){
				final Optional<Mapping> found = r.result().find(structureId, getId(), pattern, canInherits, statCasType);
				if(found.isPresent()){
					future.complete(found.get());
				} else{
					future.complete(Mapping.unknown(getId(), pattern));
					log.error("Could not found any type matching casType="+getId()+ " and pattern="+pattern);
				}
			}else{
				future.complete(Mapping.unknown(getId(), pattern));
				log.error("An error occured. Could not any found matching for casType="+getId()+ " and pattern="+pattern, r.cause());
			}
		});
		return future.future();
	}

	public Optional<Mapping> foundMappingByService(final Set<String> structureIds, final String serviceUri){
		final boolean splitByStructure = mappingService.isSplitByStructure();
		for(final Mapping mapping : criterias){
			if(mapping.matches(structureIds, serviceUri, splitByStructure)){
				return Optional.of(mapping);
			}
		}
		return Optional.empty();
	}

	private void addConfPatterns(String... patterns) {
		for (String pattern : patterns) {
			try {
				final Mapping mapping = Mapping.unknown(getId(), pattern).setAllStructures(true);
				this.confCriterias.add(mapping);
				this.criterias.add(mapping);
				getMapping(Optional.empty(),pattern, true, Optional.empty()).onComplete(r->{
					if(r.succeeded()){//set type as soon as we know it
						mapping.setType(r.result().getType());
					} else{
						log.error("Bad service configuration : failed to get mapping : " + pattern, r.cause());
					}
				});
			}
			catch (PatternSyntaxException pe) {
				log.error("Bad service configuration : failed to compile regex : " + pattern);
			}
		}
	}

	@Override
	public void addPatterns(boolean emptyPattern, String structureId, boolean canInherits, Optional<String> statCasType, String... patterns) {
		for (String pattern : patterns) {
			try {
				final String typedPattern = emptyPattern?"":pattern;
				getMapping(Optional.ofNullable(structureId), typedPattern, canInherits, statCasType).onComplete(r->{
					if(r.succeeded()){
						Mapping mapping = r.result();
						if(emptyPattern){
							mapping.addExtraPatter(pattern);
						}
						this.criterias.add(mapping);
					} else{
						log.error("Bad service configuration : failed to get mapping : " + pattern, r.cause());
					}
				});
			}
			catch (PatternSyntaxException pe) {
				log.error("Bad service configuration : failed to compile regex : " + pattern);
			}
		}
	}

	@Override
	public void cleanPatterns() {
		this.criterias.clear();
		this.criterias.addAll(this.confCriterias);
	}

	@Override
	public JsonObject getInfos(String acceptLanguage) {
		String baseKey = getId();
		return new JsonObject()
				.put("id", baseKey)
				.put("name", i18n.translate(baseKey + ".name", I18n.DEFAULT_DOMAIN, acceptLanguage))
				.put("description", i18n.translate(baseKey + ".description", I18n.DEFAULT_DOMAIN, acceptLanguage));
	}

	@Override
	public String getId() {
		return this.getClass().getSimpleName();
	}

	protected void prepareUser(final User user, final String userId, String service, final JsonObject data) {
		if (principalAttributeName != null) {
			user.setUser(data.getString(principalAttributeName));
			data.remove(principalAttributeName);
		}
		else {
			user.setUser(userId);
		}
		data.remove("password");

		Map<String, String> attributes = new HashMap<>();
		for (String attr : data.fieldNames()) {
			attributes.put(attr, data.getValue(attr).toString());
		}
		user.setAttributes(attributes);
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode() + criterias.hashCode() + principalAttributeName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof DefaultRegisteredService &&
				this.criterias.equals(((DefaultRegisteredService) obj).criterias) &&
				this.principalAttributeName.equals(((DefaultRegisteredService) obj).principalAttributeName);
	}

}
