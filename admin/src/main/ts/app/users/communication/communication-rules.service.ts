import { Injectable } from '@angular/core';
import { Column, CommunicationRule } from './communication-rules.component';
import { GroupModel, InternalCommunicationRule } from '../../core/store/models';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/observable/forkJoin';

interface CreateCommunicationResponseWithChange {
    [groupId: number]: string;
}


interface CreateCommunicationResponseWithNoChange {
    ok: 'no direction to change';
}

type CreateCommunicationResponse = CreateCommunicationResponseWithChange | CreateCommunicationResponseWithNoChange;

function isNoDirectionChangeResponse(response: CreateCommunicationResponse): response is CreateCommunicationResponseWithNoChange {
    return response['ok'] === 'no direction to change';
}

@Injectable()
export class CommunicationRulesService {

    private rulesSubject: Subject<BidirectionalCommunicationRules> = new Subject<BidirectionalCommunicationRules>();
    private rulesObservable: Observable<BidirectionalCommunicationRules> = this.rulesSubject.asObservable();
    private currentRules: BidirectionalCommunicationRules;

    constructor(private http: HttpClient) {
        this.rulesObservable.subscribe(cr => this.currentRules = cr);
    }

    public changes(): Observable<BidirectionalCommunicationRules> {
        return this.rulesObservable;
    }

