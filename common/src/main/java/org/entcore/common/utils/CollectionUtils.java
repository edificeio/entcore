package org.entcore.common.utils;

import io.vertx.core.json.JsonArray;

import java.util.stream.Collector;

public final class CollectionUtils {
  public static <T> Collector<T, ?, JsonArray> toJsonArray() {
    return Collector.of(
        JsonArray::new,
        JsonArray::add,
        (left, right) -> {
          left.addAll(right);
          return left;
        },
        Collector.Characteristics.UNORDERED
    );
  }
}
