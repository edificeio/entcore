import {UserModel} from '../models/user.model';
import {Collection, HttpResponse} from 'entcore-toolkit';

export class UserCollection extends Collection<UserModel> {

    constructor(syncUrl?: string) {
        super({
            sync: syncUrl != null ? syncUrl : '/directory/structure/:structureId/users'
        }, UserModel);
    }

    sync(opts?: {}): Promise<HttpResponse> {
        const res = super.sync(opts);
        const removeDuplicates = (users: UserModel[]): UserModel[] => {
            const res: UserModel[] = [];
            for (let user of users) {
                let exists = res.find(u => u.id == user.id);
                if (exists) {
                    exists.duplicates = exists.duplicates.concat(user.duplicates);
                } else {
                    res.push(user);
                }
            }
            return res;
        }
        res.then(event => { this.data = removeDuplicates(this.data); });
        return res;
    }

    public structureId: string;

}
