import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, Router } from '@angular/router';
import { SpinnerService, routing } from '../../core/services';
import { globalStore } from '../../core/store'
import { MessageFlashService } from './message-flash.service';
import { MessageFlashStore } from './message-flash.store';
import { FlashMessageModel } from '../../core/store';

@Injectable()
export class MessageFlashResolver implements Resolve<FlashMessageModel[]> {

    constructor(
        private spinner: SpinnerService, 
        private router: Router,
        private messageStore: MessageFlashStore,
    ) { }

    resolve(route: ActivatedRouteSnapshot): Promise<FlashMessageModel[]> | FlashMessageModel[] {
        let forceReload: boolean = false;
        if (route.queryParams['forceReload']) {
            forceReload = true;
        }
        let structureId: string = routing.getParam(route, 'structureId').toString();
        if (!forceReload && !!this.messageStore.structure && !!this.messageStore.messages && this.messageStore.structure.id === structureId) {
            return this.messageStore.messages;
        }
        let currentStructure = globalStore.structures.data.find(s => s.id === structureId);
        this.messageStore.structure = currentStructure;
        return this.spinner.perform('portal-content', MessageFlashService.getMessagesByStructure(structureId)
            .then((messages: FlashMessageModel[]) => {
                this.messageStore.messages = messages;
                return messages;
            })
            .catch(() => {
                this.router.navigate(["/admin", structureId, "management", "message-flash", "list"]);
            }));
    }

}