    public setGroups(groups: GroupModel[]) {
        Observable.forkJoin(
            Observable.forkJoin(...groups.map(group => this.getSendingCommunicationRulesOfGroup(group))),
            Observable.forkJoin(...groups.map(group => this.getReceivingCommunicationRulesOfGroup(group)))
                .map(arr => arr.reduce((prev, current) => {
                    prev.push(...current);
                    return prev;
                }, []))
        )
            .map(arr => ({sending: arr[0], receiving: arr[1]}))
            .subscribe((communicationRules: BidirectionalCommunicationRules) => this.rulesSubject.next(communicationRules));
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
                    const groupsInCommunicationRules = []
                        .concat(this.findGroupsInCommunicationsRule(this.currentRules.sending, group.id))
                        .concat(this.findGroupsInCommunicationsRule(this.currentRules.receiving, group.id));
                    if (groupsInCommunicationRules.length > 0) {
                        groupsInCommunicationRules.forEach(group => group.internalCommunicationRule = internalCommunicationRule);
                        this.rulesSubject.next(this.clone(this.currentRules));
                    }
                }
            });
    }

    public removeCommunication(sender: GroupModel, receiver: GroupModel, positionOfUserGroup: Column): Observable<void> {
        return this.http.delete<void>(`/communication/group/${sender.id}/relations/${receiver.id}`)
            .do(() => {
                if (this.currentRules) {
                    if (positionOfUserGroup === 'sending') {
                        const communicationRuleOfSender = this.currentRules.sending.find(cr => cr.sender.id === sender.id);
                        if (!!communicationRuleOfSender) {
                            communicationRuleOfSender.receivers = communicationRuleOfSender.receivers
                                .filter(r => r.id !== receiver.id);
                        }
                    } else {
                        const communicationRulesOfReceiver = this.currentRules.receiving.filter(cr => cr.receivers.some(r => r.id === receiver.id));
                        const communicationRulesToRemove = communicationRulesOfReceiver.find(cr => cr.sender.id === sender.id);
                        if (communicationRulesOfReceiver.length > 1) {
                            this.currentRules.receiving.splice(this.currentRules.receiving.indexOf(communicationRulesToRemove), 1)
                        } else {
                            communicationRulesToRemove.sender = null;
                        }
                    }
                    this.rulesSubject.next(this.clone(this.currentRules));
                }
            });
    }

    public checkAddLink(sender: GroupModel, receiver: GroupModel): Observable<{ warning: string }> {
        return this.http.get<{ warning: string }>(`/communication/v2/group/${sender.id}/communique/${receiver.id}/check`);
    }

    public createCommunication(sender: GroupModel, receiver: GroupModel, positionOfUserGroup: Column): Observable<{ groupId: number, internalCommunicationRule: string }> {
        return this.http.post<CreateCommunicationResponse>(`/communication/v2/group/${sender.id}/communique/${receiver.id}`, {})
            .do((createCommunicationResponse: CreateCommunicationResponse) => {
                if (!isNoDirectionChangeResponse(createCommunicationResponse)) {
                    sender.internalCommunicationRule = createCommunicationResponse[sender.id] || sender.internalCommunicationRule;
                    receiver.internalCommunicationRule = createCommunicationResponse[receiver.id] || receiver.internalCommunicationRule;
                    if (positionOfUserGroup === 'sending') {
                        this.findGroupsInCommunicationsRule(this.currentRules.receiving, sender.id).forEach(g => g.internalCommunicationRule = sender.internalCommunicationRule);
                        this.findGroupsInCommunicationsRule(this.currentRules.receiving, receiver.id).forEach(g => g.internalCommunicationRule = receiver.internalCommunicationRule);
                    } else {
                        this.findGroupsInCommunicationsRule(this.currentRules.sending, receiver.id).forEach(g => g.internalCommunicationRule = receiver.internalCommunicationRule);
                        this.findGroupsInCommunicationsRule(this.currentRules.sending, sender.id).forEach(g => g.internalCommunicationRule = sender.internalCommunicationRule);
                    }
                }
                if (this.currentRules) {
                    if (positionOfUserGroup === 'sending') {
                        const communicationRuleOfSender = this.currentRules.sending.find(cr => cr.sender.id === sender.id);
                        if (!!communicationRuleOfSender) {
                            communicationRuleOfSender.receivers.push(receiver);
                        }
                    } else {
                        const communicationRulesOfReceiver = this.currentRules.receiving.filter(cr => cr.receivers.some(r => r.id === receiver.id));
                        const emptyCommunicationRule = communicationRulesOfReceiver.find(cr => !cr.sender);
                        if (!!emptyCommunicationRule) {
                            emptyCommunicationRule.sender = sender;
                        } else {
                            this.currentRules.receiving.push({sender: sender, receivers: [receiver]});
                        }
                    }
                    this.rulesSubject.next(this.clone(this.currentRules));
                }
            });
    }

    private getSendingCommunicationRulesOfGroup(sender: GroupModel): Observable<CommunicationRule> {
        return this.http.get<GroupModel[]>(`/communication/group/${sender.id}/outgoing`)
            .map(receivers => ({sender, receivers}));
    }

    private getReceivingCommunicationRulesOfGroup(receiver: GroupModel): Observable<CommunicationRule[]> {
        return this.http.get<GroupModel[]>(`/communication/group/${receiver.id}/incoming`)
            .map((senders: GroupModel[]): CommunicationRule[] => senders.map(sender => ({
                sender,
                receivers: [receiver]
            })))
            .map(communicationRules => communicationRules.length === 0 ?
                [{sender: null, receivers: [receiver]}] : communicationRules
            )
    }

    private clone(bidirectionalCommunicationRules: BidirectionalCommunicationRules): BidirectionalCommunicationRules {
        return {
            sending: this.cloneCommunicationRules(bidirectionalCommunicationRules.sending),
            receiving: this.cloneCommunicationRules(bidirectionalCommunicationRules.receiving)
        }
    }

    private cloneCommunicationRules(communicationRules: CommunicationRule[]): CommunicationRule[] {
        return communicationRules.map(cr => ({
            sender: cr.sender ? Object.assign({}, cr.sender) : null,
            receivers: cr.receivers.map(re => Object.assign({}, re))
        }));
    }

    private findGroupsInCommunicationsRule(communicationRules: CommunicationRule[], groupId: string): GroupModel[] {
        return communicationRules.reduce(
            (arrayOfGroups: GroupModel[], communicationRule: CommunicationRule): GroupModel[] =>
                [...arrayOfGroups, communicationRule.sender, ...communicationRule.receivers], [])
            .filter(group => !!group)
            .filter(group => group.id === groupId);
    }
}

export interface BidirectionalCommunicationRules {
    sending: CommunicationRule[],
    receiving: CommunicationRule[]
}
