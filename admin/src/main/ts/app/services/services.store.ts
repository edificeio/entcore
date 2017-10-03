import { StructureModel, ApplicationModel, AbstractStore } from '../core/store';

export class ServicesStore extends AbstractStore {

    constructor(){ super(['structure', 'application']) }
    
    structure: StructureModel;
    application: ApplicationModel;
}