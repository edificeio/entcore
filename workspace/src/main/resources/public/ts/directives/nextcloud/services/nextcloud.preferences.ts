import { Me, notify } from "entcore";
import { ViewMode } from "../enums/viewMode.enum";

export type NextcloudPreference = {
  viewMode: ViewMode;
};

export class Preference {
  private _viewMode: ViewMode;

  get viewMode(): ViewMode {
    return this._viewMode;
  }

  set viewMode(viewMode: ViewMode) {
    this._viewMode = viewMode;
  }

  async init(): Promise<void> {
    try {
      // fetch nextcloud preference from Me
      let preference = await Me.preference("nextcloud");
      // Check on nextcloud preferences AND nextcloud viewMode preferences
      if (this.isEmpty(preference) || !preference.viewMode) {
        // If no view mode, we set it with default value
        preference.viewMode = ViewMode.ICONS;
        // persist for the first time my nextcloud preference
        await this.updatePreference(preference);
      }
      this.setProperties(preference);
    } catch (e) {
      notify.error("nextcloud.preferences.init.error");
      throw e;
    }
  }

  async updatePreference(preference: NextcloudPreference): Promise<void> {
    Me.preferences.nextcloud = preference;
    try {
      await Me.savePreference("nextcloud");
      this.setProperties(preference);
    } catch (e) {
      notify.error("nextcloud.preferences.updatepreference.error");
      throw e;
    }
  }

  private setProperties(preference: NextcloudPreference): void {
    this._viewMode = preference.viewMode;
  }

  private isEmpty(preference: NextcloudPreference): boolean {
    return !preference || !Object.keys(preference).length;
  }
}
