import { Model } from 'entcore-toolkit';
import { UserModel } from './user.model';

export type InternalCommunicationRule = 'BOTH' | 'INCOMING' | 'OUTGOING' | 'NONE';

export type GroupType = 'ProfileGroup' | 'FunctionalGroup' | 'ManualGroup' | 'CommunityGroup' | 'FunctionGroup' | 'DisciplineGroup' | 'HTGroup';

export class GroupModel extends Model<GroupModel> {

    id?: string;
    name?: string;
    displayName?: string;
    type?: GroupType;
    subType?: string;
    classes?: { id: string, name: string }[];
    structures?: { id: string, name: string }[];
    filter?: string;
    structureId?: string;
    users: UserModel[];
    internalCommunicationRule?: InternalCommunicationRule;

    constructor() {
        super({
            create: '/directory/group'
        });
        this.users = new Array<UserModel>();
    }

    syncUsers() {
        return this.http.get(`/directory/user/admin/list?groupId=${this.id}`).then(res => {
            this.users = res.data;
        });
    }

    addUsers(users: UserModel[]) {
        return this.http.put(`/directory/group/${this.id}/users/add`, {"userIds": users.map(u => u.id)});
    }

    removeUsers(users: UserModel[]) {
        return this.http.put(`/directory/group/${this.id}/users/delete`, {"userIds": users.map(u => u.id)});
    }

    toJSON() {
        return {
            name: this.name,
            structureId: this.structureId
        };
    }
}
