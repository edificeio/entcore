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

package org.entcore.portal;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileProps;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

import java.io.File;
import java.util.List;

import org.entcore.broker.api.utils.AddressParameter;
import org.entcore.broker.api.utils.BrokerProxyUtils;
import org.entcore.common.cache.CacheService;
import org.entcore.common.http.BaseServer;
import org.entcore.portal.controllers.PortalController;
import org.entcore.portal.listeners.I18nBrokerListenerImpl;
import org.entcore.common.events.EventBrokerListenerImpl;

import fr.wseduc.webutils.collections.SharedDataHelper;

public class Portal extends BaseServer {

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		final Promise<Void> promise = Promise.promise();
		super.start(promise);
		promise.future().compose(x ->
			SharedDataHelper.getInstance().<String, JsonObject>getLocal("server", "skins")
		)
        .compose(this::initPortal)
		.onComplete(startPromise);
	}

    public Future<Void> initPortal(final JsonObject skins) {
        return Future.future(p -> {
            try {
                final String assetPath = config.getString("assets-path", "../..");
                final AddressParameter parameter = new AddressParameter("application", "portal");
                final CacheService cacheService = CacheService.create(vertx);
                BrokerProxyUtils.addBrokerProxy(new I18nBrokerListenerImpl(vertx, assetPath, cacheService), vertx, parameter);
                BrokerProxyUtils.addBrokerProxy(new EventBrokerListenerImpl(), vertx);
                addController(new PortalController(skins));
                registerGlobalWidgets(config.getString("widgets-path", config.getString("assets-path", ".") + "/assets/widgets"));
                p.complete();
            } catch (Exception e) {
                p.fail(e);
            }
        });
    }

	private void registerWidget(final String widgetPath){
		final String widgetName = new File(widgetPath).getName();
		JsonObject widget = new JsonObject()
				.put("name", widgetName)
				.put("js", "/assets/widgets/"+widgetName+"/"+widgetName+".js")
				.put("path", "/assets/widgets/"+widgetName+"/"+widgetName+".html");

		if(vertx.fileSystem().existsBlocking(widgetPath + "/i18n")){
			widget.put("i18n", "/assets/widgets/"+widgetName+"/i18n");
		}

		JsonObject message = new JsonObject()
				.put("widget", widget);
		vertx.eventBus().request("wse.app.registry.widgets", message, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				if("error".equals(event.body().getString("status"))){
					log.error("Error while registering widget "+widgetName+". "+event.body().getJsonArray("errors"));
					return;
				}
				log.info("Successfully registered widget "+widgetName);
			}
		}));
	}

	private void registerGlobalWidgets(String widgetsPath) {
		vertx.fileSystem().readDir(widgetsPath, new Handler<AsyncResult<List<String>>>() {
			public void handle(AsyncResult<List<String>> asyn) {
				if(asyn.failed()){
					log.error("Error while registering global widgets.", asyn.cause());
					return;
				}
				final List<String> paths = asyn.result();
				for(final String path: paths){
					vertx.fileSystem().props(path, new Handler<AsyncResult<FileProps>>() {
						public void handle(AsyncResult<FileProps> asyn) {
							if(asyn.failed()){
								log.error("Error while registering global widget " + path, asyn.cause());
								return;
							}
							if(asyn.result().isDirectory()){
								registerWidget(path);
							}
						}
					});
				}
			}
		});
	}

}
