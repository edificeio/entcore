package org.entcore.common.migration;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.beans.Transient;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class AppMigrationConfiguration {
  private final boolean enabled;
  private final boolean writeNew;
  private final boolean writeLegacy;
  private final boolean readNew;
  private final boolean readLegacy;
  private final Set<String> availableReadActions;
  public static final AppMigrationConfiguration DISABLED = new AppMigrationConfiguration(
      false, BrokerSwitchType.READ_LEGACY_WRITE_LEGACY, Collections.emptySet());

  /**
   *
   * @param switchType The type of broker switch to determine read/write behavior. Dependiong on the supplied value, it will :<ul>
   *                         <li>READ_LEGACY_WRITE_BOTH: Read from legacy, write to both new and legacy.</li>
   *                         <li>READ_LEGACY_WRITE_LEGACY: Read from legacy, write to legacy only.</li>
   *                         <li>READ_NEW_WRITE_BOTH: Read from new service iff the action is in {@code availableReadActionsNewService} otherwise read from legacy, write to both new and legacy.</li>
   *                         <li>READ_NEW_WRITE_NEW: Read from new service and never from legacy, write to new service only.</li>
   * </ul>
   *
   * @param availableReadActions Set of read only actions for which the new service is available.
   */
  public AppMigrationConfiguration(boolean enabled, BrokerSwitchType switchType, Set<String> availableReadActions) {
    this.enabled = enabled;
    this.availableReadActions = availableReadActions;
    switch (switchType) {
      case READ_LEGACY_WRITE_BOTH:
        this.writeNew = true;
        this.writeLegacy = true;
        this.readNew = false;
        this.readLegacy = true;
        break;
      case READ_LEGACY_WRITE_LEGACY:
        this.writeNew = false;
        this.writeLegacy = true;
        this.readNew = false;
        this.readLegacy = true;
        break;
      case READ_NEW_WRITE_BOTH:
        this.writeNew = true;
        this.writeLegacy = true;
        this.readNew = true;
        this.readLegacy = false;
        break;
      case READ_NEW_WRITE_NEW:
        this.writeNew = true;
        this.writeLegacy = false;
        this.readNew = true;
        this.readLegacy = false;
        break;
      default:
        throw new IllegalArgumentException("Invalid BrokerSwitchType: " + switchType);
    }
  }
  public static AppMigrationConfiguration fromJson(final JsonObject brokerConfig) {
    if (brokerConfig != null && brokerConfig.getBoolean("enabled", false)) {
      final BrokerSwitchType switchType = BrokerSwitchType.valueOf(
          brokerConfig.getString("switch-type", BrokerSwitchType.READ_LEGACY_WRITE_LEGACY.name()));
      final Set<String> availableReadActions = brokerConfig.getJsonArray("available-read-actions", new JsonArray())
          .stream()
          .map(o -> (String)o)
          .collect(Collectors.toSet());
      return new AppMigrationConfiguration(true, switchType, availableReadActions);
    } else {
      return AppMigrationConfiguration.DISABLED;
    }
  }

  public static AppMigrationConfiguration fromVertx(final String migrationName) {
    Context context = Vertx.currentContext();
    final Vertx vertx;
    if(context == null) {
      vertx = context.owner();
    } else {
      vertx = Vertx.vertx();
    }
    return fromVertx(migrationName, vertx, null);
  }

  public static AppMigrationConfiguration fromVertx(final String migrationName, final Vertx vertx, final JsonObject defaultConfiguration) {
    JsonObject config = (JsonObject) vertx.sharedData().getLocalMap("server").get("app-migration");
    if(config == null) {
      config = defaultConfiguration;
    }
    if(config == null) {
      config = new JsonObject();
    }
    return AppMigrationConfiguration.fromJson(config.getJsonObject(migrationName));
  }

  public boolean isWriteNew() {
    return writeNew;
  }

  public boolean isWriteLegacy() {
    return writeLegacy;
  }

  public boolean isReadNew() {
    return readNew;
  }

  public boolean isReadLegacy() {
    return readLegacy;
  }

  public Set<String> getAvailableReadActions() {
    return availableReadActions;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Transient
  public boolean isReadEnabled(String actionName) {
    return enabled && readNew && availableReadActions != null && availableReadActions.contains(actionName);
  }
}
