import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';

import {globalStore} from '../../../core/store';
import {ConnectorModel} from '../../../core/store/models';
import {routing, SpinnerService} from '../../../core/services';

@Injectable()
export class ConnectorsResolver implements Resolve<ConnectorModel[]> {

    constructor(private spinner: SpinnerService) { }

    resolve(route: ActivatedRouteSnapshot): Promise<ConnectorModel[]> {
        const currentStructure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'));
        const p = new Promise<ConnectorModel[]>((resolve, reject) => {
            currentStructure.connectors.syncConnectors()
            .then(data => {
                resolve(currentStructure.connectors.data);
            }, error => {
                reject(error);
            }
            );
        });
        return this.spinner.perform('portal-content', p);
    }
}
