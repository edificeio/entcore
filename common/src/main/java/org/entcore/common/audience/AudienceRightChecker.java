package org.entcore.common.audience;

import io.vertx.core.Future;
import org.entcore.common.audience.to.AudienceCheckRightRequestMessage;

import java.util.function.Function;

public interface AudienceRightChecker extends Function<AudienceCheckRightRequestMessage, Future<Boolean>> {

  @Override
  default <V> Function<V, Future<Boolean>> compose(Function<? super V, ? extends AudienceCheckRightRequestMessage> before) {
    return Function.super.compose(before);
  }

  @Override
  default <V> Function<AudienceCheckRightRequestMessage, V> andThen(Function<? super Future<Boolean>, ? extends V> after) {
    return Function.super.andThen(after);
  }
}
