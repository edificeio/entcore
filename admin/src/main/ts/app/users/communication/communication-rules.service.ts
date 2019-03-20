import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { CommunicationRule } from './communication-rules.component';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/observable/forkJoin';
import { GroupModel, InternalCommunicationRule } from '../../core/store/models';
import { HttpClient } from '@angular/common/http';

@Injectable()
export class CommunicationRulesService {

    private rulesSubject: Subject<CommunicationRule[]> = new Subject<CommunicationRule[]>();
    private rulesObservable: Observable<CommunicationRule[]> = this.rulesSubject.asObservable();
    private currentRules: CommunicationRule[];

    constructor(private http: HttpClient) {
        this.rulesObservable.subscribe(cr => this.currentRules = cr);
    }

    public changes(): Observable<CommunicationRule[]> {
        return this.rulesObservable;
    }

    public setGroups(groups: GroupModel[]) {
        Observable.forkJoin(...groups.map(group => this.getCommunicationRulesOfGroup(group)))
            .subscribe((communicationRules: CommunicationRule[]) => this.rulesSubject.next(communicationRules));
    }

    public toggleInternalCommunicationRule(group: GroupModel): Observable<void> {
        let request: Observable<void>;
        const direction: InternalCommunicationRule = group.internalCommunicationRule === 'BOTH' ? 'NONE' : 'BOTH';

        if (direction === 'BOTH') {
            request = this.http.post<void>(`/communication/group/${group.id}`, {direction});
        } else {
            request = this.http.request<void>('delete', `/communication/group/${group.id}`, {body: {direction: 'BOTH'}});
        }

        return request.do(() => {
            const groupInCommunicationRules = this.findGroup(this.currentRules, group.id);
            if (!!groupInCommunicationRules) {
                groupInCommunicationRules.internalCommunicationRule = direction;
                this.rulesSubject.next(this.clone(this.currentRules));
            }
        });
    }

    private getCommunicationRulesOfGroup(sender: GroupModel): Observable<CommunicationRule> {
        return Observable.of({sender, receivers: []});
    }

    private findGroup(communicationRules: CommunicationRule[], groupId: string): GroupModel {
        return communicationRules.map(rule => rule.sender).find(sender => sender.id === groupId);
    }

    private clone(communicationRules: CommunicationRule[]): CommunicationRule[] {
        return communicationRules.map(cr => ({
            sender: Object.assign({}, cr.sender),
            receivers: cr.receivers.map(re => Object.assign({}, re))
        }));
    }
}
