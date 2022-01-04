import {AbstractStore} from 'src/app/core/store/abstract.store';
import { ApplicationCollection } from 'src/app/core/store/collections/application.collection';

export class AdmcAppsStore extends AbstractStore {
    constructor() {
        super('applications');
    }

    applications: ApplicationCollection = new ApplicationCollection();
}

export let g_appStore = new AdmcAppsStore();
