import {FlashMessageModel} from './../../core/store/models/flashmessage.model';
import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, Router} from '@angular/router';
import {routing, SpinnerService} from '../../core/services';
import {globalStore} from '../../core/store';
import {MessageFlashService} from './message-flash.service';
import {MessageFlashStore} from './message-flash.store';

@Injectable()
export class MessageFlashResolver implements Resolve<FlashMessageModel[]> {

    constructor(
        private spinner: SpinnerService,
        private router: Router,
        private messageStore: MessageFlashStore,
    ) { }

    resolve(route: ActivatedRouteSnapshot): Promise<FlashMessageModel[]> | FlashMessageModel[] {
        let forceReload = false;
        if (route.queryParams.forceReload) {
            forceReload = true;
        }
        const structureId: string = routing.getParam(route, 'structureId').toString();
        if (!forceReload && !!this.messageStore.structure && !!this.messageStore.messages && this.messageStore.structure.id === structureId) {
            return this.messageStore.messages;
        }
        const currentStructure = globalStore.structures.data.find(s => s.id === structureId);
        this.messageStore.structure = currentStructure;
        const p = new Promise<FlashMessageModel[]>((resolve, reject) => {
            MessageFlashService.getMessagesByStructure(structureId)
            .then((messages: FlashMessageModel[]) => {
                this.messageStore.messages = messages;
                resolve(messages);
            }, error => {
                reject(error);
            });
        });
        return this.spinner.perform('portal-content', p);
    }

}
