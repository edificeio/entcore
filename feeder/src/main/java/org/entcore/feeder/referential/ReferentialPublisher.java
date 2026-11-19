package org.entcore.feeder.referential;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerPublisher;

/** Notifications of the feeder to referential (external service: "ext." subject). */
public interface ReferentialPublisher {

	/** Entities touched by a successful manual action, for a targeted resync. */
	@BrokerPublisher(subject = "ext.referential.entity.changed")
	Future<Void> entityChanged(EntityChangedDTO changes);
}
