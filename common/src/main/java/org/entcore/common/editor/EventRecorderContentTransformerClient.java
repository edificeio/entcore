package org.entcore.common.editor;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStore;

import java.util.*;
import java.util.stream.Collectors;

public class EventRecorderContentTransformerClient implements IContentTransformerClient {
  private final IContentTransformerClient client;
  private final EventStore eventStore;
  private final String module;
  public static final String EVENT = "EDITOR_CONTENT";

  private static final Map<String, String> tagsToMultimediaCount = new HashMap<>();
  private static final Set<String> customCounts = new HashSet<>();

  static {
    tagsToMultimediaCount.put("custom-image", "nb_images");
    tagsToMultimediaCount.put("video", "nb_videos");
    tagsToMultimediaCount.put("audio", "nb_sounds");
    tagsToMultimediaCount.put("iframe", "nb_embedded");
    customCounts.add("nb_attachments");
    customCounts.add("nb_external_links");
    customCounts.add("nb_internal_links");
    customCounts.add("nb_formulae");
  }

  public EventRecorderContentTransformerClient(final IContentTransformerClient client,
                                               final EventStore eventStore,
                                               final String module) {
    this.client = client;
    this.eventStore = eventStore;
    this.module = module;
  }

  @Override
  public Future<ContentTransformerResponse> transform(final ContentTransformerRequest request,
                                                      final HttpServerRequest httpCallerRequest) {
    return client.transform(request, httpCallerRequest)
      .onSuccess(transformed -> {
        // Do not count events when we only want to migrate old content
        if(!HttpMethod.GET.equals(httpCallerRequest.method())) {
          JsonObject json = transformed.getCleanJson();
          if (json == null) {
            json = transformed.getJsonContent();
          }
          final Map<String, Integer> occurrences = computeMultimediaOccurrences(json);
          if (occurrences != null) {
            final JsonObject customAttributes = new JsonObject()
              .put("override-module", module);
            for (Map.Entry<String, Integer> entry : occurrences.entrySet()) {
              customAttributes.put(entry.getKey(), entry.getValue());
            }
            eventStore.createAndStoreEvent(EVENT, httpCallerRequest, customAttributes);
          }
        }
      });
  }

  public static Map<String, Integer> computeMultimediaOccurrences(final JsonObject json) {
    final Map<String, Integer> occurrences = new HashMap<>();
    for (String attributeName : tagsToMultimediaCount.values()) {
      occurrences.put(attributeName, 0);
    }
    for (String customCount : customCounts) {
      occurrences.put(customCount, 0);
    }
    return computeMultimediaOccurrences(json, occurrences);
  }

  private static Map<String, Integer> computeMultimediaOccurrences(final JsonObject json, final Map<String, Integer> occurrences) {
    if(json != null) {
      final String type = json.getString("type");
      if("attachments".equalsIgnoreCase(type)) {
        final int nbAttachments = computeNbAttachements(json);
        occurrences.compute("nb_attachments", (k, v) -> v + nbAttachments);
      } else if ("linker".equalsIgnoreCase(type)) {
        occurrences.compute("nb_internal_links", (k, v) -> v + 1);
      } else if("text".equalsIgnoreCase(type)) {
        final String text = json.getString("text", "").trim();
        if(text.length() >= 2 && text.charAt(0) == '$' && text.charAt(text.length() - 1) == '$') {
          occurrences.compute("nb_formulae", (k, v) -> v == null ? 1 : v + 1);
        }
        if(json.containsKey("marks")) {
          final List<String> hrefs = json.getJsonArray("marks").stream()
            .map(mark -> ((JsonObject) mark))
            .filter(mark -> "hyperlink".equalsIgnoreCase(mark.getString("type", "")))
            .map(mark -> mark.getJsonObject("attrs").getString("href"))
            .collect(Collectors.toList());
          final int nbInt = (int) hrefs.stream().filter(href -> href.charAt(0) == '/').count();
          final int nbExt = hrefs.size() - nbInt;
          occurrences.compute("nb_internal_links", (k, v) -> v + nbInt);
          occurrences.compute("nb_external_links", (k, v) -> v + nbExt);
        }
      } else if(tagsToMultimediaCount.containsKey(type)) {
        occurrences.compute(tagsToMultimediaCount.get(type), (k, v) -> v + 1);
      }
      if(json.containsKey("content")) {
        for (Object content : json.getJsonArray("content")) {
          final JsonObject child = (JsonObject) content;
          computeMultimediaOccurrences(child, occurrences);
        }
      }
    }
    return occurrences;
  }

  private static int computeNbAttachements(JsonObject json) {
    return json.getJsonObject("attrs").getJsonArray("links").size();
  }
}
