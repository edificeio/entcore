import { Injectable } from '@angular/core';
import { CommunicationRule } from './communication-rules.component';
import { GroupModel, InternalCommunicationRule } from '../../core/store/models';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/observable/forkJoin';

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

    public toggleInternalCommunicationRule(group: GroupModel): Observable<InternalCommunicationRule> {
        let request: Observable<{ users: InternalCommunicationRule | null }>;
        const direction: InternalCommunicationRule = group.internalCommunicationRule === 'BOTH' ? 'NONE' : 'BOTH';

        if (direction === 'BOTH') {
            request = this.http.post<{ users: InternalCommunicationRule | null }>(`/communication/group/${group.id}/users`, null);
        } else {
            request = this.http.delete<{ users: InternalCommunicationRule | null }>(`/communication/group/${group.id}/users`);
        }

        return request
            .map(resp => resp.users ? resp.users : 'NONE')
            .do((internalCommunicationRule) => {
                if (this.currentRules) {
                    const groupInCommunicationRules = this.findGroup(this.currentRules, group.id);
                    if (!!groupInCommunicationRules) {
                        groupInCommunicationRules.internalCommunicationRule = internalCommunicationRule;
                        this.rulesSubject.next(this.clone(this.currentRules));
                    }
                }
            });
    }

    private getCommunicationRulesOfGroup(sender: GroupModel): Observable<CommunicationRule> {
        return this.http.get<GroupModel[]>(`/communication/group/${sender.id}/outgoing`)
            .map(receivers => ({sender, receivers}));
    }

    private findGroup(communicationRules: CommunicationRule[], groupId: string): GroupModel {
        return communicationRules.reduce(
            (arrayOfGroups: GroupModel[], communicationRule: CommunicationRule): GroupModel[] =>
                [...arrayOfGroups, communicationRule.sender, ...communicationRule.receivers], [])
            .find(group => group.id === groupId);
    }

    private clone(communicationRules: CommunicationRule[]): CommunicationRule[] {
        return communicationRules.map(cr => ({
            sender: Object.assign({}, cr.sender),
            receivers: cr.receivers.map(re => Object.assign({}, re))
        }));
    }
}
