import { UserModel } from 'src/app/core/store/models/user.model';
import { UserDetailsModel } from 'src/app/core/store/models/userdetails.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { globalStore } from 'src/app/core/store/global.store';

export abstract class AbstractSection {
    constructor() {
    }

    get user(): UserModel {
        return this._user;
    }

    set user(userModel: UserModel) {
        this._user = userModel;
        this.details = userModel.userDetails;
        this.onUserChange();
    }

    protected _user: UserModel;
    details: UserDetailsModel;
    structure: StructureModel;

    protected now = `${new Date().getFullYear()}-${new Date().getMonth() + 1}-${new Date().getDate()}`;

    // HTML5 email
    emailPattern = /^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;

    protected getStructure(id: string): StructureModel {
        return globalStore.structures.data.find(s => s.id === id);
    }

    protected abstract onUserChange();
}
