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

package org.entcore.communication.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.communication.dto.bus.CommunicationBusDTO;
import org.entcore.communication.dto.rest.DiscoverModifyGroupUsersDTO;
import org.entcore.communication.dto.rest.DiscoverVisibleGroupBodyDTO;
import org.entcore.communication.dto.rest.DiscoverVisibleFilterDTO;
import org.entcore.communication.dto.rest.CountResultDTO;
import org.entcore.communication.dto.rest.IdentifiableDTO;
import org.entcore.communication.dto.rest.InitDefaultRulesDTO;
import org.entcore.communication.dto.bus.SearchVisibleBusDTO;
import org.entcore.communication.dto.rest.SearchVisibleRequestDTO;
import org.entcore.communication.filters.CommunicationDiscoverVisibleFilter;
import org.entcore.communication.mapper.AddLinkDirectionsDtoMapper;
import org.entcore.communication.mapper.CommuniqueWithDtoMapper;
import org.entcore.communication.mapper.DiscoverVisibleStructureDtoMapper;
import org.entcore.communication.mapper.GroupDtoMapper;
import org.entcore.communication.mapper.RemoveRelationsResultDtoMapper;
import org.entcore.communication.mapper.VerifyResultDtoMapper;
import org.entcore.communication.mapper.SearchVisibleContactDtoMapper;
import org.entcore.communication.mapper.SearchVisibleDtoMapper;
import org.entcore.communication.mapper.UserDtoMapper;
import org.entcore.communication.services.CommunicationService;
import org.entcore.communication.mapper.BusUserDtoMapper;
import org.entcore.communication.services.impl.DefaultCommunicationService;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class CommunicationController extends BaseController {

	private CommunicationService communicationService;

	@Get("/admin-console")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	@MfaProtected()
	public void adminConsole(final HttpServerRequest request) {
		renderView(request);
	}

	/**
	 * Adds a communication link between two groups, upgrading the users' communication values
	 * and propagating the link to all users belonging to the concerned groups.
	 *
	 * <p><strong>Deprecated — do not use.</strong> This endpoint was used by the v1 admin console
	 * and is known to corrupt data. It must not be called anymore.</p>
	 *
	 * @param request the HTTP request containing path parameters {@code startGroupId} and {@code endGroupId}
	 * @deprecated This method can corrupt data. Use the new communication API instead.
	 */
	@Post("/group/:startGroupId/communique/:endGroupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	@Deprecated
	public void addLink(HttpServerRequest request) {
		Params params = new Params(request).validate();
		if (params.isInvalid()) return;
		communicationService.addLink(params.getStartGroupId(), params.getEndGroupId(),
				notEmptyResponseHandler(request));
	}

	/**
	 * Adds a direct communication link between two users, bypassing group-based rules.
	 *
	 * <p>Restricted to super-admins. Intended for integration testing purposes only
	 * and should not be used in production workflows.</p>
	 *
	 * @param request the HTTP request containing path parameters {@code startUser} and {@code endUser},
	 *                and query parameter {@code direction} defining the direction of the communication link
	 */
	@Put("/api/admin/users/:startUser/communiqueDirect/:endUser")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void setDirectCommunication(HttpServerRequest request) {
		String startUser = request.getParam("startUser");
		String endUser = request.getParam("endUser");
		String direction = request.getParam("direction");

		if(StringUtils.isEmpty(startUser) || StringUtils.isEmpty(endUser) || StringUtils.isEmpty(direction)) {
			renderError(request);
			return;
		}
		CommunicationService.Direction directionEnum = getDirection(direction);
		communicationService.setDirectCommunication(startUser, endUser, directionEnum, defaultResponseHandler(request));
	}

	/**
	 * Removes a communication link between two groups, downgrading the users' communication values
	 * and propagating the removal to all users belonging to the concerned groups.
	 *
	 * <p><strong>Deprecated — do not use.</strong> This endpoint was used by the v1 admin console
	 * and is known to corrupt data. It must not be called anymore.</p>
	 *
	 * @param request the HTTP request containing path parameters {@code startGroupId} and {@code endGroupId}
	 * @deprecated This method can corrupt data. Use the new communication API instead.
	 */
	@Delete("/group/:startGroupId/communique/:endGroupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	@Deprecated
	public void removeLink(HttpServerRequest request) {
		Params params = new Params(request).validate();
		if (params.isInvalid()) return;
		communicationService.removeLink(params.getStartGroupId(), params.getEndGroupId(),
				notEmptyResponseHandler(request));
	}
	/**
	 * Adds a communication link between users and a group, upgrading the group's {@code users}
	 * value (NONE, INCOMING, OUTGOING, BOTH) and propagating the link to all users belonging
	 * to the concerned group.
	 *
	 * <p><strong>Deprecated — do not use.</strong> This endpoint was used by the v1 admin console
	 * and is known to corrupt data. It must not be called anymore.</p>
	 *
	 * @param request the HTTP request containing path parameter {@code groupId} and query parameter
	 *                {@code direction} defining the direction of the communication link
	 * @deprecated This method can corrupt data. Use the new communication API instead.
	 */
	@Post("/group/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	@Deprecated
	public void addLinksWithUsers(HttpServerRequest request) {
		String groupId = getGroupId(request);
		if (groupId == null) return;
		CommunicationService.Direction direction = getDirection(request.params().get("direction"));
		communicationService.addLinkWithUsers(groupId, direction, notEmptyResponseHandler(request));
	}

	/**
	 * Removes a communication link between users and a group, downgrading the group's {@code users}
	 * value (NONE, INCOMING, OUTGOING, BOTH) and propagating the removal to all users belonging
	 * to the concerned group.
	 *
	 * <p><strong>Deprecated — do not use.</strong> This endpoint was used by the v1 admin console
	 * and is known to corrupt data. It must not be called anymore.</p>
	 *
	 * @param request the HTTP request containing path parameter {@code groupId} and query parameter
	 *                {@code direction} defining the direction of the communication link to remove
	 * @deprecated This method can corrupt data. Use the new communication API instead.
	 */
	@Delete("/group/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	@Deprecated
	public void removeLinksWithUsers(HttpServerRequest request) {
		String groupId = getGroupId(request);
		if (groupId == null) return;
		CommunicationService.Direction direction = getDirection(request.params().get("direction"));
		communicationService.removeLinkWithUsers(groupId, direction, notEmptyResponseHandler(request));
	}

	/**
	 * Returns the group itself along with the list of groups referenced in its {@code communiqueWith}
	 * Neo4j property, i.e. the groups the source group can communicate with via a {@code COMMUNIQUE}
	 * relationship.
	 *
	 * @param request the HTTP request containing path parameter {@code groupId}
	 */
	@Get("/group/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void communiqueWith(HttpServerRequest request) {
		String groupId = getGroupId(request);
		if (groupId == null) return;
		communicationService.communiqueWith(groupId)
				.onSuccess(result -> render(request, CommuniqueWithDtoMapper.map(result)))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Returns the groups that the given group has outgoing {@code COMMUNIQUE} relations to,
	 * i.e. groups whose {@code users} value is {@code OUTGOING} or {@code BOTH} and that can
	 * therefore receive communication and broadcast it to their members.
	 *
	 * <p>Response: array of {@link org.entcore.communication.dto.rest.GroupDTO}.</p>
	 *
	 * @param request the HTTP request containing path parameter {@code groupId}
	 */
	@Get("/group/:groupId/outgoing")
	public void getOutgoingRelations(HttpServerRequest request) {
		String groupId = request.params().get("groupId");
		communicationService.getOutgoingRelations(groupId)
				.onSuccess( groups -> render(request, groups.stream().map(JsonObject.class::cast).map(GroupDtoMapper::map).collect(Collectors.toList())))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Returns the groups that have incoming {@code COMMUNIQUE} relations toward the given group,
	 * i.e. groups whose {@code users} value is {@code INCOMING} or {@code BOTH} and that can
	 * therefore emit communication from their members.
	 *
	 * <p>Response: array of {@link org.entcore.communication.dto.rest.GroupDTO}.</p>
	 *
	 * @param request the HTTP request containing path parameter {@code groupId}
	 */
	@Get("/group/:groupId/incoming")
	public void getIncomingRelations(HttpServerRequest request) {
		String groupId = request.params().get("groupId");
		communicationService.getIncomingRelations(groupId)
				.onSuccess( groups -> render(request, groups.stream().map(JsonObject.class::cast).map(GroupDtoMapper::map).collect(Collectors.toList())))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Creates or updates the {@code COMMUNIQUE_DIRECT} relationship between students and their
	 * relatives for all relative in the given group. Update the relativeCommuniqueStudent group value
	 *
	 * <p>The {@code direction} query parameter controls the direction of the link:</p>
	 * <ul>
	 *   <li>{@code INCOMING} — students can communicate toward their relatives</li>
	 *   <li>{@code OUTGOING} — relatives can communicate toward their students</li>
	 *   <li>{@code BOTH} — communication is allowed in both directions</li>
	 * </ul>
	 *
	 * <p>Response: {@link org.entcore.communication.dto.rest.CountResultDTO} with the number of
	 * affected relationships.</p>
	 *
	 * @param request the HTTP request containing path parameter {@code groupId} and query parameter
	 *                {@code direction}
	 */
	@Post("/relative/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void addLinkBetweenRelativeAndStudent(HttpServerRequest request) {
		String groupId = getGroupId(request);
		if (groupId == null) {
			renderError(request);
			return;
		}
		CommunicationService.Direction direction = getDirection(request.params().get("direction"));
		communicationService.addLinkBetweenRelativeAndStudent(groupId, direction)
				.onSuccess( count -> render(request, new CountResultDTO(count)))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Removes the {@code COMMUNIQUE_DIRECT} relationship between students and their relatives
	 * for all student/relative pairs in the given group.
	 *
	 * <p>The {@code direction} query parameter controls which link is removed:</p>
	 * <ul>
	 *   <li>{@code INCOMING} — removes the link from students toward their relatives</li>
	 *   <li>{@code OUTGOING} — removes the link from relatives toward their students</li>
	 *   <li>{@code BOTH} — removes links in both directions</li>
	 * </ul>
	 *
	 * <p>Response: {@link org.entcore.communication.dto.rest.CountResultDTO} with the number of
	 * affected relationships.</p>
	 *
	 * @param request the HTTP request containing path parameter {@code groupId} and query parameter
	 *                {@code direction}
	 */
	@Delete("/relative/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void removeLinkBetweenRelativeAndStudent(HttpServerRequest request) {
		String groupId = getGroupId(request);
		if (groupId == null) {
			renderError(request);
			return;
		}
		CommunicationService.Direction direction = getDirection(request.params().get("direction"));
		communicationService.removeLinkBetweenRelativeAndStudent(groupId, direction)
				.onSuccess( count -> render(request, new CountResultDTO(count)))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Retrieves all users and groups visible to the given user.
	 * <p>
	 * Route: {@code GET /visible/:userId}
	 * <p>
	 * Query parameters:
	 * <ul>
	 *   <li>{@code schoolId} — optional school context to filter results</li>
	 *   <li>{@code expectedType} — optional, repeatable; restricts results to specific profile types</li>
	 * </ul>
	 *
	 * @param request the HTTP request; path param {@code userId} identifies the user whose visible contacts are fetched
	 * @deprecated This endpoint no longer appears to be called. Consider removing it or replacing it with the POST {@code /visible} endpoint.
	 */
	@Get("/visible/:userId")
	@SecuredAction("communication.visible.user")
	public void visibleUsers(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		if (userId != null && !userId.trim().isEmpty()) {
			String schoolId = request.params().get("schoolId");
			List<String> expectedTypes = request.params().getAll("expectedType");
			visibleUsers(userId, schoolId, new JsonArray(expectedTypes), arrayResponseHandler(request));
		} else {
			renderJson(request, new JsonArray());
		}
	}

	/**
	 * Searches for all users and groups visible to the authenticated user, applying the filters provided in the request body.
	 * <p>
	 * Route: {@code POST /visible}
	 * <p>
	 * The request body is deserialized into a {@link SearchVisibleRequestDTO}. The following fields are always
	 * overridden server-side regardless of client input:
	 * <ul>
	 *   <li>{@code itSelf=true} — the caller is always included</li>
	 *   <li>{@code myGroup=true} — the caller's own groups are always included</li>
	 *   <li>{@code profile=false} — profile-only entries are excluded</li>
	 * </ul>
	 * Available filters in the body: {@code structures}, {@code classes}, {@code profiles}, {@code functions},
	 * {@code positions}, {@code search}, {@code types}, {@code nbUsersInGroups}, {@code groupType}.
	 * <p>
	 * Returns a {@link org.entcore.communication.dto.rest.SearchVisibleResultDTO} containing:
	 * <ul>
	 *   <li>{@code users} — list of {@link org.entcore.communication.dto.rest.UserDTO} visible to the caller</li>
	 *   <li>{@code groups} — list of {@link org.entcore.communication.dto.rest.GroupDTO} visible to the caller,
	 *       optionally annotated with their group type when {@code groupType=true}</li>
	 * </ul>
	 *
	 * @param request the HTTP request carrying the JSON body with search filters
	 */
	@Post("/visible")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void searchVisible(HttpServerRequest request) {
		RequestUtils.bodyToClass(request, SearchVisibleRequestDTO.class).onSuccess(filter -> UserUtils.getUserInfos(eb, request, user -> {
			// override some value for this canal
			filter.setItSelf(true)
				.setMyGroup(true)
				.setProfile(false);
			boolean returnGroupType = Boolean.TRUE.equals(filter.getGroupType());
			communicationService.visibleUsers(user, filter)
					.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())))
					.onSuccess( visibles -> {
						render(request, SearchVisibleDtoMapper.map(UserUtils.translateAndGroupVisible(visibles, I18n.acceptLanguage(request), returnGroupType)));
					});
		}));
	}

	@Get("/visible/group/:groupId")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void visibleGroupContains(HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				String groupId = request.params().get("groupId");
				if (groupId == null || groupId.trim().isEmpty()) {
					badRequest(request, "invalid.groupId");
					return;
				}
				//FIXME we must use a dedicated query to prefilter on users of a group
				String customReturn =
						"WITH COLLECT(visibles.id) as vIds " +
								"UNWIND vIds AS vId "+
								"MATCH (u:User { id: vId }) "+
								"USING INDEX u:User(id) " +
								"MATCH (s:Group { id : {groupId}})<-[:IN]-(u:User) " +
								"USING INDEX s:Group(id) "+
								"RETURN DISTINCT HEAD(u.profiles) as type, u.id as id, " +
								"u.displayName as displayName, u.login as login " +
								"ORDER BY type DESC, displayName ";
				final JsonObject params = new JsonObject().put("groupId", groupId);
				communicationService.visibleUsers(user.getUserId(), null, null, true, true, false, null,
						customReturn, params, visibles -> {
							if(visibles.isLeft()) {
								renderError(request, new JsonObject().put("error", visibles.left()));
								return;
							}
							render(request, visibles.right().getValue().stream()
									.map(JsonObject.class::cast)
									.map(UserDtoMapper::map)
									.collect(Collectors.toList()));
						});
			} else {
				badRequest(request, "invalid.user");
			}
		});
	}

	@BusAddress("wse.communication.users")
	public void visibleUsers(final Message<JsonObject> message) {
		SearchVisibleBusDTO searchQuery = message.body().mapTo(SearchVisibleBusDTO.class);
		String userId = searchQuery.getUserId();
		if (userId == null || userId.trim().isEmpty()) {
			message.reply(new JsonArray());
			return;
		}

		String schoolId = searchQuery.getSchoolId();
		JsonArray expectedTypes = new JsonArray(searchQuery.getExpectedTypes());
		final Future<JsonArray> future;

		switch (searchQuery.getAction()) {
			case "visibleUsers":
				boolean myGroup = communicationService instanceof DefaultCommunicationService || searchQuery.isMyGroup();
				//FIXME: we can't define a DTO  until we remove customeReturn from query
				future = communicationService.visibleUsers(userId, schoolId, expectedTypes,
						searchQuery.isItSelf(), myGroup, searchQuery.isProfile(),
						searchQuery.getPreFilter(), searchQuery.getCustomReturn(),
						new JsonObject(searchQuery.getAdditionnalParams()),
						searchQuery.getUserProfile(), searchQuery.isReverseUnion());
				break;
			case "visibleUsersForShare":
				future = communicationService.visibleUsersForShare(userId, searchQuery.getSearch(),
						new JsonArray(searchQuery.getUserIds()))
						.map(arr -> serialize(arr, BusUserDtoMapper::map));
				break;
			case "usersCanSeeMe":
				future = communicationService.usersCanSeeMe(userId)
						.map(arr -> serialize(arr, BusUserDtoMapper::map));
				break;
			case "visibleProfilsGroups":
				//FIXME: we can't define a DTO  until we remove customeReturn from query
				future = communicationService.visibleProfilsGroups(userId, searchQuery.getCustomReturn(),
						new JsonObject(searchQuery.getAdditionnalParams()), searchQuery.getPreFilter());
				break;
			case "visibleManualGroups":
				//FIXME: we can't define a DTO  until we remove customeReturn from query
				future = communicationService.visibleManualGroups(userId, searchQuery.getCustomReturn(),
						new JsonObject(searchQuery.getAdditionnalParams()));
				break;
			default:
				message.reply(new JsonArray());
				return;
		}

		future.onSuccess(message::reply)
			  .onFailure(err -> {
					log.warn(err.getMessage());
					message.reply(new JsonArray());
				});
	}

	private static <T> JsonArray serialize(JsonArray arr, Function<JsonObject, T> mapper) {
		return new JsonArray(arr.stream()
				.map(JsonObject.class::cast)
				.map(mapper)
				.map(JsonObject::mapFrom)
				.collect(Collectors.toList()));
	}

	private void visibleUsers(String userId, String schoolId, JsonArray expectedTypes,
			final Handler<Either<String, JsonArray>> handler) {
		visibleUsers(userId, schoolId, expectedTypes, false, null, null, handler);
	}

	private void visibleUsers(String userId, String schoolId, JsonArray expectedTypes, boolean itSelf,
			String customReturn, JsonObject additionnalParams, Handler<Either<String, JsonArray>> handler) {
		communicationService.visibleUsers(
				userId, schoolId, expectedTypes, itSelf, false, true, null, customReturn, additionnalParams, handler);
	}

	@Put("/init/rules")
	@SecuredAction("communication.init.default.rules")
	public void initDefaultCommunicationRules(final HttpServerRequest request) {
		RequestUtils.bodyToClass(request, InitDefaultRulesDTO.class).onSuccess(body -> {
			if (body.getStructures() == null || body.getStructures().isEmpty()) {
				badRequest(request, "invalid.structures");
				return;
			}
			JsonObject initDefaultRules = config.getJsonObject("initDefaultCommunicationRules");
			communicationService.initDefaultRules(new JsonArray(body.getStructures()), initDefaultRules)
					.onSuccess(result -> renderJson(request, result))
					.onFailure(err -> renderError(request, new JsonObject().put("error", err.getMessage())));
		}).onFailure(err -> badRequest(request, err.getMessage()));
	}

	@Post("/rules/:structureId/reset")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	@MfaProtected()
	public void resetRules(final HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		communicationService.resetRules(structureId, defaultResponseHandler(request));
	}

	/**
	 * Send the default communication rules contained inside the mod.json file.
	 * @param request Incoming request.
	 */
	@Get("/rules")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	@MfaProtected()
	public void getDefaultCommunicationRules(final HttpServerRequest request) {
		JsonObject initDefaultRules = config.getJsonObject("initDefaultCommunicationRules");
		Renders.renderJson(request, initDefaultRules, 200);
	}

	@Put("/rules/:structureId")
	@SecuredAction("communication.default.rules")
	public void defaultCommunicationRules(final HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		if (structureId == null || structureId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		communicationService.applyDefaultRules(new JsonArray().add(structureId),
				defaultResponseHandler(request));
	}

	@BusAddress("wse.communication")
	public void communicationEventBusHandler(final Message<JsonObject> message) {
		CommunicationBusDTO dto = message.body().mapTo(CommunicationBusDTO.class);
		JsonObject initDefaultRules = config.getJsonObject("initDefaultCommunicationRules");
		final Future<JsonObject> future;

		switch (dto.getAction() != null ? dto.getAction() : "") {
			case "initDefaultCommunicationRules":
				future = communicationService.initDefaultRules(new JsonArray(dto.getSchoolIds()), initDefaultRules);
				break;
			case "initAndApplyDefaultCommunicationRules":
				future = communicationService.initDefaultRules(new JsonArray(dto.getSchoolIds()), initDefaultRules,
								dto.getTransactionId(), dto.isCommit())
						.compose(ignored -> communicationService.applyDefaultRules(new JsonArray(dto.getSchoolIds()),
								dto.getTransactionId(), dto.isCommit()));
				break;
			case "setDefaultCommunicationRules":
				future = communicationService.applyDefaultRules(new JsonArray().add(dto.getSchoolId()));
				break;
			case "setMultipleDefaultCommunicationRules":
				future = communicationService.applyDefaultRules(new JsonArray(dto.getSchoolIds()));
				break;
			case "setCommunicationRules":
				future = communicationService.applyRules(dto.getGroupId());
				break;
			case "addLink":
				if (dto.getStartGroupId() == null || dto.getEndGroupId() == null) {
					future = Future.failedFuture("missing.parameters");
				} else {
					future = communicationService.addLink(dto.getStartGroupId(), dto.getEndGroupId());
				}
				break;
			case "addLinkWithUsers":
				if (dto.getGroupId() == null || dto.getDirection() == null) {
					future = Future.failedFuture("missing.parameters");
				} else {
					future = communicationService.addLinkWithUsers(dto.getGroupId(), parseDirection(dto.getDirection()));
				}
				break;
			case "removeLinkWithUsers":
				if (dto.getGroupId() == null || dto.getDirection() == null) {
					future = Future.failedFuture("missing.parameters");
				} else {
					future = communicationService.removeLinkWithUsers(dto.getGroupId(), parseDirection(dto.getDirection()));
				}
				break;
			default:
				future = Future.failedFuture("invalid.action");
		}

		future
				.onSuccess(message::reply)
				.onFailure(err -> message.reply(new JsonObject().put("status", "error").put("message", err.getMessage())));
	}

	private static CommunicationService.Direction parseDirection(String direction) {
		try {
			return CommunicationService.Direction.valueOf(direction.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			return CommunicationService.Direction.BOTH;
		}
	}

	@Delete("/rules")
	@SecuredAction("communication.remove.rules")
	public void removeCommunicationRules(HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		communicationService.removeRules(structureId)
				.onSuccess(v -> Renders.ok(request))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	public void setCommunicationService(CommunicationService communicationService) {
		this.communicationService = communicationService;
	}

	private class Params {
		private boolean myResult;
		private HttpServerRequest request;
		private String startGroupId;
		private String endGroupId;

		public Params(HttpServerRequest request) {
			this.request = request;
		}

		boolean isInvalid() {
			return myResult;
		}

		public String getStartGroupId() {
			return startGroupId;
		}

		public String getEndGroupId() {
			return endGroupId;
		}

		public Params validate() {
			startGroupId = request.params().get("startGroupId");
			endGroupId = request.params().get("endGroupId");
			if (startGroupId == null || startGroupId.trim().isEmpty() ||
					endGroupId == null || endGroupId.trim().isEmpty()) {
				badRequest(request, "invalid.parameter");
				myResult = true;
				return this;
			}
			myResult = false;
			return this;
		}
	}

	private CommunicationService.Direction getDirection(String direction) {
		CommunicationService.Direction d;
		try {
			d = CommunicationService.Direction.valueOf(direction.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			d = CommunicationService.Direction.BOTH;
		}
		return d;
	}

	private String getGroupId(HttpServerRequest request) {
		String groupId = request.params().get("groupId");
		if (groupId == null || groupId.trim().isEmpty()) {
			badRequest(request, "invalid.parameter");
			return null;
		}
		return groupId;
	}

    @Post("/group/:groupId/users")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
    public void safelyAddLinksWithUsers(HttpServerRequest request) {
        String groupId = getGroupId(request);
        if (groupId == null) return;
        communicationService.addLinkWithUsers(groupId, CommunicationService.Direction.BOTH)
                .onSuccess(v -> Renders.renderJson(request, new JsonObject().put("users", CommunicationService.Direction.BOTH), 200))
                .onFailure(th -> renderJson(request, new JsonObject().put("error", th.getMessage()), 400));
    }

    @Delete("/group/:groupId/users")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
    public void safelyRemoveLinksWithUsers(HttpServerRequest request) {
        String groupId = getGroupId(request);
        if (groupId == null) return;
        communicationService.safelyRemoveLinkWithUsers(groupId)
                .onSuccess(result -> Renders.renderJson(request, result, 200))
                .onFailure(th -> {
                    int status = CommunicationService.IMPOSSIBLE_TO_CHANGE_DIRECTION.equals(th.getMessage()) ? 409 : 500;
                    renderJson(request, new JsonObject().put("error", th.getMessage()), status);
                });
    }

    @Get("/v2/group/:startGroupId/communique/:endGroupId/check")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void addLinkCheckOnly(HttpServerRequest request) {
		Params params = new Params(request).validate();
		if (params.isInvalid()) return;

		UserUtils.getAuthenticatedUserInfos(eb, request)
				.compose(user -> communicationService.addLinkCheckOnly(params.getStartGroupId(), params.getEndGroupId(), user))
				.onSuccess(result -> Renders.renderJson(request, result, 200))
				.onFailure(th -> {
					int status = CommunicationService.IMPOSSIBLE_TO_CHANGE_DIRECTION.equals(th.getMessage()) ? 409 : 500;
					renderJson(request, new JsonObject().put("error", th.getMessage()), status);
				});
	}

	@Post("/v2/group/:startGroupId/communique/:endGroupId")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void processAddLinkAndChangeDirection(HttpServerRequest request) {
		Params params = new Params(request).validate();
		if (params.isInvalid()) return;

		communicationService.addLink(params.getStartGroupId(), params.getEndGroupId())
				.compose(v -> communicationService.processChangeDirectionAfterAddingLink(params.getStartGroupId(), params.getEndGroupId()))
				.onSuccess(result -> render(request, AddLinkDirectionsDtoMapper.map(result)))
				.onFailure(th -> renderJson(request, new JsonObject().put("error", th.getMessage()), 500));
	}
    
	@Delete("/group/:startGroupId/relations/:endGroupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void removeRelations(HttpServerRequest request) {
		Params params = new Params(request).validate();
		if (params.isInvalid()) return;
		communicationService.removeRelations(params.getStartGroupId(), params.getEndGroupId())
				.onSuccess(result -> render(request, RemoveRelationsResultDtoMapper.map(result)))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Indicates if a sender (user) can communicate to a receiver (user, group)
	 * Returns JsonObject :
	 * {
	 * canCommunicate : true/false
	 * } Check if communication is allowed between two user Id
	 *
	 * @param request from : id for the sender
	 *                to : id for the recipient
	 */
	// Check if MfaProtected annotation is still needed
	@Get("/verify/:from/:to")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void verify(HttpServerRequest request) {
		String from = request.params().get("from");
		String to = request.params().get("to");

		if(from == null || from.trim().isEmpty() || to == null || to.trim().isEmpty()) {
			badRequest(request, "invalid.parameter");
			return;
		}
		communicationService.verify(from, to)
				.onSuccess(result -> render(request, VerifyResultDtoMapper.map(result)))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Discover visible users, return list of users that can be dicover by the user
	 * @param request
	 * 	{
	 * 		"structures": ["id1", "id2"],
	 * 		"profiles": ["Teacher"],
	 * 		"search": "search",
	 * 	}
	 *
	 * */
	@Post("/discover/visible/users")
	@SecuredAction(value= "", type = ActionType.RESOURCE)
	@ResourceFilter(CommunicationDiscoverVisibleFilter.class)
	public void getDiscoverVisibleUsers(HttpServerRequest request) {
		RequestUtils.bodyToClass(request, DiscoverVisibleFilterDTO.class)
				.compose(filter -> UserUtils.getAuthenticatedUserInfos(eb, request)
						.compose(user -> communicationService.getDiscoverVisibleUsers(user.getUserId(), filter)))
				.onSuccess(results -> render(request, results.stream()
						.map(JsonObject.class::cast)
						.map(UserDtoMapper::mapDiscoverVisible)
						.collect(Collectors.toList())))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Return list of accepted profile for discover visible
	 * */
	@Get("/discover/visible/profiles")
	@SecuredAction(value= "", type = ActionType.AUTHENTICATED)
	public void getDiscoverVisibleAcceptedProfile(HttpServerRequest request) {
		UserUtils.getAuthenticatedUserInfos(eb, request)
				.compose(user -> communicationService.getDiscoverVisibleAcceptedProfile(user))
				.onSuccess(results -> render(request, results.getList()))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Return list of all structures, to be use to filter discover visible users
	 * */
	@Get("/discover/visible/structures")
	@SecuredAction(value= "", type = ActionType.RESOURCE)
	@ResourceFilter(CommunicationDiscoverVisibleFilter.class)
	public void getDiscoverVisibleStructures(HttpServerRequest request) {
		UserUtils.getAuthenticatedUserInfos(eb, request)
				.compose(user -> communicationService.getDiscoverVisibleStructures())
				.onSuccess(results -> render(request, results.stream()
						.map(JsonObject.class::cast)
						.map(DiscoverVisibleStructureDtoMapper::map)
						.collect(Collectors.toList())))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Add communication rights between two users, this methode use "COMMUIQUE_DIRECT" with source 'MANUAL'
	 * @param request
	 * 	{
	 * 		"receiverId": "receiverId"
	 * 	}
	 * */
	@Post("/discover/visible/add/commuting/:receiverId")
	@SecuredAction(value= "", type = ActionType.RESOURCE)
	@ResourceFilter(CommunicationDiscoverVisibleFilter.class)
	public void discoverVisibleAddCommuteUsers(HttpServerRequest request) {
		String receiverId = request.params().get("receiverId");
		if(receiverId == null || receiverId.trim().isEmpty()) {
			badRequest(request, "invalid.parameter");
			return;
		}
		UserUtils.getAuthenticatedUserInfos(eb, request)
				.compose(user -> communicationService.discoverVisibleAddCommuteUsers(user, receiverId, request))
				.onSuccess(result -> render(request, new CountResultDTO(result.getInteger("number"))))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Delete communication rights between two users, this methode delete "COMMUIQUE_DIRECT" with source 'MANUAL'
	 * */
	@Delete("/discover/visible/remove/commuting/:receiverId")
	@SecuredAction(value= "", type = ActionType.RESOURCE)
	@ResourceFilter(CommunicationDiscoverVisibleFilter.class)
	public void discoverVisibleRemoveCommuteUsers(HttpServerRequest request) {
		String receiverId = request.params().get("receiverId");
		if(receiverId == null || receiverId.trim().isEmpty()) {
			badRequest(request, "invalid.parameter");
			return;
		}
		UserUtils.getAuthenticatedUserInfos(eb, request)
				.compose(user -> communicationService.discoverVisibleRemoveCommuteUsers(user.getUserId(), receiverId))
				.onSuccess(result -> render(request, new CountResultDTO(result.getInteger("number"))))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * This methode return all groups that the user can see, with the group type 'manager'
	 * */
	@Get("/discover/visible/groups")
	@SecuredAction(value= "", type = ActionType.RESOURCE)
	@ResourceFilter(CommunicationDiscoverVisibleFilter.class)
	public void discoverVisibleGetGroups(HttpServerRequest request) {
		UserUtils.getAuthenticatedUserInfos(eb, request)
				.compose(user -> communicationService.discoverVisibleGetGroups(user.getUserId()))
				.onSuccess(results -> render(request, results.stream()
						.map(JsonObject.class::cast)
						.map(GroupDtoMapper::map)
						.collect(Collectors.toList())))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Get the list of users in a group
	 * @param request
	 * 	{
	 * 		"groupId": "groupId"
	 * 	}
	 * */
	@Get("/discover/visible/group/:groupId/users")
	@SecuredAction(value= "", type = ActionType.RESOURCE)
	@ResourceFilter(CommunicationDiscoverVisibleFilter.class)
	public void discoverVisibleGetUsersInGroup(HttpServerRequest request) {

		String groupId = request.params().get("groupId");

		UserUtils.getAuthenticatedUserInfos(eb, request)
				.compose(user -> communicationService.discoverVisibleGetUsersInGroup(user.getUserId(), groupId))
				.onSuccess(results -> render(request, results.stream()
						.map(JsonObject.class::cast)
						.map(UserDtoMapper::mapDiscoverVisibleGroupUser)
						.collect(Collectors.toList())))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));

	}

	/**
	 * Create a group with the group type 'manager'
	 * @param request
	 * 	{
	 * 		"name": "name",
	 * 	}
	 * */
	@Post("/discover/visible/group")
	@SecuredAction(value= "", type = ActionType.RESOURCE)
	@ResourceFilter(CommunicationDiscoverVisibleFilter.class)
	public void createDiscoverVisibleGroup(HttpServerRequest request) {
		RequestUtils.bodyToClass(request, DiscoverVisibleGroupBodyDTO.class).compose(body -> {
			if (body.getName() == null || body.getName().trim().isEmpty()) {
				return Future.failedFuture("invalid.name");
			}
			return UserUtils.getAuthenticatedUserInfos(eb, request)
					.compose(user -> communicationService.createDiscoverVisibleGroup(user.getUserId(), body));
		})
				.onSuccess(result -> render(request, new IdentifiableDTO(result.getString("id"), null)))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Update a group with the group type 'manager'
	 * @param request
	 * 	{
	 * 		"groupId": "groupId",
	 * 		"name": "name",
	 * 	}
	 * */
	@Put("/discover/visible/group/:groupId")
	@SecuredAction(value= "", type = ActionType.RESOURCE)
	@ResourceFilter(CommunicationDiscoverVisibleFilter.class)
	public void updateDiscoverVisibleGroup(HttpServerRequest request) {
		String groupId = request.params().get("groupId");
		if(groupId == null || groupId.trim().isEmpty()) {
			badRequest(request, "invalid.parameter");
			return;
		}
		RequestUtils.bodyToClass(request, DiscoverVisibleGroupBodyDTO.class)
				.compose(body -> {
			if (body.getName() == null || body.getName().trim().isEmpty()) {
				return Future.failedFuture("invalid.name");
			}
			return UserUtils.getAuthenticatedUserInfos(eb, request)
					.compose(user -> communicationService.updateDiscoverVisibleGroup(user.getUserId(), groupId, body));
		}).onSuccess(result -> render(request, new IdentifiableDTO(result.getString("id"), null)))
		  .onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Update the groups member, with adding the new user in the group and send a notification to the user, and removing the user from the group
	 * @param request
	 * 	{
	 * 		"groupId": "groupId",
	 * 		"oldUsers": ["userId1", "userId2"], // list of users befor change
	 * 		"newUsers": "["userId1", "userId2"]" //list of users after change
	 * 	}
	 * */
	@Put("/discover/visible/group/:groupId/users")
	@SecuredAction(value= "", type = ActionType.RESOURCE)
	@ResourceFilter(CommunicationDiscoverVisibleFilter.class)
	public void addDiscoverVisibleGroupUsers(HttpServerRequest request) {
		String groupId = request.params().get("groupId");
		if(groupId == null || groupId.trim().isEmpty()) {
			badRequest(request, "invalid.parameter");
			return;
		}

		RequestUtils.bodyToClass(request, DiscoverModifyGroupUsersDTO.class).compose(body -> {
					if (body.getNewUsers() == null || body.getNewUsers().isEmpty()) {
						return Future.failedFuture("invalid.users");
					}
					if (body.getNewUsers().size() > 100) {
						return Future.failedFuture("too.many.users");
					}
					return UserUtils.getAuthenticatedUserInfos(eb, request)
							.compose(user -> communicationService.addDiscoverVisibleGroupUsers(user, groupId, body, request));
				})
				.onSuccess(v -> Renders.ok(request))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
	}

	/**
	 * Search entities (users, groups) visible by the requester.
	 * This endpoint can serve 2 different types of results based on the configuration :
	 * - the old format that used to be served by the module conversation
	 * - the new format which returns more data
	 * @param request Caller's HTTP request
	 */
	@Get("/visible/search")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void searchVisibleContacts(HttpServerRequest request) {
		UserUtils.getAuthenticatedUserInfos(eb, request)
		.onSuccess(userInfos -> {
			final String query = request.params().get("query");
			final String mode = request.params().get("mode");
			communicationService.searchVisibles(userInfos, query, mode, I18n.acceptLanguage(request))
				.onSuccess(visibles -> render(request, visibles.stream()
						.map(JsonObject.class::cast)
						.map(SearchVisibleContactDtoMapper::map)
						.collect(Collectors.toList())))
				.onFailure(th -> renderError(request, new JsonObject().put("error", th.getMessage())));
		});
	}
}
