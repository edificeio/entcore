import { AbstractStore } from '../core/store/abstract.store';
import { StructureModel } from '../core/store/models/structure.model';
import { UserModel } from '../core/store/models/user.model';

export class UsersStore extends AbstractStore {
    constructor() {
        super(['structure', 'user']);
    }

    structure: StructureModel;
    user: UserModel;
}
