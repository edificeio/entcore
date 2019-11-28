import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import { ConnectorModel } from 'src/app/core/store/models/connector.model';
import { SpinnerService } from 'src/app/core/services/spinner.service';
import { globalStore } from 'src/app/core/store/global.store';
import { routing } from 'src/app/core/services/routing.service';

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
