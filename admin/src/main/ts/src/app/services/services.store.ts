import { AbstractStore } from '../core/store/abstract.store';
import { StructureModel } from '../core/store/models/structure.model';
import { ApplicationModel } from '../core/store/models/application.model';
import { ConnectorModel } from '../core/store/models/connector.model';

export class ServicesStore extends AbstractStore {

    constructor() {
        super(['structure', 'application']);
    }

    structure: StructureModel;
    application: ApplicationModel;
    connector: ConnectorModel;
}
