package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.appregistry.AppRegistrationRequestDTO;
import org.entcore.broker.api.appregistry.AppRegistrationResponseDTO;

public interface AppRegistryProxy {
  @BrokerListener(subject = "ent.appregistry.app.register", proxy = true, description = "Register a starting app to ENT")
  Future<AppRegistrationResponseDTO> registerApp(final AppRegistrationRequestDTO request);
  @BrokerListener(subject = "ent.{application}.test", proxy = true, description = "Register a starting app to ENT")
  Future<AppRegistrationResponseDTO> testApplication(final AppRegistrationRequestDTO request);
}
