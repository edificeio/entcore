package org.entcore.common.utils;

import io.vertx.core.json.JsonArray;

import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

  public static <T> Set<T> toSet(final JsonArray array,
                                          final Class<T> arrayTypeClass) {
    return array.stream()
      .map(arrayTypeClass::cast)
      .collect(Collectors.toSet());
  }

  public static <T> List<T> toList(final JsonArray array,
                                   final Class<T> arrayTypeClass) {
    return array.getList();
  }
}
