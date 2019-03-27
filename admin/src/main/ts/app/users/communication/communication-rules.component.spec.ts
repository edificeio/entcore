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
import { CommunicationRulesService } from "./communication-rules.service";
import { NotifyService } from "../../core/services";
import { GroupNameService } from "./group-name.service";

describe('CommunicationRulesComponent', () => {
    let component: CommunicationRulesComponent;
    let notifyService: NotifyService;
    let groupNameService: GroupNameService;
    let communicationRulesService: CommunicationRulesService;
    let fixture: ComponentFixture<CommunicationRulesComponent>;

    beforeEach(async(() => {
        communicationRulesService = jasmine.createSpyObj('CommunicationRulesService', ['removeCommunication']);
        notifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        groupNameService = jasmine.createSpyObj('GroupNameService', ['getGroupName']);
        TestBed.configureTestingModule({
            declarations: [
                CommunicationRulesComponent,
                MockGroupCard
            ],
            providers: [
                {useValue: notifyService, provide: NotifyService},
                {useValue: groupNameService, provide: GroupNameService},
                {useValue: communicationRulesService, provide: CommunicationRulesService}
            ],
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

    describe('resetHighlight', () => {
        it('should unset the highlighted cell', () => {
            component.highlighted = {column: 'sending', group: generateGroup('group1')};
            component.resetHighlight();
            expect(component.highlighted).toBeNull();
        });
    });

    describe('select', () => {
        it('should set the selected cell and call resetHighlight', () => {
            spyOn(component, 'resetHighlight');
            component.select('receiving', generateGroup('group2'));
            expect(component.selected.column).toBe('receiving');
            expect(component.selected.group.id).toBe('group2');
            expect(component.resetHighlight).toHaveBeenCalled();
        });
    });

    describe('highlight', () => {
        it('should set the highlighted cell if the selected and highlighted cells are different', () => {
            component.highlight('receiving', generateGroup('group1'),
                {column: 'sending', group: generateGroup('group2')});
            expect(component.highlighted.column).toBe('receiving');
            expect(component.highlighted.group.id).toBe('group1');
        });

        it('should not set the highlighted cell if the selected and highlighted cells are the same', () => {
            component.highlighted = {column: 'sending', group: generateGroup('group2')};
            component.highlight('receiving', generateGroup('group1'),
                {column: 'receiving', group: generateGroup('group1')});
            expect(component.highlighted.column).toBe('sending');
            expect(component.highlighted.group.id).toBe('group2');
        });

        it('should set the highlighted cell if the selected cell is null', () => {
            component.highlight('receiving', generateGroup('group1'), null);
            expect(component.highlighted.column).toBe('receiving');
            expect(component.highlighted.group.id).toBe('group1');
        });
    });

    describe('isSelected', () => {
        it('should return true if the given cell is the one selected', () => {
            expect(component.isSelected(
                'receiving',
                generateGroup('group1'),
                {column: 'receiving', group: generateGroup('group1')}
            )).toBe(true);
        });

        it('should return false if the given cell is not the one selected (different id)', () => {
            expect(component.isSelected(
                'receiving',
                generateGroup('group1'),
                {column: 'receiving', group: generateGroup('group2')}
            )).toBe(false);
        });

        it('should return false if the given cell is not the one selected (different column)', () => {
            expect(component.isSelected(
                'receiving',
                generateGroup('group1'),
                {column: 'sending', group: generateGroup('group1')}
            )).toBe(false);
        });
    });

    describe('isRelatedWithCell', () => {
        it('should return false if the given related cell is null', () => {
            expect(component.isRelatedWithCell(
                'receiving',
                generateGroup('group1'),
                null,
                [
                    generateCommunicationRule('group1', ['group2', 'group3'])
                ])
            ).toBe(false);
        });

        it('should return false if they both are in the same column but with different ids', () => {
            expect(component.isRelatedWithCell(
                'receiving',
                generateGroup('group1'),
                {column: 'receiving', group: generateGroup('group2')},
                [
                    generateCommunicationRule('group1', ['group2', 'group3'])
                ])
            ).toBe(false);
        });

        it('should return true if the cells are the same', () => {
            expect(component.isRelatedWithCell(
                'receiving',
                generateGroup('group1'),
                {column: 'receiving', group: generateGroup('group1')},
                [
                    generateCommunicationRule('group1', ['group2', 'group3'])
                ])
            ).toBe(true);
        });

        it('should return true if the column and group match a sending group of the related receiving cell communication rules', () => {
            expect(component.isRelatedWithCell(
                'sending',
                generateGroup('group1'),
                {column: 'receiving', group: generateGroup('group2')},
                [
                    generateCommunicationRule('group1', ['group2'])
                ])
            ).toBe(true);
        });

        it('should return false if the column and group does not match a sending group of the related receiving cell communication rules', () => {
            expect(component.isRelatedWithCell(
                'sending',
                generateGroup('group1'),
                {column: 'receiving', group: generateGroup('group2')},
                [
                    generateCommunicationRule('group3', ['group2'])
                ])
            ).toBe(false);
        });

        it('should return true if the column and group match a receiving group of the related sending cell communication rules', () => {
            expect(component.isRelatedWithCell(
                'receiving',
                generateGroup('group1'),
                {column: 'sending', group: generateGroup('group2')},
                [
                    generateCommunicationRule('group2', ['group3', 'group1'])
                ])
            ).toBe(true);
        });

        it('should return false if the column and group does not match a receiving group of the related sending cell communication rules', () => {
            expect(component.isRelatedWithCell(
                'receiving',
                generateGroup('group1'),
                {column: 'sending', group: generateGroup('group2')},
                [
                    generateCommunicationRule('group2', ['group3'])
                ])
            ).toBe(false);
        });
    });

    describe('removeCommunication', () => {
        it('should ask for confirmation', () => {
            component.removeCommunication(generateGroup('group1'), generateGroup('group2'));
            expect(component.confirmationDisplayed).toBe(true);
        });
        it('should close the lightbox if the user cancel', () => {
            component.removeCommunication(generateGroup('group1'), generateGroup('group2'));
            component.confirmationClicked.next('cancel');
            expect(communicationRulesService.removeCommunication).not.toHaveBeenCalled();
            expect(component.confirmationDisplayed).toBe(false);
        });
        it('should call the communicationRulesService.removeCommunication if the user confirms', () => {
            component.removeCommunication(generateGroup('group1'), generateGroup('group2'));
            component.confirmationClicked.next('confirm');
            expect(communicationRulesService.removeCommunication).toHaveBeenCalled();
            expect(component.confirmationDisplayed).toBe(false);
        });
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

    @Input()
    active: boolean = false;

    @Input()
    selected: boolean = false;

    @Input()
    highlighted: boolean = false;
}
