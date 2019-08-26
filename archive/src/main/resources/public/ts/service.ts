import { model, idiom as lang, notify } from "entcore";
import http from 'axios';

export const archiveService = {

    async getApplications(): Promise<{ activatedUserApps: string[], preDeletedUserApps: string[], isPreDeleted: boolean }> {
        
        let exportApps = await http.get('/archive/conf/public');
        let myApps = await http.get('/applications-list');

        let activatedUserApps = myApps.data.apps.map(app => app.prefix ? app.prefix.slice(1) : app.displayName ? app.displayName : "undefined")
        .filter(app => exportApps.data.apps.includes(app))
        .sort(function(a, b) {
            let a2 = lang.translate(a), b2 = lang.translate(b);
            return a2 < b2 ? -1 : a2 > b2 ? 1 : 0;
        });

        let isPreDeleted = false;
        let preDeletedUserApps;
        if (activatedUserApps.length === 0) {
            isPreDeleted = true;
            preDeletedUserApps = exportApps.data.apps;
        }

        return {activatedUserApps, preDeletedUserApps, isPreDeleted};
    }
}