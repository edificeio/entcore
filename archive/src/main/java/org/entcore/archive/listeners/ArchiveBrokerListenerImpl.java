package org.entcore.archive.listeners;

import fr.wseduc.webutils.security.RSA;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import org.entcore.broker.api.dto.archive.FinalizeManifestRequestDTO;
import org.entcore.broker.api.dto.archive.FinalizeManifestResponseDTO;
import org.entcore.broker.api.dto.archive.VerifySignatureRequestDTO;
import org.entcore.broker.api.dto.archive.VerifySignatureResponseDTO;
import org.entcore.broker.proxy.ArchiveBrokerListener;
import org.entcore.common.archive.ArchiveConstants;
import org.entcore.common.archive.ArchiveFolder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Manifest and signature operations exposed by archive module
 * <p>
 * Same algorithm/key as {@code FileSystemExportService.signExport}/
 * {@code DefaultImportService.verifyImport} 
 * <p>
 * Expected one app per subfolder:
 * <pre>
 * {prefix}/
 *   Manifest.json
 *   archive.signature
 *   Blog/
 *     post_My post_abc123        (resource file)
 *     Documents/
 *       image_def456.png         (attachment)
 * </pre>
 */
public class ArchiveBrokerListenerImpl implements ArchiveBrokerListener {

  private final Vertx vertx;
  private final ArchiveFolder folder;
  private final PrivateKey signKey;
  private final PublicKey verifyKey;
  private final boolean forceEncryption;

  public ArchiveBrokerListenerImpl(Vertx vertx, ArchiveFolder folder, PrivateKey signKey,
      PublicKey verifyKey, boolean forceEncryption) {
    this.vertx = vertx;
    this.folder = folder;
    this.signKey = signKey;
    this.verifyKey = verifyKey;
    this.forceEncryption = forceEncryption;
  }

  /**
   * Writes `Manifest.json` (and, if a signing key is configured, `archive.signature`)
   * for every app folder already written under {@code request.getPrefix()}.
   * <p>
   * Called once, after every app has finished exporting its own resources into the job's
   * prefix.
   * <p>
   * Example: for a job prefix `job42` already containing
   * <pre>
   * job42/
   *   Blog/
   *     post_x
   *     Documents/
   *       img_y
   * </pre>
   * writes {@code job42/Manifest.json}:
   * <pre>
   * {
   *   "org.entcore.blog": {
   *     "version": "1.2.3",
   *     "folder": "Blog"
   *   }
   * }
   * </pre>
   * ("org.entcore.blog" is the app's technical key, resolved by {@link #resolveManifestEntries};
   * "Blog" is the real folder name observed on S3)
   * then signs it for every app folder into `job42/archive.signature`.
   *
   * @param request the job's prefix
   * @return whether the manifest, and the signature, were written
   */
  @Override
  public Future<FinalizeManifestResponseDTO> finalizeManifest(FinalizeManifestRequestDTO request) {
    if (request == null || !request.isValid()) {
      return Future.failedFuture("request.parameters.invalid");
    }
    final String prefix = request.getPrefix();
    return folder.listKeys(prefix).compose(keys -> {
      final Map<String, List<String>> byApp = groupByApp(prefix, keys);
      return resolveManifestEntries(byApp.keySet()).compose(manifestEntries -> {
        final JsonObject manifest = new JsonObject();
        for (String appFolder : byApp.keySet()) {
          final ManifestEntry entry = manifestEntries.get(appFolder);
          final String manifestKey = entry != null ? entry.technicalKey : appFolder;
          final String version = entry != null ? entry.version : "0";
          manifest.put(manifestKey, new JsonObject().put("version", version).put("folder", appFolder));
        }
        final Buffer manifestBuffer = manifest.toBuffer();
        final Future<String> writeManifest = folder.writeFile(prefix + "/" + ArchiveConstants.MANIFEST_NAME, manifestBuffer, "application/json");

        if (signKey == null) {
          if (forceEncryption) {
            return Future.failedFuture("No signing key");
          }
          return writeManifest.map(v -> new FinalizeManifestResponseDTO(true, true));
        }

        return writeManifest
            .compose(v -> signAllEntries(prefix, byApp, manifestBuffer))
            .compose(signatures -> folder.writeFile(prefix + "/" + ArchiveConstants.SIGNATURE_NAME, signatures.toBuffer(), "application/json"))
            .map(v -> new FinalizeManifestResponseDTO(true, true));
      });
    });
  }

