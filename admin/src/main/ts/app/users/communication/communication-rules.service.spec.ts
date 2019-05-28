import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BidirectionalCommunicationRules, CommunicationRulesService } from './communication-rules.service';
import { generateGroup } from '../../shared/utils';
import { GroupModel } from '../../core/store/models';
import 'rxjs/add/operator/skip';

describe('CommunicationRulesService', () => {
    let communicationRulesService: CommunicationRulesService;
    let httpController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                CommunicationRulesService
            ],
            imports: [
                HttpClientTestingModule
            ]
        });
        communicationRulesService = TestBed.get(CommunicationRulesService);
        httpController = TestBed.get(HttpTestingController);
    });

    it('should retrieve outgoing and incoming communications and emit new communication rules when giving a list of groups', () => {
        let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
        communicationRulesService
            .changes()
            .subscribe(cr => rules = cr);
        communicationRulesService.setGroups([generateGroup('group1'), generateGroup('group2')]);
        httpController.expectOne('/communication/group/group1/outgoing').flush([]);
        httpController.expectOne('/communication/group/group2/outgoing').flush([generateGroup('group3')]);
        httpController.expectOne('/communication/group/group1/incoming').flush([]);
        httpController.expectOne('/communication/group/group2/incoming').flush([generateGroup('group4')]);
        expect(rules.sending.length).toBe(2);
        expect(rules.sending[0].receivers.length).toBe(0);
        expect(rules.sending[1].receivers.length).toBe(1);
        expect(rules.sending[1].receivers[0].id).toBe('group3');
        expect(rules.receiving.length).toBe(2);
        expect(rules.receiving[0].sender).toBeNull();
        expect(rules.receiving[0].receivers[0].id).toBe('group1');
        expect(rules.receiving[1].sender.id).toBe('group4');
        expect(rules.receiving[1].receivers[0].id).toBe('group2');
    });

    describe('toggleInternalCommunicationRule', () => {
        it(`should call DELETE /communication/group/myGroupId/users given group id 'myGroupId' and internal communication rule 'BOTH'`, () => {
            const group: GroupModel = generateGroup('myGroupId', 'BOTH');
            communicationRulesService.toggleInternalCommunicationRule(group).subscribe();
            const communicationGroupRequest = httpController.expectOne('/communication/group/myGroupId/users');
            expect(communicationGroupRequest.request.method).toBe('DELETE');
        });

        it(`should call POST /communication/group/myGroupId/users given group id 'myGroupId' and internal communication rule 'NONE'`, () => {
            const group: GroupModel = generateGroup('myGroupId', 'NONE');
            communicationRulesService.toggleInternalCommunicationRule(group).subscribe();
            expect(httpController.expectOne('/communication/group/myGroupId/users').request.method).toEqual('POST');
        });

        it(`should call POST /communication/group/myGroupId given group id 'myGroupId' and internal communication rule 'INCOMING'`, () => {
            const group: GroupModel = generateGroup('myGroupId', 'INCOMING');
            communicationRulesService.toggleInternalCommunicationRule(group).subscribe();
            expect(httpController.expectOne('/communication/group/myGroupId/users').request.method).toEqual('POST');
        });

        it(`should call POST /communication/group/myGroupId/users with direction BOTH given group id 'myGroupId' and internal communication rule 'OUTGOING'`, () => {
            const group: GroupModel = generateGroup('myGroupId', 'OUTGOING');
            communicationRulesService.toggleInternalCommunicationRule(group).subscribe();
            expect(httpController.expectOne('/communication/group/myGroupId/users').request.method).toBe('POST');
        });

        it(`should emit a new communication rules if the given group is part of the current sending communication rules as sender`, () => {
            const group1: GroupModel = generateGroup('group1', 'BOTH');
            const group2: GroupModel = generateGroup('group2', 'BOTH');
            const group3: GroupModel = generateGroup('group3', 'BOTH');
            let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(cr => rules = cr);
            communicationRulesService.setGroups([group1, group2]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group2/outgoing').flush([group3]);
            httpController.expectOne('/communication/group/group1/incoming').flush([]);
            httpController.expectOne('/communication/group/group2/incoming').flush([]);
            communicationRulesService.toggleInternalCommunicationRule(group1).subscribe();
            httpController.expectOne('/communication/group/group1/users').flush({users: null});
            expect(rules.sending[0].sender.internalCommunicationRule).toBe('NONE');
        });

        it(`should emit a new communication rules if the given group is part of the current sending communication rules as a receiver`, () => {
            const group1: GroupModel = generateGroup('group1', 'BOTH');
            const group2: GroupModel = generateGroup('group2', 'BOTH');
            const group3: GroupModel = generateGroup('group3', 'BOTH');
            let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(cr => rules = cr);
            communicationRulesService.setGroups([group1, group2]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group2/outgoing').flush([group3]);
            httpController.expectOne('/communication/group/group1/incoming').flush([]);
            httpController.expectOne('/communication/group/group2/incoming').flush([]);
            communicationRulesService.toggleInternalCommunicationRule(group3).subscribe();
            httpController.expectOne('/communication/group/group3/users').flush({users: null});
            expect(rules.sending[1].receivers[0].internalCommunicationRule).toBe('NONE');
        });

        it(`should emit a new communication rules if the given group is part of the current receiving communication rules as sender`, () => {
            const group1: GroupModel = generateGroup('group1', 'BOTH');
            const group2: GroupModel = generateGroup('group2', 'BOTH');
            const group3: GroupModel = generateGroup('group3', 'BOTH');
            let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(cr => rules = cr);
            communicationRulesService.setGroups([group1, group2]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group2/outgoing').flush([]);
            httpController.expectOne('/communication/group/group1/incoming').flush([group3]);
            httpController.expectOne('/communication/group/group2/incoming').flush([]);
            communicationRulesService.toggleInternalCommunicationRule(group3).subscribe();
            httpController.expectOne('/communication/group/group3/users').flush({users: null});
            expect(rules.receiving[0].sender.internalCommunicationRule).toBe('NONE');
        });

        it(`should not emit a new communication rules if the given group is not part of the current communication rules`, () => {
            const group1: GroupModel = generateGroup('group1', 'BOTH');
            const group2: GroupModel = generateGroup('group2', 'BOTH');
            const group3: GroupModel = generateGroup('group3', 'BOTH');
            let emitted: boolean = false;
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(() => emitted = true);
            communicationRulesService.setGroups([group1, group2]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group2/outgoing').flush([]);
            httpController.expectOne('/communication/group/group1/incoming').flush([]);
            httpController.expectOne('/communication/group/group2/incoming').flush([]);
            communicationRulesService.toggleInternalCommunicationRule(group3).subscribe();
            httpController.expectOne('/communication/group/group3/users').flush({users: null});
            expect(emitted).toBe(false);
        });

        it(`should emit a new communication rules with updated groups in sending and receiving directions`, () => {
            const group1: GroupModel = generateGroup('group1', 'BOTH');
            let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(cr => rules = cr);
            communicationRulesService.setGroups([group1]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group1/incoming').flush([]);
            communicationRulesService.toggleInternalCommunicationRule(group1).subscribe();
            httpController.expectOne('/communication/group/group1/users').flush({users: null});
            expect(rules.sending[0].sender.internalCommunicationRule).toBe('NONE');
            expect(rules.receiving[0].receivers[0].internalCommunicationRule).toBe('NONE');
        });
    });

    describe('removeCommunication', () => {
        it(`should call DELETE /communication/group/myGroup1/relations/myGroup2 given groups myGroup1, myGroup2`, () => {
            const group1: GroupModel = generateGroup('myGroup1');
            const group2: GroupModel = generateGroup('myGroup2');
            communicationRulesService.removeCommunication(group1, group2, 'sending').subscribe();
            const communicationGroupRequest = httpController.expectOne('/communication/group/myGroup1/relations/myGroup2');
            expect(communicationGroupRequest.request.method).toBe('DELETE');
        });

        it(`should emit a new communication rules (without the receiver) if communication rule was in the monitored sending communication rules`, () => {
            const group1: GroupModel = generateGroup('group1');
            const group2: GroupModel = generateGroup('group2');
            const group3: GroupModel = generateGroup('group3');
            let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(cr => rules = cr);
            communicationRulesService.setGroups([group1, group2]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group2/outgoing').flush([group3]);
            httpController.expectOne('/communication/group/group1/incoming').flush([]);
            httpController.expectOne('/communication/group/group2/incoming').flush([]);
            communicationRulesService.removeCommunication(group2, group3, 'sending').subscribe();
            httpController.expectOne('/communication/group/group2/relations/group3').flush(null);
            expect(rules.sending[1].receivers.length).toBe(0);
        });

        it(`should emit a new communication rules (with the sender as null) if communication rule was in the monitored receiving communication rules`, () => {
            const group1: GroupModel = generateGroup('group1');
            const group2: GroupModel = generateGroup('group2');
            let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(cr => rules = cr);
            communicationRulesService.setGroups([group1]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group1/incoming').flush([group2]);
            communicationRulesService.removeCommunication(group2, group1, 'receiving').subscribe();
            httpController.expectOne('/communication/group/group2/relations/group1').flush(null);
            expect(rules.receiving.length).toBe(1);
            expect(rules.receiving[0].sender).toBeNull();
            expect(rules.receiving[0].receivers[0].id).toBe('group1');
        });

        it(`should emit a new communication rules (without the sender) if communication rule was in the monitored receiving communication rules`, () => {
            const group1: GroupModel = generateGroup('group1');
            const group2: GroupModel = generateGroup('group2');
            const group3: GroupModel = generateGroup('group3');
            let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(cr => rules = cr);
            communicationRulesService.setGroups([group1]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group1/incoming').flush([group2, group3]);
            communicationRulesService.removeCommunication(group2, group1, 'receiving').subscribe();
            httpController.expectOne('/communication/group/group2/relations/group1').flush(null);
            expect(rules.receiving.length).toBe(1);
            expect(rules.receiving[0].sender.id).toBe('group3');
            expect(rules.receiving[0].receivers[0].id).toBe('group1');
        });
    });

    describe('createCommunication', () => {
        it('should call POST /communication/v2/group/group1/communique/group2 given group1, group2 and sending direction', () => {
            const group1: GroupModel = generateGroup('group1');
            const group2: GroupModel = generateGroup('group2');
            communicationRulesService.createCommunication(group1, group2, 'sending').subscribe();
            httpController.expectOne('/communication/v2/group/group1/communique/group2');
        });
        it('should call POST /communication/v2/group/group1/communique/group2 given group1, group2 and receiving direction', () => {
            const group1: GroupModel = generateGroup('group1');
            const group2: GroupModel = generateGroup('group2');
            communicationRulesService.createCommunication(group1, group2, 'receiving').subscribe();
            httpController.expectOne('/communication/v2/group/group1/communique/group2');
        });

        it('should emit rules with updated receivers when creating a rule in the sending direction', () => {
            const group1: GroupModel = generateGroup('group1', 'INCOMING');
            const group2: GroupModel = generateGroup('group2');
            let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(cr => rules = cr);
            communicationRulesService.setGroups([group1]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group1/incoming').flush([]);

            communicationRulesService.createCommunication(group1, group2, 'sending').subscribe();
            httpController.expectOne('/communication/v2/group/group1/communique/group2').flush({'ok': 'no direction to change'});

            expect(rules.sending[0].sender.internalCommunicationRule).toBe('INCOMING');
            expect(rules.sending[0].receivers[0].id).toBe('group2');
        });

        it('should emit rules with updated receivers and internal com rule if the new rule changed it when creating a rule in the sending direction', () => {
            const group1: GroupModel = generateGroup('group1', 'INCOMING');
            const group2: GroupModel = generateGroup('group2');
            let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(cr => rules = cr);
            communicationRulesService.setGroups([group1]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group1/incoming').flush([]);

            communicationRulesService.createCommunication(group1, group2, 'sending').subscribe();
            httpController.expectOne('/communication/v2/group/group1/communique/group2').flush({'group1': 'BOTH'});

            expect(rules.sending[0].sender.internalCommunicationRule).toBe('BOTH');
            expect(rules.receiving[0].receivers[0].internalCommunicationRule).toBe('BOTH');
            expect(rules.sending[0].receivers[0].id).toBe('group2');
        });

        it('should emit rules with updated senders when creating a rule in the receiving direction', () => {
            const group1: GroupModel = generateGroup('group1', 'BOTH');
            const group2: GroupModel = generateGroup('group2');
            let rules: BidirectionalCommunicationRules = {sending: [], receiving: []};
            communicationRulesService
                .changes()
                .skip(1)
                .subscribe(cr => rules = cr);
            communicationRulesService.setGroups([group1]);
            httpController.expectOne('/communication/group/group1/outgoing').flush([]);
            httpController.expectOne('/communication/group/group1/incoming').flush([]);

            communicationRulesService.createCommunication(group2, group1, 'receiving').subscribe();
            httpController.expectOne('/communication/v2/group/group2/communique/group1').flush({'ok': 'no direction to change'});

            expect(rules.receiving.length).toBe(1);
            expect(rules.receiving[0].sender.id).toBe('group2');
            expect(rules.receiving[0].receivers[0].id).toBe('group1');
            expect(rules.receiving[0].receivers[0].internalCommunicationRule).toBe('BOTH');
        });
    });
});
