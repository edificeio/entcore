package org.entcore.broker.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;

public class BrokerController extends BaseController {

  @Get("/neverused")
  @SecuredAction("broker.neverused")
  public void neverused(HttpServerRequest request) {

  }
}