  /**
   * Resolves, for each observed app folder, its real technical key and version - from the same
   * shared "versions" map DefaultImportService's minimum-import-version check reads.
   * <p>
   * The launcher fills it as module key -> version, e.g.
   * <pre>
   * "org.entcore.blog" -> "1.2.3"
   * </pre>
   * An app folder (e.g. "Blog" - a real v1 export names it after its i18n display name, not
   * its technical name) is matched by the segment after the last "." in the map's key,
   * case-insensitively:
   * <pre>
   * "org.entcore.blog".split(".")  ->  ["org", "entcore", "blog"]
   * last part                      ->  "blog"
   * "blog" matches folder "Blog"   ->  technicalKey = "org.entcore.blog", version = "1.2.3"
   * </pre>
   * The technical key, not the folder name, must be the manifest's own top-level key: v1's own
   * import ({@code DefaultImportService.parseFolders}) looks up the app's workflow by that same
   * technical name - using "Blog" there would make it silently skip the app.
   *
   * @param appFolders the observed app folder names
   * @return each app folder mapped to its technical key and version, for the ones that were found
   */
  private Future<Map<String, ManifestEntry>> resolveManifestEntries(Set<String> appFolders) {
    return vertx.sharedData().<String, String>getAsyncMap("versions")
        .compose(AsyncMap::entries)
        .map(entries -> {
          final Map<String, ManifestEntry> result = new HashMap<>();
          entries.forEach((technicalKey, version) -> {
            final int dot = technicalKey.lastIndexOf('.');
            final String appName = (dot >= 0 ? technicalKey.substring(dot + 1) : technicalKey).replace("-", "");
            for (String appFolder : appFolders) {
              if (appName.equalsIgnoreCase(appFolder)) {
                result.put(appFolder, new ManifestEntry(technicalKey, version));
              }
            }
          });
          return result;
        });
  }

  private static final class ManifestEntry {
    final String technicalKey;
    final String version;

    ManifestEntry(String technicalKey, String version) {
      this.technicalKey = technicalKey;
      this.version = version;
    }
  }

  /**
   * Verifies the job's signature (if any) and lists each app's resource filenames.
   * <p>
   * Called right after the uploaded archive is exploded into th prefix.
   * <p>
   * Example: for a job prefix `job42` already containing
   * <pre>
   * job42/
   *   Manifest.json
   *   archive.signature
   *   blog/
   *     post_x
   *     Documents/
   *       img_y
   * </pre>
   * returns `valid = true` (if archive.signature matches) and
   * <pre>
   * {
   *   "blog": ["post_x"]
   * }
   * </pre>
   * No signing key configured, or no archive.signature present: returns
   * `valid = !forceEncryption`, without checking anything.
   *
   * @param request the job's prefix
   * @return whether the signature is valid, and each app's resource filenames
   */
  @Override
  public Future<VerifySignatureResponseDTO> verifySignature(VerifySignatureRequestDTO request) {
    if (request == null || !request.isValid()) {
      return Future.failedFuture("request.parameters.invalid");
    }
    final String prefix = request.getPrefix();
    return folder.listKeys(prefix).compose(keys -> {
      final Map<String, List<String>> byApp = groupByApp(prefix, keys);
      final Map<String, List<String>> resourceIdsByApp = extractResourceFilenames(prefix, byApp);
      final boolean hasSignature = keys.contains(prefix + "/" + ArchiveConstants.SIGNATURE_NAME);

      if (verifyKey == null || !hasSignature) {
        return Future.succeededFuture(new VerifySignatureResponseDTO(!forceEncryption, resourceIdsByApp));
      }

      return folder.readFile(prefix + "/" + ArchiveConstants.SIGNATURE_NAME)
          .map(buf -> (JsonObject) new JsonObject(buf.toString()))
          .compose(signatures -> verifyAllEntries(prefix, byApp, signatures))
          .map(valid -> new VerifySignatureResponseDTO(valid, resourceIdsByApp));
    });
  }

