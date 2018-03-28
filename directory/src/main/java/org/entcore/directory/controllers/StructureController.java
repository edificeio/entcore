/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.directory.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;

import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.http.filter.AdmlOfStructure;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.pojo.Ent;
import org.entcore.directory.security.AdminStructureFilter;
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
import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class StructureController extends BaseController {

	private SchoolService structureService;
	private EmailSender notifHelper;
	private String assetsPath = "../..";
	private Map<String, String> skins = new HashMap<>();
	private String node;

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		node = (String) vertx.sharedData().getLocalMap("server").get("node");
		if (node == null) {
			node = "";
		}
	}

	@Put("/structure/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "updateStructure", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				String structureId = request.params().get("structureId");
				structureService.update(structureId, body, defaultResponseHandler(request));
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

				structureService.massmailUsers(structureId, filter, true, true, filterMail, infos, arrayResponseHandler(request));
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

				structureService.massMailAllUsersByStructure(structureId, infos, arrayResponseHandler(request));
			}
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

		this.assetsPath = (String) vertx.sharedData().getLocalMap("server").get("assetPath");
		this.skins = vertx.sharedData().getLocalMap("skins");

		final String assetsPath = this.assetsPath + "/assets/themes/" + this.skins.get(Renders.getHost(request));
		final String templatePath = assetsPath + "/template/directory/";
		final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) + "/assets/themes/" + this.skins.get(Renders.getHost(request)) + "/img/";

		final boolean groupClasses = !filter.getJsonArray("sort").contains("classname");

		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			public void handle(final UserInfos infos) {

				//PDF
				if("pdf".equals(type)){
					structureService.massmailUsers(structureId, filter, groupClasses, false, filterMail, infos, new Handler<Either<String,JsonArray>>() {
						public void handle(Either<String, JsonArray> result) {
							if(result.isLeft()){
								forbidden(request);
								return;
							}

							final JsonObject templateProps = new JsonObject().put("users", result.right().getValue());

							vertx.fileSystem().readFile(templatePath + "massmail.pdf.xhtml", new Handler<AsyncResult<Buffer>>() {

								@Override
								public void handle(AsyncResult<Buffer> result) {
									if(!result.succeeded()){
										badRequest(request);
										return;
									}

									StringReader reader = new StringReader(result.result().toString("UTF-8"));

									processTemplate(request, templateProps, "massmail.pdf.xhtml", reader, new Handler<Writer>(){
										public void handle(Writer writer) {
											String processedTemplate = ((StringWriter) writer).getBuffer().toString();

											if(processedTemplate == null){
												badRequest(request);
												return;
											}

											JsonObject actionObject = new JsonObject();
					    		        	actionObject
					    		        		.put("content", processedTemplate.getBytes())
					    		        		.put("baseUrl", baseUrl);

											eb.send(node + "entcore.pdf.generator", actionObject, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
												public void handle(Message<JsonObject> reply) {
													JsonObject pdfResponse = reply.body();
													if(!"ok".equals(pdfResponse.getString("status"))){
														badRequest(request, pdfResponse.getString("message"));
														return;
													}

													byte[] pdf = pdfResponse.getBinary("content");
													request.response().putHeader("Content-Type", "application/pdf");
													request.response().putHeader("Content-Disposition",
															"attachment; filename="+filename+".pdf");
													request.response().end(Buffer.buffer(pdf));
												}
					        	        	}));
										}

									});
								}
							});
						}
					});
				}
				//Mail
				else if("mail".equals(type)){
					structureService.massmailUsers(structureId, filter, true, true, filterMail, infos, new Handler<Either<String,JsonArray>>() {
						public void handle(final Either<String, JsonArray> result) {
							if(result.isLeft()){
								forbidden(request);
								return;
							}

							final JsonArray users = result.right().getValue();

							vertx.fileSystem().readFile(templatePath + "massmail.mail.txt", new Handler<AsyncResult<Buffer>>() {
								@Override
								public void handle(AsyncResult<Buffer> result) {
									if(!result.succeeded()){
										badRequest(request);
										return;
									}

									StringReader reader = new StringReader(result.result().toString("UTF-8"));
									final JsonArray mailHeaders = new fr.wseduc.webutils.collections.JsonArray().add(
											new JsonObject().put("name", "Content-Type").put("value", "text/html; charset=\"UTF-8\""));

									for(Object userObj : users){
										final JsonObject user = (JsonObject) userObj;
										final String userMail = user.getString("email");
										if(userMail == null || userMail.trim().isEmpty()){
											continue;
										}

										final String mailTitle = !user.containsKey("activationCode") ||
													user.getString("activationCode") == null ||
													user.getString("activationCode").trim().isEmpty() ?
												"directory.massmail.mail.subject.activated" :
												"directory.massmail.mail.subject.not.activated";

										try{
											reader.reset();
										} catch(IOException exc){
											log.error("[MassMail] Error on StringReader ("+exc.toString()+")");
										}

										processTemplate(request, user, "massmail.mail.txt", reader, new Handler<Writer>(){
											public void handle(Writer writer) {
												String processedTemplate = ((StringWriter) writer).getBuffer().toString();

												if(processedTemplate == null){
													badRequest(request);
													return;
												}

												notifHelper.sendEmail(
														request,
														userMail, null, null,
														mailTitle,
														processedTemplate, null, true, mailHeaders,
														ar -> {
															if(ar.failed()) {
																log.error("[MassMail] Error while sending mail", ar.cause());
															}
														});
											}

										});
									}

									ok(request);
								}
							});
						}
					});
				} else {
					badRequest(request);
				}

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
	
	public void setStructureService(SchoolService structureService) {
		this.structureService = structureService;
	}

	public void setNotifHelper(EmailSender notifHelper) {
		this.notifHelper = notifHelper;
	}

}

