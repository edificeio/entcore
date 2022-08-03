import { AbstractStore } from 'src/app/core/store/abstract.store';
import { ApplicationModel } from 'src/app/core/store/models/application.model';
import { Injectable } from "@angular/core";

@Injectable()
export class AdmcAppsRolesStore extends AbstractStore {

    constructor() {
        super(['application']);
    }

    application: ApplicationModel;
}
