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

package org.entcore.directory.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.rs.Delete;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.AdmlOfStructure;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.utils.MapFactory;
import org.entcore.common.utils.StringUtils;
import org.entcore.directory.security.UserInStructure;
import org.entcore.directory.security.AdmlOfStructureWithoutEDTInit;
import org.entcore.directory.services.TimetableService;
import org.joda.time.DateTime;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class TimetableController extends BaseController {

  private TimetableService timetableService;
  private Map<String, Long> importInProgress;

  @Override
  public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
    super.init(vertx, config, rm, securedActions);

    this.importInProgress = MapFactory.getSyncClusterMap("timetable-imports", vertx);

    Long periodicInProgressClear = config.getLong("periodicInProgressClear");

    if (periodicInProgressClear != null) {
      vertx.setPeriodic(periodicInProgressClear, new Handler<Long>() {
        @Override
        public void handle(Long event) {
          final long limit = System.currentTimeMillis() - config.getLong("periodicInProgressClear", 3600000l);
          Set<Map.Entry<String, Long>> entries = new HashSet<>(importInProgress.entrySet());

          for (Map.Entry<String, Long> e : entries) {
            if (e.getValue() == null || e.getValue() < limit) {
              importInProgress.remove(e.getKey());
            }
          }
        }
      });
    }
  }

  @Get("/timetable")
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @ResourceFilter(AdminFilter.class)
  @MfaProtected()
  public void timetable(HttpServerRequest request) {
    renderView(request);
  }

  @Get("/timetable/courses/:structureId")
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @ResourceFilter(SuperAdminFilter.class)
  @MfaProtected()
  public void listCourses(HttpServerRequest request) {
    final String structureId = request.params().get("structureId");
    long lastDate;
    try {
      lastDate = Long.parseLong(getOrElse(request.params().get("lastDate"), "0", false));
    } catch (NumberFormatException e) {
      try {
        lastDate = DateTime.parse(request.params().get("lastDate")).getMillis();
      } catch (RuntimeException e2) {
        badRequest(request, "invalid.date");
        return;
      }
    }
    timetableService.listCourses(structureId, lastDate, arrayResponseHandler(request));
  }

  @Get("/timetable/courses/:structureId/:begin/:end")
  @ApiDoc("Get courses for a structure between two dates by optional teacher id and/or optional group name.")
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @ResourceFilter(UserInStructure.class)
  public void listCoursesBetweenTwoDates(final HttpServerRequest request) {
    final String structureId = request.params().get("structureId");
    final String teacherId = request.params().get("teacherId");
    final List<String> groupNames = request.params().getAll("group");
    final String beginDate = request.params().get("begin");
    final String endDate = request.params().get("end");

    if (beginDate != null && endDate != null &&
        beginDate.matches("\\d{4}-\\d{2}-\\d{2}") && endDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
      timetableService.listCoursesBetweenTwoDates(structureId, teacherId, groupNames, beginDate, endDate, arrayResponseHandler(request));
    } else {
      badRequest(request, "timetable.invalid.dates");
    }
  }

  @Get("/timetable/subjects/:structureId")
  @ApiDoc("Get subject list of the structure by optional teacher identifiers and with the ability to display associated groups and classes.")
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @ResourceFilter(UserInStructure.class)
  public void listSubjects(HttpServerRequest request) {
    final String structureId = request.params().get("structureId");
    final List<String> teachers = request.params().getAll("teacherId");
    final boolean classes = request.params().contains("classes");
    final boolean groups = request.params().contains("groups");

    timetableService.listSubjects(structureId, teachers, classes, groups, arrayResponseHandler(request));
  }

  @Get("/timetable/subjects/:structureId/group")
  @ApiDoc("Get subject list of the structure by external group id.")
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @ResourceFilter(UserInStructure.class)
  public void listSubjectsByGroup(HttpServerRequest request) {
    final String structureId = request.params().get("structureId");
    final String externalGroupId = request.params().get("externalGroupId");

    timetableService.listSubjectsByGroup(structureId, externalGroupId, arrayResponseHandler(request));
  }

  @Put("/timetable/init/:structureId")
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @ResourceFilter(AdmlOfStructureWithoutEDTInit.class)
  @MfaProtected()
  public void initStructure(final HttpServerRequest request) {
    RequestUtils.bodyToJson(request, pathPrefix + "initTimetable", new Handler<JsonObject>() {
      @Override
      public void handle(JsonObject conf) {
        timetableService.initStructure(request.params().get("structureId"), conf, notEmptyResponseHandler(request));
      }
    });
  }

  @Get("/timetable/classes/:structureId")
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @ResourceFilter(AdmlOfStructure.class)
  @MfaProtected()
  public void classesMapping(final HttpServerRequest request) {
    timetableService.classesMapping(request.params().get("structureId"), defaultResponseHandler(request));
  }

  @Put("/timetable/classes/:structureId")
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @ResourceFilter(AdmlOfStructure.class)
  @MfaProtected()
  public void updateClassesMapping(final HttpServerRequest request) {
    RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
      @Override
      public void handle(JsonObject mapping) {
        timetableService.updateClassesMapping(request.params().get("structureId"), mapping, defaultResponseHandler(request));
      }
    });
  }

  @Get("/timetable/groups/:structureId")
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @ResourceFilter(AdmlOfStructure.class)
  @MfaProtected()
  public void groupsMapping(final HttpServerRequest request) {
    timetableService.groupsMapping(request.params().get("structureId"), defaultResponseHandler(request));
  }

  @Put("/timetable/groups/:structureId")
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @ResourceFilter(AdmlOfStructure.class)
  @MfaProtected()
  public void updateGroupsMapping(final HttpServerRequest request) {
    RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
      @Override
      public void handle(JsonObject mapping) {
        timetableService.updateGroupsMapping(request.params().get("structureId"), mapping, defaultResponseHandler(request));
      }
    });
  }

  @Delete("/timetable/import/progress")
  @ResourceFilter(SuperAdminFilter.class)
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @MfaProtected()
  public void clearImportInProgress(final HttpServerRequest request) {
    this.importInProgress.clear();
    Renders.ok(request);
  }

  @Post("/timetable/import/:structureId")
  @ResourceFilter(AdminFilter.class)
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @MfaProtected()
  public void importTimetable(final HttpServerRequest request) {
    String structAttr = request.params().get("structAttr");
    String setReportAsAutomatic = request.params().get("setReportAsAutomatic");
    boolean isUAI = structAttr == null ? false : structAttr.toLowerCase().equals("uai");
    boolean reportAsAutomatic = setReportAsAutomatic == null ? false : setReportAsAutomatic.equals("true");
    this.receiveTimetableFile(request, request.params().get("structureId"), null, false, isUAI, false, false, reportAsAutomatic);
  }

  @Post("/timetable/import/:timetableType/:structureId")
  @ResourceFilter(AdminFilter.class)
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @MfaProtected()
  public void importSpecificTimetable(final HttpServerRequest request) {
    String structAttr = request.params().get("structAttr");
    String setReportAsAutomatic = request.params().get("setReportAsAutomatic");
    boolean isUAI = structAttr == null ? false : structAttr.toLowerCase().equals("uai");
    boolean reportAsAutomatic = setReportAsAutomatic == null ? false : setReportAsAutomatic.equals("true");
    this.receiveTimetableFile(request, request.params().get("structureId"), request.params().get("timetableType"), true, isUAI, false, false, reportAsAutomatic);
  }

  @Post("/timetable/import/groups/:structureId")
  @ResourceFilter(AdminFilter.class)
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @MfaProtected()
  public void importTimetableGroupsOnly(final HttpServerRequest request) {
    String structAttr = request.params().get("structAttr");
    String setReportAsAutomatic = request.params().get("setReportAsAutomatic");
    boolean isUAI = structAttr != null && structAttr.equalsIgnoreCase("uai");
    boolean reportAsAutomatic = setReportAsAutomatic != null && setReportAsAutomatic.equals("true");
    this.receiveTimetableFile(request, request.params().get("structureId"), null, false, isUAI, false, true, reportAsAutomatic);
  }

  private void receiveTimetableFile(final HttpServerRequest request, String structureIdentifier, String timetableType, boolean timetableMode,
                                    boolean identifierIsUAI, boolean feederImport, boolean groupsOnly, boolean setReportAsAutomatic) {
    if (importInProgress.containsKey(structureIdentifier)) {
      badRequest(request, "timetable.import.exists");
      return;
    }
    request.setExpectMultipart(true);

    importInProgress.put(structureIdentifier, System.currentTimeMillis());
    request.pause();
    final String importId = UUID.randomUUID().toString();
    final String path = config.getString("timetable-path", "/tmp") + File.separator + importId;
    request.exceptionHandler(new Handler<Throwable>() {
      @Override
      public void handle(Throwable event) {
        importInProgress.remove(structureIdentifier);
        badRequest(request, event.getMessage());
        deleteImportPath(vertx, path);
      }
    });
    request.uploadHandler(upload -> {
      final String filename = path + File.separator + upload.filename();
      upload.streamToFileSystem(filename).onComplete(event -> {
        Handler<Either<JsonObject, JsonObject>> hnd = result -> {
          importInProgress.remove(structureIdentifier);
          reportResponseHandler(vertx, path, request).handle(result);
        };

        if (feederImport != true) {
          timetableService.importTimetable(structureIdentifier, filename,
              getHost(request), I18n.acceptLanguage(request),
              identifierIsUAI, timetableType, groupsOnly, timetableMode,
              setReportAsAutomatic, hnd);
        } else {
          timetableService.feederPronote(structureIdentifier, filename,
              getHost(request), I18n.acceptLanguage(request),
              identifierIsUAI, setReportAsAutomatic,
              hnd);
        }
      });
      request.resume();
    });
    vertx.fileSystem().mkdir(path, new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> event) {
        if (event.succeeded()) {
          request.resume();
        } else {
          importInProgress.remove(structureIdentifier);
          badRequest(request, "mkdir.error");
        }
      }
    });
  }

  @Post("/timetable/feeder/pronote/:structureId")
  @ResourceFilter(AdminFilter.class)
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @MfaProtected()
  public void launchPronoteImport(final HttpServerRequest request) {
    String structAttr = request.params().get("structAttr");
    String setReportAsAutomatic = request.params().get("setReportAsAutomatic");
    boolean isUAI = structAttr == null ? false : structAttr.toLowerCase().equals("uai");
    boolean reportAsAutomatic = setReportAsAutomatic == null ? false : setReportAsAutomatic.equals("true");
    this.receiveTimetableFile(request, request.params().get("structureId"), null, false, isUAI, true, false, reportAsAutomatic);
  }

  @Get("/timetable/import/:structureId/reports")
  @ResourceFilter(AdminFilter.class)
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @MfaProtected()
  public void listReports(final HttpServerRequest request) {
    timetableService.listReports(request.params().get("structureId"), arrayResponseHandler(request));
  }

  @Get("/timetable/import/:structureId/report/:reportId")
  @ResourceFilter(AdminFilter.class)
  @SecuredAction(value = "", type = ActionType.RESOURCE)
  @MfaProtected()
  public void getReport(final HttpServerRequest request) {
    timetableService.getReport(request.params().get("structureId"), request.params().get("reportId"), defaultResponseHandler(request));
  }

  @BusAddress("timetable")
  @SuppressWarnings("unchecked")
  public void getTimetable(final Message<JsonObject> message) {
    final String action = message.body().getString("action");

    if (action == null) {
      log.warn("[@BusAddress](timetable) Invalid action.");
      message.reply(new JsonObject().put("status", "error")
          .put("message", "Invalid action."));
      return;
    }

    final String structureId = message.body().getString("structureId");

    switch (action) {
      case "get.course":
        final String teacherId = message.body().getString("teacherId");
        final List<String> groupNames = message.body().getJsonArray("group", new JsonArray()).getList();
        final String beginDate = message.body().getString("begin");
        final String endDate = message.body().getString("end");

        if (beginDate != null && endDate != null &&
            beginDate.matches("\\d{4}-\\d{2}-\\d{2}") && endDate.matches("\\d{4}-\\d{2}-\\d{2}")) {

          timetableService.listCoursesBetweenTwoDates(structureId, teacherId, groupNames, beginDate, endDate, getBusResultHandler(message));
        } else {
          message.reply(new JsonObject()
              .put("status", "error")
              .put("message", "timetable.invalid.dates"));
        }
        break;
      case "get.subjects":
        final List<String> teachers = message.body().getJsonArray("teacherIds", new JsonArray()).getList();
        final String externalGroupId = message.body().getString("externalGroupId");
        final boolean classes = message.body().getBoolean("classes", false);
        final boolean groups = message.body().getBoolean("groups", false);

        if (StringUtils.isEmpty(externalGroupId)) {
          timetableService.listSubjects(structureId, teachers, classes, groups, getBusResultHandler(message));
        } else {
          timetableService.listSubjectsByGroup(structureId, externalGroupId, getBusResultHandler(message));
        }

        break;
      default:
        message.reply(new JsonObject().put("status", "error")
            .put("message", "Invalid action."));
        break;
    }
  }

  private Handler<Either<String, JsonArray>> getBusResultHandler(final Message<JsonObject> message) {
    return new Handler<Either<String, JsonArray>>() {
      @Override
      public void handle(Either<String, JsonArray> result) {
        if (result.isRight()) {
          message.reply(new JsonObject()
              .put("status", "ok")
              .put("results", result.right().getValue()));
        } else {
          message.reply(new JsonObject()
              .put("status", "error")
              .put("message", result.left().getValue()));
        }
      }
    };
  }

  public void setTimetableService(TimetableService timetableService) {
    this.timetableService = timetableService;
  }

}
