package org.entcore.conversation.service.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

import java.util.List;

import static org.entcore.common.postgres.PostgresClient.toJson;

public final class ReactiveSql {

  private static final Logger logger = LoggerFactory.getLogger(ReactiveSql.class);

  public static void validUniqueResult(RowSet<Row> r, Handler<Either<String, JsonObject>> resultHandler) {
    try {
      final Either<String, JsonObject> validatedResults;
      final int rowCount = r.rowCount();
      if (rowCount == 0) {
        validatedResults = new Either.Right<>(new JsonObject());
      } else if (r.size() > 1) {
        validatedResults = new Either.Left<>("non.unique.result");
      } else {
        final JsonObject jsonResult;
        if(r.size() > 0) {
          jsonResult = toJson(r.iterator().next());
        } else {
          // We just updated something and got no results back
          jsonResult = new JsonObject().put("updated", r.rowCount());
        }
        validatedResults = new Either.Right<>(jsonResult);
      }
      resultHandler.handle(validatedResults);
    } catch (Exception e) {
      logger.error("An error occurred while validating results", e);
      resultHandler.handle(new Either.Left<>(e.getMessage()));
    }
  }

  public static void validMultipleResults(RowSet<Row> r, Handler<Either<String, JsonArray>> results) {
    try {
      final JsonArray array = new JsonArray();
      for (Row row : r) {
        array.add(toJson(row));
      }
      results.handle(new Either.Right<>(array));
    } catch (Exception e) {
      logger.error("An error occurred while validating results", e);
      results.handle(new Either.Left<>(e.getMessage()));
    }
  }


  public static  void validMultipleResults(AsyncResult<RowSet<Row>> r, Handler<Either<String, JsonArray>> result) {
    if(r.succeeded()) {
      validMultipleResults(r.result(), result);
    } else {
      result.handle(new Either.Left<>(r.cause().getMessage()));
    }
  }

  public static void validListOfMultipleResults(AsyncResult<List<RowSet<Row>>> r, Handler<Either<String, JsonArray>> result) {
    try {
      if(r.failed()) {
        result.handle(new Either.Left<>(r.cause().getMessage()));
      } else {
        final JsonArray array = new JsonArray();
        final List<RowSet<Row>> listOfRows = r.result();
        for (RowSet<Row> rows : listOfRows) {
          for (Row row : rows) {
            array.add(toJson(row));
          }
        }
        result.handle(new Either.Right<>(array));
      }
    } catch (Exception e) {
      logger.error("An error occurred while validating results", e);
      result.handle(new Either.Left<>(e.getMessage()));
    }
  }
  

  public static <T> String listPrepared(Iterable<T> arguments) {
    return listPrepared(arguments, 1);
  }

  public static <T> String listPrepared(Iterable<T> arguments, final int startIndex) {
    StringBuilder sb = new StringBuilder("(");
    if (arguments != null && arguments.iterator().hasNext()) {
      int i = startIndex;
      for (T ignored : arguments) {
        sb.append('$');
        sb.append(i++);
        sb.append(',');
      }
      sb.deleteCharAt(sb.length() - 1);
    }
    return sb.append(')').toString();
  }

  public static void validUniqueResult(AsyncResult<RowSet<Row>> r, Handler<Either<String, JsonObject>> result) {
    if(r.succeeded()) {
      validUniqueResult(r.result(), result);
    } else {
      result.handle(new Either.Left<>(r.cause().getMessage()));
    }
  }

}