  /**
   * Groups keys by app folder (first path segment under {@code prefix}).
   * <p>
   * {@code Manifest.json}/{@code archive.signature} are dropped.
   * <p>
   * Example: for keys
   * <pre>
   * job42/Manifest.json
   * job42/blog/post_x
   * job42/blog/Documents/img_y
   * </pre>
   * returns
   * <pre>
   * {
   *   "blog": [
   *     job42/blog/post_x,
   *     job42/blog/Documents/img_y
   *   ]
   * }
   * </pre>
   * <p>
   * The {@code TreeMap} is only for deterministic iteration (readable logs...)
   *
   * @param prefix the job's own prefix
   * @param keys every key found under {@code prefix}
   * @return each app's keys, by app folder name
   */
  private static Map<String, List<String>> groupByApp(String prefix, List<String> keys) {
    final Map<String, List<String>> byApp = new TreeMap<>();
    final String root = prefix + "/";
    for (String key : keys) {
      final String rel = key.substring(root.length());
      final int slash = rel.indexOf('/');
      if (slash < 0) {
        continue;
      }
      byApp.computeIfAbsent(rel.substring(0, slash), k -> new ArrayList<>()).add(key);
    }
    return byApp;
  }

  /**
   * Lists each app's direct children's filenames (the {@code Documents/} subfolder's is excluded).
   * <p>
   * Not resource ids: a filename is just the per-job tracking key. The real resource id
   * is read from the file's own {@code _id} field on import.
   * <p>
   * Example: for
   * <pre>
   * byApp = {
   *   "blog": [
   *     job42/blog/post_x,
   *     job42/blog/Documents/img_y
   *   ]
   * }
   * </pre>
   * returns
   * <pre>
   * {
   *   "blog": ["post_x"]
   * }
   * </pre>
   *
   * @param prefix the prefix
   * @param byApp each app's keys, as returned by {@link #groupByApp}
   * @return each app's resource filenames
   */
  private static Map<String, List<String>> extractResourceFilenames(String prefix, Map<String, List<String>> byApp) {
    final Map<String, List<String>> result = new TreeMap<>();
    for (Map.Entry<String, List<String>> entry : byApp.entrySet()) {
      final String appPrefix = prefix + "/" + entry.getKey() + "/";
      final List<String> names = new ArrayList<>();
      for (String key : entry.getValue()) {
        final String rel = key.substring(appPrefix.length());
        if (rel.indexOf('/') < 0) {
          names.add(rel);
        }
      }
      result.put(entry.getKey(), names);
    }
    return result;
  }

  /**
   * Signs Manifest.json and every app folder, for archive.signature:
   * <ul>
   *   <li>signs Manifest.json's own content, under key "Manifest.json"</li>
   *   <li>for each app: signs its ordered content streams (see {@link #collectAppStreams}), under key = app name</li>
   * </ul>
   *
   * @return each entry name (Manifest.json, then each app) mapped to its signature
   */
  private Future<JsonObject> signAllEntries(String prefix, Map<String, List<String>> byApp, Buffer manifestBuffer) {
    final JsonObject signatures = new JsonObject();
    Future<Void> chain = sign(Collections.singletonList(toInputStream(manifestBuffer)))
        .map(sig -> put(signatures, ArchiveConstants.MANIFEST_NAME, sig));
    for (Map.Entry<String, List<String>> entry : byApp.entrySet()) {
      final String app = entry.getKey();
      chain = chain.compose(v -> collectAppStreams(prefix, app, entry.getValue())
          .compose(this::sign)
          .map(sig -> put(signatures, app, sig)));
    }
    return chain.map(v -> signatures);
  }

