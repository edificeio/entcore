import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BundlesService, SijilModule } from 'sijil';
import { Component, DebugElement, Input } from '@angular/core';
import { UxModule } from '../../shared/ux/ux.module';
import {
    CommunicationRule,
    CommunicationRulesComponent,
    communicationRulesLocators as locators,
    sortGroups,
    uniqueGroups
} from './communication-rules.component';
import { generateGroup } from '../../shared/utils';
import { GroupModel, StructureModel } from '../../core/store/models';
import { CommunicationRulesService } from './communication-rules.service';
import { GroupNameService, NotifyService } from '../../core/services';
import { UsersStore } from '../users.store';

describe('CommunicationRulesComponent', () => {
    let component: CommunicationRulesComponent;
    let notifyService: NotifyService;
    let groupNameService: GroupNameService;
    let communicationRulesService: CommunicationRulesService;
    let fixture: ComponentFixture<CommunicationRulesComponent>;
    let usersStoreMock: UsersStore;

    beforeEach(() => {
        usersStoreMock = {
            structure: {
                id: 'myStructure',
                groups: {
                    data: [
                        {id: 'myGroup1', name: 'myGroup1'},
                        {id: 'myGroup2', name: 'myGroup2'},
                        {id: 'myGroup3', name: 'myGroup3'}
                    ]
                }
            }
        } as UsersStore;
    });

    beforeEach(async(() => {
        communicationRulesService = jasmine.createSpyObj('CommunicationRulesService', ['removeCommunication', 'createRule']);
        notifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        groupNameService = jasmine.createSpyObj('GroupNameService', ['getGroupName']);
        (groupNameService.getGroupName as jasmine.Spy).and.callFake((g: GroupModel) => g.name);
        TestBed.configureTestingModule({
            declarations: [
                CommunicationRulesComponent,
                MockGroupCard
            ],
            providers: [
                {useValue: notifyService, provide: NotifyService},
                {useValue: groupNameService, provide: GroupNameService},
                {useValue: communicationRulesService, provide: CommunicationRulesService},
                {provide: UsersStore, useValue: usersStoreMock}
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
        component.addCommunicationPickableGroups = [generateGroup('group1')];
        component.activeColumn = 'sending';
        component.activeStructureId = 'activeStructure';
        component.manageableStructuresId = ['activeStructure'];
        component.structure = {id: 'activeStructure', name: 'activeStructure'} as StructureModel;
        component.structures = [
            {id: 'activeStructure', name: 'activeStructure'} as StructureModel,
            {id: 'structure2', name: 'structure2'} as StructureModel
        ];
        fixture.detectChanges();
    }));

    it('should create the CommunicationRulesComponent component', async(() => {
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

        describe('related to issue CAV2-416', () => {
            it('should check the column of the given cell, ', () => {
                expect(component.isRelatedWithCell(
                    'receiving',
                    generateGroup('group1'),
                    {column: 'sending', group: generateGroup('group2')},
                    [
                        generateCommunicationRule('group0', ['group1', 'group2']),
                        generateCommunicationRule('group1', []),
                        generateCommunicationRule('group2', [])
                    ])
                ).toBe(false);
            });

            it('should not highlight the given sending group in the receiving column', () => {
                expect(component.isRelatedWithCell(
                    'receiving',
                    generateGroup('group1'),
                    {column: 'sending', group: generateGroup('group1')},
                    [
                        generateCommunicationRule('group0', ['group1', 'group2']),
                        generateCommunicationRule('group1', []),
                        generateCommunicationRule('group2', [])
                    ])
                ).toBe(false);
            });
        });
    });

    describe('removeCommunication', () => {
        it('should ask for confirmation', () => {
            component.removeCommunication(generateGroup('group1'), generateGroup('group2'));
            expect(component.removeConfirmationDisplayed).toBe(true);
        });
        it('should close the lightbox if the user cancel', () => {
            component.removeCommunication(generateGroup('group1'), generateGroup('group2'));
            component.removeConfirmationClicked.next('cancel');
            expect(communicationRulesService.removeCommunication).not.toHaveBeenCalled();
            expect(component.removeConfirmationDisplayed).toBe(false);
        });
        it('should call the communicationRulesService.removeCommunication if the user confirms', () => {
            component.removeCommunication(generateGroup('group1'), generateGroup('group2'));
            component.removeConfirmationClicked.next('confirm');
            expect(communicationRulesService.removeCommunication).toHaveBeenCalled();
            expect(component.removeConfirmationDisplayed).toBe(false);
        });
    });

    describe('getSenders', () => {
        it('should filter null group', () => {
            component.communicationRules = [
                generateCommunicationRule('group1', ['group2']),
                {sender: null, receivers: [generateGroup('group3')]}
            ];
            const senders = component.getSenders();
            expect(senders.length).toBe(1);
            expect(senders[0].id).toBe('group1');
        });

        it('should return unique senders', () => {
            component.communicationRules = [
                generateCommunicationRule('group1', ['group2']),
                generateCommunicationRule('group1', ['group3'])
            ];
            const senders = component.getSenders();
            expect(senders.length).toBe(1);
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

describe('sortGroups', () => {
    const mockGetGroupNameFn = (g: GroupModel) => g.name;

    it('should sort groups by structures (first active structure, then based on groups number by structure)', () => {
        const groups = [
            generateGroupForStructure('group1', 'structure1'),
            generateGroupForStructure('group2', 'structure1'),
            generateGroupForStructure('group3', 'structure1'),
            generateGroupForStructure('group4', 'structure1'),
            generateGroupForStructure('group5', 'activeStructure'),
            generateGroupForStructure('group6', 'activeStructure'),
            generateGroupForStructure('group7', 'structure2'),
            generateGroupForStructure('group8', 'structure3'),
            generateGroupForStructure('group9', 'structure3')
        ];
        const sortedGroups = sortGroups(groups, mockGetGroupNameFn, 'activeStructure');
        expect(sortedGroups.map(s => s.id)).toEqual([
            'group5',
            'group6',
            'group1',
            'group2',
            'group3',
            'group4',
            'group8',
            'group9',
            'group7'
        ]);
    });

    it('should sort groups alphabetically in their structure group', () => {
        const groups = [
            generateGroupForStructure('dgroup1', 'structure1'),
            generateGroupForStructure('agroup2', 'structure1'),
            generateGroupForStructure('cgroup3', 'structure1'),
            generateGroupForStructure('bgroup4', 'structure1'),
            generateGroupForStructure('bgroup5', 'activeStructure'),
            generateGroupForStructure('agroup6', 'activeStructure')
        ];
        const sortedGroups = sortGroups(groups, mockGetGroupNameFn, 'activeStructure');
        expect(sortedGroups.map(s => s.id)).toEqual([
            'agroup6',
            'bgroup5',
            'agroup2',
            'bgroup4',
            'cgroup3',
            'dgroup1'
        ]);
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

function generateGroupForStructure(groupName: string, structureId: string) {
    return generateGroup(groupName, "BOTH",
        null, null, null,
        [{id: structureId, name: structureId}]);
}


@Component({
    selector: 'group-card',
    template: ''
})
class MockGroupCard {
    @Input()
    group: GroupModel;

    @Input()
    manageable: boolean = false;

    @Input()
    communicationRuleManageable: boolean = false;

    @Input()
    active: boolean = false;

    @Input()
    selectable: boolean = false;

    @Input()
    selected: boolean = false;

    @Input()
    highlighted: boolean = false;
}
