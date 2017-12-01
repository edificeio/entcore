package org.entcore.admin;

import org.entcore.admin.controllers.AdminController;
import org.entcore.admin.controllers.PlateformeInfoController;
import org.entcore.common.http.BaseServer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.eventbus.DeliveryOptions;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class Admin extends BaseServer {

	 @Override
	 public void start() throws Exception {
		 super.start();
		 
		 addController(new AdminController());
		 
		 final PlateformeInfoController plateformeInfoController = new PlateformeInfoController();
		 
		 // check if sms module activated
		 String smsAddress = "";
		 String smsProvider = "";
		 LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		 if(server != null && server.get("smsProvider") != null) {
			 smsProvider = (String) server.get("smsProvider");
			 final String node = (String) server.get("node");
			 smsAddress = (node != null ? node : "") + "entcore.sms";
		 } else {
			 smsAddress = "entcore.sms";
		 }
		 
		 JsonObject pingAction = new JsonObject()
				 .put("provider", smsProvider)
				 .put("action", "ping");
		 
		 vertx.eventBus().send(smsAddress, pingAction, new DeliveryOptions().setSendTimeout(5000l),
				 new Handler<AsyncResult<Message<JsonObject>>>() {
					 @Override
					 public void handle(AsyncResult<Message<JsonObject>> res) {
						 if (res != null && res.succeeded()) {
							 if ("ok".equals(res.result().body().getString("status"))) {
								 plateformeInfoController.setSmsModule(true);
							 }
						 }
						 addController(plateformeInfoController);
					 }
				 }
		 );
	 }

}
