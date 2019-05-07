import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';

import { globalStore } from '../../../core/store';
import { ConnectorModel } from '../../../core/store/models';
import { SpinnerService, routing } from '../../../core/services';

@Injectable()
export class ConnectorsResolver implements Resolve<ConnectorModel[]> {

    constructor(private spinner: SpinnerService) { }

    resolve(route: ActivatedRouteSnapshot): Promise<ConnectorModel[]> {
        let currentStructure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'));
        if (currentStructure.connectors.data.length > 0) {
            return Promise.resolve(currentStructure.connectors.data);
        } else {
            return this.spinner.perform('portal-content', currentStructure.connectors.syncConnectors()
                .then(data => {
                    return currentStructure.connectors.data;
                })
                .catch(e => console.error(e))
            );
        }
    }

}