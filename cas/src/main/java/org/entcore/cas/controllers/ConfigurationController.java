/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.cas.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Delete;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;

import java.util.*;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.entcore.cas.mapping.Mapping;
import org.entcore.cas.mapping.MappingService;
import org.entcore.cas.services.RegisteredService;
import org.entcore.cas.services.RegisteredServices;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.filter.AdmlOfStructure;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.utils.StringUtils;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;


public class ConfigurationController extends BaseController {

	private RegisteredServices services;
	private final Neo4j neo = Neo4j.getInstance();
	private final MappingService mappingService = MappingService.getInstance();

	@Get("/configuration/reload")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void reloadPatterns(HttpServerRequest request) {
		loadPatterns();
		Renders.renderJson(request, new JsonObject().put("result", "done"), 200);
	}

	@Get("/configuration/mappings")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void getMappings(HttpServerRequest request) {
		mappingService.getMappings().onComplete(res->{
			if(res.succeeded()){
				Renders.renderJson(request, res.result().toJson());
			}else{
				Renders.renderError(request, new JsonObject().put("error", "cas.mappings.cantload"));
				log.error("Failed to load mapping : ", res.cause());
			}
		});
	}

	@Get("/configuration/mappings/reload")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void reloadMapping(HttpServerRequest request) {
		log.info("Reloading cas mapping");
		mappingService.reset();
		getMappings(request);
	}

	@Post("/configuration/mappings")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void createMapping(HttpServerRequest request) {
		RequestUtils.bodyToJson(request, r->{
			mappingService.create(r).onComplete(res->{
				if(res.succeeded()){
					reloadMapping(request);
				}else{
					Renders.renderError(request, new JsonObject().put("error", "cas.mappings.cantcreate"));
					log.error("Failed to create mapping : ", res.cause());
				}
			});
		});
	}

	@Delete("/configuration/mappings/:mappingId")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void deleteMapping(HttpServerRequest request) {
		mappingService.delete(request.params().get("mappingId")).onComplete(res->{
			if(res.succeeded()){
				reloadMapping(request);
			}else{
				Renders.renderError(request, new JsonObject().put("error", "cas.mappings.cantdelete"));
				log.error("Failed to delete mapping : ", res.cause());
			}
		});
	}

	@Get("/configuration/mappings/:mappingId/usage")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void getMappingUsage(HttpServerRequest request) {
		mappingService.getMappingUsage(request.params().get("mappingId"), Optional.empty()).onComplete(res->{
			if(res.succeeded()){
				Renders.renderJson(request, res.result());
			}else{
				Renders.renderError(request, new JsonObject().put("error","cas.mappings.cantload"));
				log.error("Failed to load mapping : ", res.cause());
			}
		});
	}

