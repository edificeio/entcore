import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { UxModule } from '../../shared/ux/ux.module';
import { GroupCardComponent, groupCardLocators as locators } from './group-card.component';
import { BundlesService, SijilModule } from 'sijil';
import { clickOn, generateGroup, getText } from './communication-test-utils';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { CommunicationRulesService } from './communication-rules.service';
import { ActivatedRoute } from '@angular/router';
import { NotifyService, SpinnerService } from '../../core/services';

describe('GroupCardComponent', () => {
    let component: GroupCardComponent;
    let communicationRulesService: CommunicationRulesService;
    let notifyService: NotifyService;
    let spinnerService: SpinnerService;
    let activatedRoute: ActivatedRoute;
    let fixture: ComponentFixture<GroupCardComponent>;

    beforeEach(async(() => {
        communicationRulesService = jasmine.createSpyObj('CommunicationRulesService', ['toggleInternalCommunicationRule']);
        notifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        spinnerService = jasmine.createSpyObj('SpinnerService', ['perform']);
        activatedRoute = {root: {firstChild: {firstChild: {}}}} as ActivatedRoute;
        TestBed.configureTestingModule({
            declarations: [
                GroupCardComponent
            ],
            providers: [
                {useValue: communicationRulesService, provide: CommunicationRulesService},
                {useValue: spinnerService, provide: SpinnerService},
                {useValue: notifyService, provide: NotifyService},
                {useValue: activatedRoute, provide: ActivatedRoute}
            ],
            imports: [
                SijilModule.forRoot(),
                UxModule.forRoot(null)
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(GroupCardComponent);
        component = fixture.debugElement.componentInstance;
        const bundlesService = TestBed.get(BundlesService);
        bundlesService.addToBundle({
            "group.card.structure.Personnel": "Personnels de {{name}}",
            "group.card.structure.Relative": "Parents de {{name}}",
            "group.card.structure.Student": "Élèves de {{name}}",
            "group.card.structure.Teacher": "Enseignants de {{name}}",
            "group.card.structure.Guest": "Invités de {{name}}",
            "group.card.class.Personnel": "Personnels de la classe {{name}}",
            "group.card.class.Relative": "Parents de la classe {{name}}",
            "group.card.class.Student": "Élèves de la classe {{name}}",
            "group.card.class.Teacher": "Enseignants de la classe {{name}}",
            "group.card.class.Guest": "Invités de la classe {{name}}"
        });
        component.group = generateGroup('Elèves du Lycée Paul Martin');
        component.active = true;
        fixture.detectChanges();
    }));

    it('should create the GroupCardComponent component', async(() => {
        expect(component).toBeTruthy();
    }));

    it('should display the name of the given group "Elèves du Lycée Paul Martin"', async(() => {
        expect(getText(getTitle(fixture))).toBe('Elèves du Lycée Paul Martin');
    }));

    it('should display the name of the given group "test"', async(() => {
        component.group = generateGroup('test');
        fixture.detectChanges();
        expect(getText(getTitle(fixture))).toBe('test');
    }));

    it('should call the communicationRulesService.toggleInternalCommunicationRule when clicking on the communication rules switch', async(() => {
        component.group = generateGroup('test');
        fixture.detectChanges();
        clickOn(getInternalCommunicationSwitch(fixture));
        expect(communicationRulesService.toggleInternalCommunicationRule).toHaveBeenCalled();
    }));

    describe('getGroupName', () => {
        it(`should return 'test' if the group is a manual group named 'test'`, () => {
            expect(component.getGroupName(generateGroup('test', 'BOTH', 'ManualGroup'))).toBe('test');
        });

        it('should return a nice label for a ProfileGroup of a class (6emeA)', () => {
            expect(component.getGroupName(generateGroup('test', 'BOTH',
                'ProfileGroup', null,
                [{
                    id: '6A',
                    name: '6emeA'
                }], null, 'Student'))).toBe('Élèves de la classe 6emeA');
        });

        it('should return a nice label for a ProfileGroup of a structure (Emile Zola)', () => {
            expect(component.getGroupName(generateGroup('test', 'BOTH',
                'ProfileGroup', 'StructureGroup',
                null, [{
                    id: 'emilezola',
                    name: 'Emile Zola'
                }], 'Student'))).toBe('Élèves de Emile Zola');
        });
    });

    describe('viewMembers', () => {
        it(`should navigate to /admin/myStructure/groups/manual/groupId given a manual group with id groupId and structure id myStructure`, () => {
            spyOn(window, 'open');
            component.viewMembers(generateGroup('groupId',
                'BOTH',
                'ManualGroup', null, null,
                [{
                    id: 'myStructure',
                    name: 'myStructureName'
                }]));
            expect((window.open as jasmine.Spy).calls.mostRecent().args[0])
                .toEqual('/admin/myStructure/groups/manual/groupId');
        });
    });
});

function getTitle(fixture: ComponentFixture<GroupCardComponent>): DebugElement {
    return fixture.debugElement.query(By.css(locators.title));
}

function getInternalCommunicationSwitch(fixture: ComponentFixture<GroupCardComponent>): DebugElement {
    return fixture.debugElement.query(By.css(locators.internalCommunicationSwitch));
}