  /**
   * Checks archive.signature against Manifest.json and every app folder:
   * <ul>
   *   <li>reads Manifest.json back and verifies it against signatures' "Manifest.json" entry - stops (false) if it fails</li>
   *   <li>otherwise, for each app: verifies its ordered content streams (see {@link #collectAppStreams}) against signatures' entry for that app name</li>
   * </ul>
   *
   * @return true only if every entry checks out
   */
  private Future<Boolean> verifyAllEntries(String prefix, Map<String, List<String>> byApp, JsonObject signatures) {
    return folder.readFile(prefix + "/" + ArchiveConstants.MANIFEST_NAME).compose(manifestBuffer -> {
      Future<Boolean> chain = verify(Collections.singletonList(toInputStream(manifestBuffer)), signatures.getString(ArchiveConstants.MANIFEST_NAME));
      for (Map.Entry<String, List<String>> entry : byApp.entrySet()) {
        chain = chain.compose(ok -> !ok ? Future.succeededFuture(false)
            : collectAppStreams(prefix, entry.getKey(), entry.getValue())
                .compose(streams -> verify(streams, signatures.getString(entry.getKey()))));
      }
      return chain;
    });
  }

  /**
   * Builds the ordered content streams for one app folder, matching exactly what
   * RSA.signFile would read off a real directory:
   * <ul>
   *   <li>push the folder's own name</li>
   *   <li>list its direct children - files and the Documents subfolder - together, sorted by name</li>
   *   <li>for each child: a file's content, or, for Documents, its name then each of its own files (sorted), in turn</li>
   * </ul>
   * <p>
   * Example: for app folder
   * <pre>
   * blog/
   *   post_x
   *   post_z
   *   Documents/
   *     img_y
   * </pre>
   * produces these streams, in order:
   * <pre>
   * name:    blog
   * name:    Documents      (sorts before "post_x", "post_z")
   * content: img_y
   * content: post_x
   * content: post_z
   * </pre>
   *
   * @param prefix the job's own prefix
   * @param app the app folder name
   * @param keys that app's keys, as returned by {@link #groupByApp}
   * @return the streams to feed to {@link RSA#signFile}/{@code verifyFile}, in order
   */
  private Future<List<InputStream>> collectAppStreams(String prefix, String app, List<String> keys) {
    final String appPrefix = prefix + "/" + app + "/";
    final SortedMap<String, String> resourceFiles = new TreeMap<>();
    final SortedMap<String, String> documentFiles = new TreeMap<>();
    for (String key : keys) {
      final String rel = key.substring(appPrefix.length());
      final int slash = rel.indexOf('/');
      if (slash < 0) {
        resourceFiles.put(rel, key);
      } else if (ArchiveConstants.DOCUMENTS_FOLDER.equals(rel.substring(0, slash))) {
        documentFiles.put(rel.substring(slash + 1), key);
      }
    }
    final SortedSet<String> topLevelNames = new TreeSet<>(resourceFiles.keySet());
    if (!documentFiles.isEmpty()) {
      topLevelNames.add(ArchiveConstants.DOCUMENTS_FOLDER);
    }

    final List<InputStream> streams = new ArrayList<>();
    streams.add(nameStream(app));
    Future<Void> chain = Future.succeededFuture();
    for (String name : topLevelNames) {
      if (ArchiveConstants.DOCUMENTS_FOLDER.equals(name)) {
        streams.add(nameStream(ArchiveConstants.DOCUMENTS_FOLDER));
        for (String key : documentFiles.values()) {
          chain = chain.compose(v -> folder.readFile(key).map(buf -> {
            streams.add(toInputStream(buf));
            return null;
          }));
        }
      } else {
        final String key = resourceFiles.get(name);
        chain = chain.compose(v -> folder.readFile(key).map(buf -> {
          streams.add(toInputStream(buf));
          return null;
        }));
      }
    }
    return chain.map(v -> streams);
  }

  private Future<String> sign(List<InputStream> streams) {
    return vertx.executeBlocking(() -> RSA.signFile(streams, signKey), false);
  }

  private Future<Boolean> verify(List<InputStream> streams, String signature) {
    if (signature == null) {
      return Future.succeededFuture(false);
    }
    return vertx.executeBlocking(() -> RSA.verifyFile(streams, signature, verifyKey), false);
  }

  private static InputStream toInputStream(Buffer buffer) {
    return new ByteArrayInputStream(buffer.getBytes());
  }

  private static InputStream nameStream(String name) {
    return new ByteArrayInputStream(name.getBytes(StandardCharsets.UTF_8));
  }

  private static Void put(JsonObject target, String key, String value) {
    target.put(key, value);
    return null;
  }
}
