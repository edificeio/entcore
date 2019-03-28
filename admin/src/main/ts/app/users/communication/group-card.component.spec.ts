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
import { GroupNameService } from "./group-name.service";

describe('GroupCardComponent', () => {
    let component: GroupCardComponent;
    let communicationRulesService: CommunicationRulesService;
    let notifyService: NotifyService;
    let groupNameService: GroupNameService;
    let spinnerService: SpinnerService;
    let activatedRoute: ActivatedRoute;
    let fixture: ComponentFixture<GroupCardComponent>;

    beforeEach(async(() => {
        communicationRulesService = jasmine.createSpyObj('CommunicationRulesService', ['toggleInternalCommunicationRule']);
        notifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        groupNameService = jasmine.createSpyObj('GroupNameService', ['getGroupName']);
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
                {useValue: groupNameService, provide: GroupNameService},
                {useValue: activatedRoute, provide: ActivatedRoute}
            ],
            imports: [
                SijilModule.forRoot(),
                UxModule.forRoot(null)
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(GroupCardComponent);
        component = fixture.debugElement.componentInstance;
        component.group = generateGroup('Elèves du Lycée Paul Martin');
        (groupNameService.getGroupName as jasmine.Spy).and.returnValue('Elèves du Lycée Paul Martin');
        component.active = true;
        fixture.detectChanges();
    }));

    it('should create the GroupCardComponent component', async(() => {
        expect(component).toBeTruthy();
    }));

    it('should display the name of the given group "Elèves du Lycée Paul Martin" using the groupNameService', async(() => {
        expect(getText(getTitle(fixture))).toBe('Elèves du Lycée Paul Martin');
    }));

    it('should display the name of the given group "test" using the groupNameService', async(() => {
        component.group = generateGroup('test');
        (groupNameService.getGroupName as jasmine.Spy).and.returnValue('test');
        fixture.detectChanges();
        expect(getText(getTitle(fixture))).toBe('test');
    }));

    it('should call the communicationRulesService.toggleInternalCommunicationRule when clicking on the communication rules switch and confirming the change', async(() => {
        component.group = generateGroup('test');
        fixture.detectChanges();
        clickOn(getInternalCommunicationSwitch(fixture));
        component.confirmationClicked.next('confirm');
        expect(communicationRulesService.toggleInternalCommunicationRule).toHaveBeenCalled();
    }));

    it('should emit a clickOnRemoveCommunication event when clicking on the remove communication button', async(() => {
        let emitted = false;
        component.clickOnRemoveCommunication.subscribe(() => emitted = true);
        clickOn(getRemoveCommunicationButton(fixture));
        expect(emitted).toBe(true);
    }));

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

function getRemoveCommunicationButton(fixture: ComponentFixture<GroupCardComponent>): DebugElement {
    return fixture.debugElement.query(By.css(locators.removeCommunicationButton));
}
