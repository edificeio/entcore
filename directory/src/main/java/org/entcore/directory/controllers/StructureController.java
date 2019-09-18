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

package org.entcore.directory.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;

import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.http.filter.AdmlOfStructure;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.pojo.Ent;
import org.entcore.directory.security.AdminStructureFilter;
import org.entcore.directory.security.AnyAdminOfUser;
import org.entcore.directory.services.MassMailService;
import org.entcore.directory.services.SchoolService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class StructureController extends BaseController {

	private MassMailService massMailService;
	private SchoolService structureService;
	private EmailSender notifHelper;
	private String assetsPath = "../..";
	private Map<String, String> skins = new HashMap<>();


	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
	}

	@Put("/structure/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "updateStructure", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				UserUtils.getUserInfos(eb, request, user -> {
					if (user == null || !UserUtils.isSuperAdmin(user)) {
						body.remove("UAI");
						body.remove("hasApp");
					} else if (body.containsKey("UAI") && isEmpty(body.getString("UAI"))) {
						body.putNull("UAI");
					}
					String structureId = request.params().get("structureId");
					structureService.update(structureId, body, defaultResponseHandler(request));
				});
			}
		});
	}

	@Put("/structure/:structureId/link/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void linkUser(final HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final String userId = request.params().get("userId");
		structureService.link(structureId, userId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					if (r.right().getValue() != null && r.right().getValue().size() > 0) {
						JsonArray a = new fr.wseduc.webutils.collections.JsonArray().add(userId);
						ApplicationUtils.sendModifiedUserGroup(eb, a, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> message) {
								JsonObject j = new JsonObject()
										.put("action", "setDefaultCommunicationRules")
										.put("schoolId", structureId);
								eb.send("wse.communication", j);
							}
						}));
						renderJson(request, r.right().getValue(), 200);
					} else {
						notFound(request);
					}
				} else {
					renderJson(request, new JsonObject().put("error", r.left().getValue()), 400);
				}
			}
		});
	}

	@Delete("/structure/:structureId/unlink/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void unlinkUser(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String structureId = request.params().get("structureId");
		structureService.unlink(structureId, userId, notEmptyResponseHandler(request));
	}

	@Get("/structure/admin/list")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listAdmin(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					structureService.listAdmin(user, arrayResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Put("/structure/:structureId/parent/:parentStructureId")
	@SecuredAction("structure.define.parent")
	public void defineParent(final HttpServerRequest request) {
		final String parentStructureId = request.params().get("parentStructureId");
		final String structureId = request.params().get("structureId");
		structureService.defineParent(structureId, parentStructureId, notEmptyResponseHandler(request));
	}

	@Delete("/structure/:structureId/parent/:parentStructureId")
	@SecuredAction("structure.remove.parent")
	public void removeParent(final HttpServerRequest request) {
		final String parentStructureId = request.params().get("parentStructureId");
		final String structureId = request.params().get("structureId");
		structureService.removeParent(structureId, parentStructureId, defaultResponseHandler(request));
	}

	@Get("/structures")
	@SecuredAction("structure.list.all")
	public void listStructures(final HttpServerRequest request) {
		String format = request.params().get("format");
		JsonArray fields = new fr.wseduc.webutils.collections.JsonArray().add("id").add("externalId").add("name").add("UAI")
				.add("address").add("zipCode").add("city").add("phone").add("academy");
		if ("XML".equalsIgnoreCase(format)) {
			structureService.list(fields, new Handler<Either<String, JsonArray>>() {
				@Override
				public void handle(Either<String, JsonArray> event) {
					if (event.isRight()) {
						JsonArray r = event.right().getValue();
						Ent ent = new Ent();
						for (Object o: r) {
							if (!(o instanceof JsonObject)) continue;
							JsonObject j = (JsonObject) o;
							Ent.Etablissement etablissement = new Ent.Etablissement();
							etablissement.setEtablissementId(j.getString("UAI", ""));
							etablissement.setEtablissementUid(j.getString("UAI", ""));
							etablissement.setCodePorteur(j.getString("academy", ""));
							etablissement.setNomCourant(j.getString("name", ""));
							etablissement.setAdressePlus(j.getString("address", ""));
							etablissement.setCodePostal(j.getString("zipCode", ""));
							etablissement.setVille(j.getString("city", ""));
							etablissement.setTelephone(j.getString("phone", ""));
							etablissement.setFax("");
							ent.getEtablissement().add(etablissement);
						}
						try {
							StringWriter response = new StringWriter();
							JAXBContext context = JAXBContext.newInstance(Ent.class);
							Marshaller marshaller = context.createMarshaller();
							marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
							marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
							marshaller.marshal(ent, response);
							request.response().putHeader("content-type", "application/xml");
							request.response().end(response.toString());
						} catch (JAXBException e) {
							log.error(e.toString(), e);
							request.response().setStatusCode(500);
							request.response().end(e.getMessage());
						}
					} else {
						leftToResponse(request, event.left());
					}
				}
			});
		} else {
			structureService.list(fields, arrayResponseHandler(request));
		}
	}

	@Get("/structure/:structureId/levels")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getLevels(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos infos) {
				structureService.getLevels(request.params().get("structureId"), infos, arrayResponseHandler(request));
			}
		});
	}

	@Put("/structure/:structureId/levels-of-education")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void setLevels(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				JsonArray jsonLevelsOfEducation = body.getJsonArray("levelsOfEducation");
				List<Integer> levelsOfEducation = jsonLevelsOfEducation.getList();
				structureService.setLevelsOfEducation(
						request.params().get("structureId"),
						levelsOfEducation,
						defaultResponseHandler(request)
				);
			}
		});
	}

	@Put("/structure/:structureId/distributions")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void setDistributions(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				JsonArray jsonDistributions = body.getJsonArray("distributions");
				List<String> distributions = jsonDistributions.getList();
				structureService.setDistributions(
						request.params().get("structureId"),
						distributions,
						defaultResponseHandler(request)
				);
			}
		});
	}

	@Get("/structure/:structureId/massMail/users")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getMassmailUsers(final HttpServerRequest request){
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos infos) {
				final JsonObject filter = new JsonObject();
				final String structureId = request.params().get("structureId");
				final List<String> sorts = request.params().getAll("s");
				final Boolean filterMail = request.params().contains("mail") ?
						new Boolean(request.params().get("mail")) :
						null;

				filter
					.put("profiles", new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("p")))
					.put("levels", new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("l")))
					.put("classes", new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("c")))
					.put("sort", new fr.wseduc.webutils.collections.JsonArray(sorts));

				if(request.params().contains("a")){
					filter.put("activated", request.params().get("a"));
				}

				if(request.params().contains("dateFilter") && request.params().contains("date")) {
				    filter.put("dateFilter", request.params().get("dateFilter"));
				    filter.put("date", request.params().get("date"));
                }

				massMailService.massmailUsers(structureId, filter, true, true, filterMail, true, infos, arrayResponseHandler(request));
			}
		});
	}

	@Get("/structure/:structureId/massMail/allUsers")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getMassMailUsersList(final HttpServerRequest request){
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos infos) {
				final String structureId = request.params().get("structureId");

				massMailService.massMailAllUsersByStructure(structureId, infos, arrayResponseHandler(request));
			}
		});
	}

	@Get("/structure/massMail/:userId/:type")
	@ResourceFilter(AnyAdminOfUser.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void performMassmailUser(final HttpServerRequest request){
		final String userId = request.params().get("userId");
		final String type = request.params().get("type");
		final String filename = "mettre le nom de de l'utilisateur";
		if(userId == null){
			badRequest(request);
			return;
		}

		this.assetsPath = (String) vertx.sharedData().getLocalMap("server").get("assetPath");
		this.skins = vertx.sharedData().getLocalMap("skins");

		getSkin(request, result -> {

			final String skin;
			if (result.isLeft() || result.right().getValue() == null) {
				skin = this.skins.get(Renders.getHost(request));
			} else {
				skin = result.right().getValue();
			}

			final String assetsPath = this.assetsPath + "/assets/themes/" + skin;
			final String templatePath = assetsPath + "/template/directory/";
			final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) + "/assets/themes/" + skin + "/img/";


			UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
				public void handle(final UserInfos infos) {

					//PDF
					if("pdf".equals(type)){
						massMailService.massMailUser(userId, infos, new Handler<Either<String,JsonArray>>() {
							public void handle(Either<String, JsonArray> result) {
								if(result.isLeft()){
									forbidden(request);
									return;
								}

								massMailService.massMailTypePdf(infos, request, templatePath, baseUrl, filename, "pdf", result.right().getValue());

							}
						});
					}
					//Mail
					else if("mail".equals(type)){
						massMailService.massMailUser(userId, infos, new Handler<Either<String,JsonArray>>() {
							public void handle(final Either<String, JsonArray> result) {
								if(result.isLeft()){
									forbidden(request);
									return;
								}

								massMailService.massMailTypeMail(infos, request, templatePath, result.right().getValue());
							}
						});
					} else {
						badRequest(request);
					}

				}
			});

		});

	}


	@Get("/structure/:structureId/massMail/process/:type")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void performMassmail(final HttpServerRequest request){
		final String structureId = request.params().get("structureId");
		final String type = request.params().get("type");
		final JsonObject filter = new JsonObject();
		final String filename = request.params().get("filename");
		final Boolean filterMail = request.params().contains("mail") ?
				new Boolean(request.params().get("mail")) :
				null;

		filter
			.put("profiles", new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("p")))
			.put("levels", new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("l")))
			.put("classes", new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("c")))
			.put("sort", new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("s")));

		if(request.params().contains("a")){
			filter.put("activated", request.params().get("a"));
		}

        if(request.params().contains("adml")){
            filter.put("adml", request.params().get("adml"));
        }

        if(request.params().contains("dateFilter") && request.params().contains("date")) {
            filter.put("dateFilter", request.params().get("dateFilter"));
            filter.put("date", request.params().get("date"));
        }

		this.assetsPath = (String) vertx.sharedData().getLocalMap("server").get("assetPath");
		this.skins = vertx.sharedData().getLocalMap("skins");

		getSkin(request, result -> {

			final String skin;
			if (result.isLeft() || result.right().getValue() == null) {
				skin = this.skins.get(Renders.getHost(request));
			} else {
				skin = result.right().getValue();
			}

			final String assetsPath = this.assetsPath + "/assets/themes/" + skin;
			final String templatePath = assetsPath + "/template/directory/";
			final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) + "/assets/themes/" + skin + "/img/";

			final boolean isSimplePdf = "simplePdf".equals(type);

			UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
				public void handle(final UserInfos infos) {

					//PDF
					if("pdf".equals(type) || "newPdf".equals(type) || isSimplePdf){
						massMailService.massmailUsers(structureId, filter, true, isSimplePdf, filterMail, true, infos, new Handler<Either<String,JsonArray>>() {
							public void handle(Either<String, JsonArray> result) {
								if(result.isLeft()){
									forbidden(request);
									return;
								}

								massMailService.massMailTypePdf(infos, request, templatePath, baseUrl, filename, type, result.right().getValue());
							}
						});
					}
					//Mail
					else if("mail".equals(type)){
						massMailService.massmailUsers(structureId, filter, true, true, filterMail, true, infos, new Handler<Either<String,JsonArray>>() {
							public void handle(final Either<String, JsonArray> result) {
								if(result.isLeft()){
									forbidden(request);
									return;
								}

								massMailService.massMailTypeMail(infos, request, templatePath, result.right().getValue());
							}
						});
					} else {
						badRequest(request);
					}

				}
			});

		});

	}

	@Post("/class-admin/massmail")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void classAdminMassMail(final HttpServerRequest request){
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {

				JsonArray userIds = body.getJsonArray("ids");
				String type = body.getString("type");
				String schoolId = body.getString("structureId");

				if (userIds == null || userIds.isEmpty() || type == null || schoolId == null) {
					badRequest(request);
				}

				final String host = Renders.getHost(request);
				final Map<String, String> skins = vertx.sharedData().getLocalMap("skins");

				getSkin(request, result -> {

					final String skin;
					if (result.isLeft() || result.right().getValue() == null) {
						skin = skins.get(host);
					} else {
						skin = result.right().getValue();
					}

					final String assetsPath = (String) vertx.sharedData().getLocalMap("server").get("assetPath") +
							"/assets/themes/" + skin;
					final String templatePath = assetsPath + "/template/directory/";
					final String baseUrl = getScheme(request) + "://" + host + "/assets/themes/" + skin + "/img/";

					UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
						public void handle(final UserInfos infos) {

							JsonObject filterObj = new JsonObject();
							filterObj.put("userIds",userIds);
							// We set neither true or false to get both activated and non-activated users
							filterObj.put("activated","both");
							filterObj.put("sort",new JsonArray().add("displayName"));

							final boolean isNotPdf = !type.equals("pdf");
							massMailService.massmailNoCheck(schoolId, filterObj, isNotPdf, infos, new Handler<Either<String, JsonArray>>() {
								@Override
								public void handle(Either<String, JsonArray> result) {
									if (result.isLeft()) {
										forbidden(request);
										return;
									}
									JsonArray users = result.right().getValue();
									switch (type) {
										case "pdf":
										case "newPdf":
										case "simplePdf":
											massMailService.massMailTypePdf(infos, request, templatePath, baseUrl, "massmail", type, users);
											break;
										case "mail":
											massMailService.massMailTypeMail(infos, request, templatePath, users);
											break;
										case "csv":
											massMailService.massMailTypeCSV(request, users);
											break;
										default:
											badRequest(request);
									}
								}
							});
						}
					});

				});

			}
		});
	}

	@Get("/structure/:structureId/metrics")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdmlOfStructure.class)
	public void metrics(final HttpServerRequest request){
		structureService.getMetrics(request.params().get("structureId"), defaultResponseHandler(request));
	}
	
	@Get("/structure/:id/sources")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listSources(final HttpServerRequest request) {
		String structureId = request.params().get("id");
		this.structureService.listSources(structureId, arrayResponseHandler(request));
	}

	@Get("/structure/:id/aaffunctions")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listAafFunctions(final HttpServerRequest request) {
		String structureId = request.params().get("id");
		this.structureService.listAafFunctions(structureId, arrayResponseHandler(request));
	}

	@Get("/structure/:id/quicksearch/users")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminStructureFilter.class)
	public void quickSearchUsers(HttpServerRequest request) {
		String structureId = request.params().get("id");
		String input = request.params().get("input");

		if(input == null || input.trim().length() == 0){
			badRequest(request);
			return;
		}

		this.structureService.quickSearchUsers(structureId, input, arrayResponseHandler(request));
	}

	@Get("/structure/:id/users")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminStructureFilter.class)
	public void userList(HttpServerRequest request) {
		String structureId = request.params().get("id");
		this.structureService.userList(structureId, arrayResponseHandler(request));
	}

	@Put("structure/:id/profile/block")
	@SecuredAction( value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminStructureFilter.class)
	public void blockUsers(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject json) {
				final String structureId = request.params().get("id");
				final String profile = json.getString("profile");
				final boolean block = json.getBoolean("block", true);
				structureService.blockUsers(structureId, profile, block, new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject r) {
						if ("ok".equals(r.getString("status"))) {
							request.response().end();
							JsonArray usersId = r.getJsonArray("result").getJsonObject(0).getJsonArray("usersId");
							for (Object userId : usersId) {
								UserUtils.deletePermanentSession(eb, (String) userId, null, new Handler<Boolean>() {
									@Override
									public void handle(Boolean event) {
										if (!event) {
											log.error("Error delete permanent session with userId : " + userId);
										}
									}
								});
								UserUtils.deleteCacheSession(eb, (String) userId, new Handler<Boolean>() {
									@Override
									public void handle(Boolean event) {
										if (!event) {
											log.error("Error delete cache session with userId : " + userId);
										}
									}
								});
							}
						}
						else {
							badRequest(request);
						}
					}
				});
			}
		});
	}

	public void setMassMailService(MassMailService massMailService) {
		this.massMailService = massMailService;
	}

	public void setStructureService(SchoolService structureService) {
		this.structureService = structureService;
	}

	public void setNotifHelper(EmailSender notifHelper) {
		this.notifHelper = notifHelper;
	}

	private void getSkin(HttpServerRequest request, Handler<Either<String,String>> handler) {
		eb.send("userbook.preferences", new JsonObject().put("action", "get.currentuser")
				.put("request", new JsonObject().put("headers", new JsonObject().put("Cookie", request.getHeader("Cookie"))))
				.put("application", "theme"), result -> {
			if (result.succeeded()) {
				JsonObject data = (JsonObject)(result.result().body());
				String skin = null;
				try {
					skin = data.getJsonObject("value").getString("preference");
				} catch (Exception e) {
					skin = null;
				} finally {
					handler.handle(new Either.Right<>(skin));
				}
			} else {
				handler.handle(new Either.Left<>(result.cause().getMessage()));
			}
		});
	}
}

