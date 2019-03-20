import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BundlesService, SijilModule } from 'sijil';
import { Component, DebugElement, Input } from '@angular/core';
import { UxModule } from '../../shared/ux/ux.module';
import {
    CommunicationRule,
    CommunicationRulesComponent,
    communicationRulesLocators as locators,
    uniqueGroups
} from './communication-rules.component';
import { generateGroup } from './communication-test-utils';
import { GroupModel } from '../../core/store/models';

describe('CommunicationRulesComponent', () => {
    let component: CommunicationRulesComponent;
    let fixture: ComponentFixture<CommunicationRulesComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                CommunicationRulesComponent,
                MockGroupCard
            ],
            providers: [],
            imports: [
                SijilModule.forRoot(),
                UxModule.forRoot(null)
            ]

        }).compileComponents();
        const bundlesService = TestBed.get(BundlesService);
        bundlesService.addToBundle({
            "user.communication.groups-of-user": "Groupes de l'utlisateur"
        });
        fixture = TestBed.createComponent(CommunicationRulesComponent);
        component = fixture.debugElement.componentInstance;
        component.communicationRules = [
            generateCommunicationRule('c1'),
            generateCommunicationRule('groupf1'),
            generateCommunicationRule('groupf2'),
            generateCommunicationRule('groupm1')
        ];
        fixture.detectChanges();
    }));

    it('should create the UserCommunicationComponent component', async(() => {
        expect(component).toBeTruthy();
    }));

    it('should display the user groups (c1, groupf1, groupf2, groupm1) given user belonging to c1, groupf1, groupf2 and groupm1', () => {
        expect(getSendingGroups(fixture).length).toBe(4);
    });

    it('should display the user groups (c1) and receiver groups (c2, c3) given user belonging to c1 and c1 can communicate with c2 and c3', () => {
        component.communicationRules = [generateCommunicationRule('c1', ['c2', 'c3'])];
        fixture.detectChanges();
        expect(getSendingGroups(fixture).length).toBe(1);
        expect(getReceivingGroups(fixture).length).toBe(2);
    });
});

describe('uniqueGroups', () => {
    it('should return a filtered array of unique groups', () => {
        const nonUniqueGroups = [generateGroup('jojo'),
            generateGroup('titi'),
            generateGroup('jojo')];
        const filtered = uniqueGroups(nonUniqueGroups);
        expect(filtered.map(g => g.id)).toEqual(['jojo', 'titi']);
    });
});

function getSendingGroups(fixture: ComponentFixture<CommunicationRulesComponent>): DebugElement[] {
    return fixture.debugElement.queryAll(By.css(`${locators.sendingColumn} ${locators.group}`));
}

function getReceivingGroups(fixture: ComponentFixture<CommunicationRulesComponent>): DebugElement[] {
    return fixture.debugElement.queryAll(By.css(`${locators.receivingColumn} ${locators.group}`));
}

function generateCommunicationRule(senderName: string, receiversName: string[] = []): CommunicationRule {
    return {sender: generateGroup(senderName), receivers: receiversName.map(name => generateGroup(name))}
}

@Component({
    selector: 'group-card',
    template: ''
})
class MockGroupCard {
    @Input()
    group: GroupModel;
}
