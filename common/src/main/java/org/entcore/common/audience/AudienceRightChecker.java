package org.entcore.common.audience;

import io.vertx.core.Future;
import org.entcore.common.audience.to.AudienceCheckRightRequestMessage;

import java.util.function.Function;

public interface AudienceRightChecker extends Function<AudienceCheckRightRequestMessage, Future<Boolean>> {
}
