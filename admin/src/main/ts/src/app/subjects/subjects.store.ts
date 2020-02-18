import { AbstractStore } from '../core/store/abstract.store';
import { StructureModel} from '../core/store/models/structure.model'
import { SubjectModel } from '../core/store/models/subject.model';

export class SubjectsStore extends AbstractStore {

    constructor() {
        super(['structure', 'subject']);
    }

    structure: StructureModel;
    subject: SubjectModel;
}