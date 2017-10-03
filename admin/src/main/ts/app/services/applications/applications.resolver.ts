import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';

import { SessionModel, Session, globalStore, ApplicationCollection } from '../../core/store';
import { ApplicationModel } from '../../core/store/models';
import { SpinnerService, routing } from '../../core/services';

@Injectable()
export class ApplicationsResolver implements Resolve<ApplicationModel[]> {

    constructor(private spinner: SpinnerService) { }

    resolve(route: ActivatedRouteSnapshot): Promise<ApplicationModel[]> {
        let currentStructure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'));
        if (currentStructure.applications.data.length > 0) {
            return Promise.resolve(currentStructure.applications.data);
        } else {
            return this.spinner.perform('portal-content', currentStructure.applications.syncApps()
                .then(data => {
                    return currentStructure.applications.data;
                })
                .catch(e => console.error(e))
            );
        }
    }

}