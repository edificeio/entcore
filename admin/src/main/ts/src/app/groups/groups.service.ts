import {Injectable} from '@angular/core';
import {GroupModel} from '../core/store/models/group.model';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {tap} from 'rxjs/operators';
import {GroupsStore} from './groups.store';
import { StructureModel } from '../core/store/models/structure.model';

export type GroupUpdatePayload = {
    name?: string;
    autolinkTargetAllStructs?: boolean;
    autolinkTargetStructs?: Array<string>;
    autolinkUsersFromGroups?: Array<string>;
}

@Injectable()
export class GroupsService {

    constructor(private httpClient: HttpClient,
                public groupsStore: GroupsStore) {
    }

    public delete(group: GroupModel): Observable<void> {
        return this.httpClient.delete<void>(`/directory/group/${group.id}`)
        .pipe(
            tap(() => {
                this.groupsStore.structure.groups.data.splice(
                    this.groupsStore.structure.groups.data.findIndex(g => g.id === group.id)
                    , 1);
            })
        );
    }

    public update(groupId: string, groupUpdatePayload: GroupUpdatePayload): Observable<void> {
        return this.httpClient.put<void>(`/directory/group/${groupId}`, groupUpdatePayload)
        .pipe(
            tap(() => {
                const sGroup: GroupModel = this.groupsStore.structure.groups.data.find(g => g.id === groupId);
                if (sGroup) {
                    sGroup.name = groupUpdatePayload.name;
                    sGroup.autolinkTargetAllStructs = groupUpdatePayload.autolinkTargetAllStructs;
                    sGroup.autolinkTargetStructs = groupUpdatePayload.autolinkTargetStructs;
                    sGroup.autolinkUsersFromGroups = groupUpdatePayload.autolinkUsersFromGroups;
                }
                this.groupsStore.group.name = groupUpdatePayload.name;
                this.groupsStore.group.autolinkTargetAllStructs = groupUpdatePayload.autolinkTargetAllStructs;
                this.groupsStore.group.autolinkTargetStructs = groupUpdatePayload.autolinkTargetStructs;
                this.groupsStore.group.autolinkUsersFromGroups = groupUpdatePayload.autolinkUsersFromGroups;
            })
        );
    }

    public getFuncAndDisciplines(structure: StructureModel): Observable<Array<GroupModel>> {
        return this.httpClient.get<Array<GroupModel>>(
            '/directory/group/admin/funcAndDisciplines', 
            {
                'params': {
                    'structureId': structure.id,
                    'recursive': 'true'
                }
            }
        );
    }
}