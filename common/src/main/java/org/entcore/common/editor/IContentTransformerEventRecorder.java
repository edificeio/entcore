package org.entcore.common.editor;

import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.http.HttpServerRequest;

/**
 * A service that records details about the result of the transformation of rich content.
 */
@FunctionalInterface
public interface IContentTransformerEventRecorder {
  /**
   * Record details about the result of the transformation of rich content.
   * @param id id of the transformed resource
   * @param resourceType type of the transformed resource
   * @param response Response from the content transformer service
   * @param httpCallerRequest Original request that triggered the transformation
   */
  void recordTransformation(final String id,
                            final String resourceType,
                            final ContentTransformerResponse response,
                            final HttpServerRequest httpCallerRequest);
  /** Dummy implementation that does not record anything.*/
  IContentTransformerEventRecorder noop = (id, type, response, httpCallerRequest) -> {
  };
}
