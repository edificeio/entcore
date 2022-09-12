import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { BlockProfile } from './BlockProfile';
import { Trace, TraceResponse } from '../../types/trace';

@Injectable()
export class BlockProfilesService {
    constructor(private httpClient: HttpClient) {}

    update(structureId: string, profile: BlockProfile): Observable<object> {
        return this.httpClient.put<BlockProfile>(`/directory/structure/${structureId}/profile/block`, profile);
    }

    getTraces(structureId: string): Observable<TraceResponse[]> {
        return this.httpClient.get<TraceResponse[]>(`/admin/api/block/traces/${structureId}`);
    }
}
