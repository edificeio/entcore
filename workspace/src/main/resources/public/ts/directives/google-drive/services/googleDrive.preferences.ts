import { Me, notify } from "entcore";
import { ViewMode } from "../enums/viewMode.enum";

export type GoogleDrivePreference = {
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
      let preference = await Me.preference("google-drive");
      if (this.isEmpty(preference) || !preference.viewMode) {
        preference.viewMode = ViewMode.ICONS;
        await this.updatePreference(preference);
      }
      this.setProperties(preference);
    } catch (e) {
      notify.error("google-drive.preferences.init.error");
      throw e;
    }
  }

  async updatePreference(preference: GoogleDrivePreference): Promise<void> {
    Me.preferences["google-drive"] = preference;
    try {
      await Me.savePreference("google-drive");
      this.setProperties(preference);
    } catch (e) {
      notify.error("google-drive.preferences.updatepreference.error");
      throw e;
    }
  }

  private setProperties(preference: GoogleDrivePreference): void {
    this._viewMode = preference.viewMode;
  }

  private isEmpty(preference: GoogleDrivePreference): boolean {
    return !preference || !Object.keys(preference).length;
  }
}
