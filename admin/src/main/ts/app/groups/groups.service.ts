import { Injectable } from "@angular/core";
import { GroupModel } from "../core/store";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs/Observable";

@Injectable()
export class GroupsService {

    constructor(private httpClient: HttpClient) {
    }

    public delete(group: GroupModel): Observable<void> {
        return this.httpClient.delete<void>(`/directory/group/${group.id}`);
    }
}
