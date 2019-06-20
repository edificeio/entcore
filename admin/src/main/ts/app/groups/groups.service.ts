import { Injectable } from "@angular/core";
import { GroupModel } from "../core/store";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs/Observable";
import "rxjs/add/operator/do";
import { GroupsStore } from "./groups.store";

@Injectable()
export class GroupsService {

    constructor(private httpClient: HttpClient,
        public groupsStore: GroupsStore) {
    }

    public delete(group: GroupModel): Observable<void> {
        return this.httpClient.delete<void>(`/directory/group/${group.id}`)
            .do(() => {
                this.groupsStore.structure.groups.data.splice(
                    this.groupsStore.structure.groups.data.findIndex(g => g.id === group.id)
                    , 1);
            });
    }
}
