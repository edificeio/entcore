import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {Slot, SlotProfile} from './SlotProfile';

@Injectable({
    providedIn: 'root'
})
export class CalendarService {

    constructor(private httpClient: HttpClient) {
    }

    public getGrids(schoolId: string): Observable<SlotProfile[]> {
        return this.httpClient.get<SlotProfile[]>(`/directory/slotprofiles/schools/${schoolId}`);
    }

    public addSlot(gridId: string, slot: Slot): Observable<void> {
        return this.httpClient.post<void>(`/directory/slotprofiles/${gridId}/slots`, slot);
    }

    public updateSlot(gridId: string, slotId: string, slot: Slot): Observable<void> {
        return this.httpClient.put<void>(`/directory/slotprofiles/${gridId}/slots/${slotId}`, slot);
    }

    public deleteSlot(gridId: string, slotId: string): Observable<void> {
        return this.httpClient.delete<void>(`/directory/slotprofiles/${gridId}/slots/${slotId}`);
    }

    public addGrid(slotProfile: SlotProfile): Observable<void> {
        return this.httpClient.post<void>(`/directory/slotprofiles`, slotProfile);
    }

    public updateGrid(gridId: string, slotProfile: SlotProfile): Observable<void> {
        return this.httpClient.put<void>(`/directory/slotprofiles/${gridId}`, slotProfile);
    }

    public deleteGrid(gridId: string): Observable<void> {
        return this.httpClient.delete<void>(`/directory/slotprofiles/${gridId}`);
    }
}
