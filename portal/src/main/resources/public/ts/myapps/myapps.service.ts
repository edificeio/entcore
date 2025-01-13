import { model, ui } from "entcore";
import { App } from "./myapps.types";
import http from "axios";

export interface UserAppsPreferenceResponse {
    preference: string;
}

export interface UserAppsPreference {
    bookmarks: Array<string>; 
    applications: Array<string>;
}

export class AppsService {
    private static readonly INSTANCE = new AppsService();

    private constructor() {}

    public static getInstance() {
        return AppsService.INSTANCE;
    }

    async getConnectorsThresold(): Promise<number> {
        const conf = await http.get('/conf/public');
        let threshold = conf.data["my-apps-connectors-threshold"];
        if (threshold == undefined) {
            threshold = 6;
        }
        return threshold;
    }

    async getApplicationsList(): Promise<Array<App>> {
        const applicationListResponse = await http.get('/applications-list');
        if (applicationListResponse.data) {
            return applicationListResponse.data.apps;
        }
        return null;
    }

    /**
     * sync user preference apps list with applications-list API:
     * - remove duplicates apps in user preference apps
     * - remove user preference apps that are not in applications-list API anymore
     * 
     * @param applicationsListFromAPI applications list from /applications-list API
     */
    async syncUserPrefAppsWith(applicationsListFromAPI: Array<App>): Promise<void> {
        // remove duplicates model.me.myApps.bookmarks
        model.me.myApps.bookmarks = Array.from(new Set(model.me.myApps.bookmarks));
        // remove duplicates model.me.myApps.apps
        model.me.myApps.applications = Array.from(new Set(model.me.myApps.applications));

        // WB-3745, do not overwrite preferences when applications list is obviously defective.
        if(!applicationsListFromAPI || applicationsListFromAPI.length < 1)
            return;

        const userAppsPrefResponse = await http.get('/userbook/preference/apps');
        if (userAppsPrefResponse.data && userAppsPrefResponse.data.preference) {
            const originalPrefs: UserAppsPreference = JSON.parse(userAppsPrefResponse.data.preference);
            const userPrefs: UserAppsPreference = JSON.parse(userAppsPrefResponse.data.preference);

            // remove duplicates bookmarks
            userPrefs.bookmarks = Array.from(new Set<string>(userPrefs.bookmarks));
            // remove duplicates apps
            userPrefs.applications = Array.from(new Set<string>(userPrefs.applications));

            // remove missing bookmarks from user prefs if not in application-list API
            const missingBookmarks: Array<string> = [];
            userPrefs.bookmarks.forEach(prefBookmark => {
                if (!applicationsListFromAPI.find(a => a.name === prefBookmark)) {
                    missingBookmarks.push(prefBookmark);
                }
            });
            if(missingBookmarks.length > 0) {
                userPrefs.bookmarks = userPrefs.bookmarks.filter(b => !missingBookmarks.find(m => m === b));
                // sync model.me.myApps lists too
                model.me.myApps.bookmarks = model.me.myApps.bookmarks.filter(b => !missingBookmarks.find(m => m === b));
            }

            // remove missing apps from user prefs if not in application-list API
            const missingApps: Array<string> = [];
            userPrefs.applications.forEach(prefApp => {
                if (!applicationsListFromAPI.find(a => a.name === prefApp)) {
                    missingApps.push(prefApp);
                }
            });
            if(missingApps.length > 0) {
                userPrefs.applications = userPrefs.applications.filter(b => !missingApps.find(m => m === b));
                // sync model.me.myApps lists too
                model.me.myApps.applications = model.me.myApps.applications.filter(a => !missingApps.find(m => m === a));
            }

            // Sync any change.
            if(userPrefs.bookmarks.length !== originalPrefs.bookmarks.length
                || 
               userPrefs.applications.length !== originalPrefs.applications.length
            ) {
                await http.put('/userbook/preference/apps', userPrefs);
            }
        }
    }

    isConnectorBeforeLastAppInMyApps(apps: Array<App>, connectors: Array<App>): boolean {
        let res = false;
        let lastAppIndex = 0;
        model.me.myApps.applications.forEach((appName: string, index) => {
            if (apps.find(app => appName === app.name)) {
                lastAppIndex = index;
            }
        });
        model.me.myApps.applications.forEach((appName: String, index) => {
            if (connectors.find(conn => appName === conn.name) && index < lastAppIndex) {
                res = true;
            }
        });
        return res;
    }
    
    orderAppsFirstInMyApps(apps: Array<App>): void {
        const orderedApplications = [];
        const orderedConnectors = [];
        model.me.myApps.applications.forEach((myAppsAppName: string) => {
            if (apps.find(app => myAppsAppName === app.name)) {
                orderedApplications.push(myAppsAppName);
            } else {
                orderedConnectors.push(myAppsAppName);
            }
        });
        model.me.myApps.applications = [...orderedApplications, ...orderedConnectors];   
    }

    isBookmark(app: App, is1DTheme: boolean): boolean {
        if (is1DTheme) return false; // We don't consider bookmarks in 1D
        return model.me.bookmarkedApps.find(bookmarkedApp => bookmarkedApp.name === app.name);
    }

    isApplication(app: App, is1DTheme: boolean): boolean {
        return !AppsService.getInstance().isBookmark(app, is1DTheme) && !app.isExternal;
    }

    isConnector(app: App, is1DTheme: boolean): boolean {
        return !AppsService.getInstance().isBookmark(app, is1DTheme) && app.isExternal;
    }
    
    sortApp(a: App, b: App) {
        return model.me.myApps.applications.indexOf(a.name) > model.me.myApps.applications.indexOf(b.name)? 1: -1;
    }

    pushToMyApps(app: App) {
        if (model.me.myApps.applications.indexOf(app.name) == -1) {
            model.me.myApps.applications.push(app.name);
        }
    }

    async is1DTheme(): Promise<boolean> {
        let conf = await ui.getCurrentThemeConf();
        return conf && conf.parent == "panda";
    }

}
