package org.entcore.directory.referential;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerPublisher;

import java.util.Map;

/** Notifications of the directory to referential (referential POC, step 4 in fallback mode B, "ext." subject). */
public interface ReferentialPublisher {

	/** Entities touched by a successful write, same JSON as the feeder's EntityChangedDTO (see PositionChanges). */
	@BrokerPublisher(subject = "ext.referential.entity.changed")
	Future<Void> entityChanged(Map<String, Object> changes);
}
