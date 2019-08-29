import { HttpClient } from "@angular/common/http";
import { BlockProfileModel } from "../../core/store/models/blockprofile.model";
import { Observable } from "rxjs";
import { Injectable } from "@angular/core";

@Injectable()
export class BlockProfilesService {
    constructor(private httpClient: HttpClient) {}

    update(structureId: string, profile: BlockProfileModel): Observable<void> {
        return this.httpClient
            .put<BlockProfileModel>(`/directory/structure/${structureId}/profile/block`, profile)
            .flatMap(() => this.httpClient.post('/admin/trace', {'action': profile.block ? "BLOCK": "UNBLOCK", profile: profile.profile}))
            .map(res => console.log(res));
    }
}