	@Get("/configuration/mappings/:mappingId/usage/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdmlOfStructure.class)
	@MfaProtected()
	public void getMappingUsageForStruct(HttpServerRequest request) {
		mappingService.getMappingUsage(request.params().get("mappingId"), Optional.of(request.params().get("structureId"))).onComplete(res->{
			if(res.succeeded()){
				Renders.renderJson(request, res.result());
			}else{
				Renders.renderError(request, new JsonObject().put("error", "cas.mappings.cantload"));
				log.error("Failed to load mapping : ", res.cause());
			}
		});
	}

	private static boolean safeGetBoolean(final JsonObject json, final String path, final boolean defaut){
		if(json.containsKey(path) && json.getValue(path) == null){
			return defaut;
		}
		return json.getBoolean(path, defaut);
	}

	public void loadPatterns() {
		log.info("Reloading cas pattern and mapping");
		mappingService.reset();
		eb.request("wse.app.registry.bus", new JsonObject().put("action", "list-cas-connectors"),
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					services.cleanPatterns();
					JsonArray externalApps = event.body().getJsonArray("result");
					for (Object o: externalApps) {
						try {
							if (!(o instanceof JsonObject)) continue;
							final JsonObject j = (JsonObject) o;
							final String service = j.getString("service");
							final String statCasType = j.getString("statCasType");
							final String structureId = j.getString("structureId");
							final JsonArray patterns = j.getJsonArray("patterns");
							final boolean inherits = safeGetBoolean(j,"inherits", false);
							final boolean emptyPattern = safeGetBoolean(j,"emptyPattern", false);
							final Optional<String> statCasTypeOpt = StringUtils.isEmpty(statCasType)?Optional.empty():Optional.ofNullable(statCasType);
							if (service != null && !service.trim().isEmpty() && patterns != null && patterns.size() > 0) {
								services.addPatterns(emptyPattern, service, structureId, inherits, statCasTypeOpt, Arrays.copyOf(patterns.getList().toArray(), patterns.size(), String[].class));
							}
						} catch(Exception e) {
							log.error("Could not add CAS app", e);
						}
					}
					log.info("Cas pattern has been reloaded successfully: "+externalApps.size());
				} else {
					log.error(event.body().getString("message"));
				}
			}
		}));
	}

	@BusAddress(value = "cas.configuration", local = false)
	public void cas(Message<JsonObject> message) {
		switch (message.body().getString("action", "")) {
			case "list-services" :
				message.reply(new JsonObject().put("status", "ok")
						.put("result", services.getInfos(message.body().getString("accept-language", "fr"))));
				break;
			case "add-patterns" :
				final String structureId = message.body().getString("structureId");
				final String service = message.body().getString("service");
				final String statCasType = message.body().getString("statCasType");
				final JsonArray patterns = message.body().getJsonArray("patterns");
				final boolean inherits = safeGetBoolean(message.body(),"inherits", false);
				final boolean emptyPattern = safeGetBoolean(message.body(),"emptyPattern", false);
				final Optional<String> statCasTypeOpt = StringUtils.isEmpty(statCasType)?Optional.empty():Optional.ofNullable(statCasType);
				message.reply(new JsonObject().put("status",
						services.addPatterns(emptyPattern, service, structureId, inherits, statCasTypeOpt, Arrays.copyOf(patterns.getList().toArray(), patterns.size(), String[].class)) ? "ok" : "error"));
				break;
			default:
				message.reply(new JsonObject().put("status", "error").put("message", "invalid.action"));
		}
	}

	public void setRegisteredServices(RegisteredServices services) {
		this.services = services;
	}

	@Get("/configuration/simulate")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void simulate(HttpServerRequest request) {
		final String service = request.params().get("service");
		final String user = request.params().get("user");
		final String splitByStructureStr = request.params().get("splitByStructure");
		final List<String> structures = request.params().getAll("structures");
		final boolean splitByStructure;
		final Promise<List<String>> futureStructures = Promise.promise();
		//compute split flag if not exists
		if(splitByStructureStr == null){
			splitByStructure = mappingService.isSplitByStructure();
		}else{
			splitByStructure = "true".equals(splitByStructureStr);
		}
		//if missing structure param compute it
		if(!StringUtils.isEmpty(user) && (structures == null || structures.isEmpty())){
			final String query = "MATCH (u:`User` { id : {id}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
					"RETURN s.id as id ";
			neo.execute(query, new JsonObject().put("id", user), Neo4jResult.validResultHandler(r->{
				if(r.isLeft()){
					futureStructures.fail(r.left().getValue());
				}else{
					final List<String> structureIds = new ArrayList<>();
					for(final Object obj : r.right().getValue()){
						if(obj instanceof JsonObject){
							final String id = ((JsonObject)obj).getString("id");
							if(id!=null){
								structureIds.add(id);
							}
						}
					}
					futureStructures.complete(structureIds);
				}
			}));
		}else{
			futureStructures.complete(structures != null? structures : new ArrayList<>());
		}
		futureStructures.future().onComplete(r->{
			if(r.failed()){
				renderJson(request, new JsonObject().put("error", r.cause().getMessage()));
				return;
			}
			final AuthCas authCas = new AuthCas();
			authCas.setUser(user);
			authCas.setStructureIds(new HashSet<>(r.result()));
			final Optional<Mapping> result = services.findMatch(authCas, service, splitByStructure);
			final Date structDate = mappingService.getCacheStructuresDate();
			final Date mappingDate = mappingService.getCacheMappingDate();
			final String structDateStr = structDate!=null?structDate.toString():"";
			final String mappingDateStr = mappingDate!=null?mappingDate.toString():"";
			final JsonObject response = new JsonObject().put("cacheStructureDate", structDateStr).put("cacheMappingDate", mappingDateStr);
			response.put("split-by-structure", splitByStructure);
			if(result.isPresent()){
				response.put("found", true);
				response.put("mapping-type", result.get().getType());
				response.put("pattern", result.get().getPattern());
				response.put("cas-type", result.get().getCasType());
				response.put("structures-ids", new JsonArray(new ArrayList<>(result.get().getStructureIds())));
				response.put("generated-pattern", new JsonArray(new ArrayList<>(result.get().getExtraPatterns())));
			}else{
				response.put("found", false);
			}
			renderJson(request, response);
		});
	}

}
