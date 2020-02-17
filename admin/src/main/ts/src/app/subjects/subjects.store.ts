import {AbstractStore, SubjectModel, StructureModel} from '../core/store';

export class SubjectsStore extends AbstractStore {

    constructor() {
        super(['structure', 'subject']);
    }

    structure: StructureModel;
    subject: SubjectModel;
} 