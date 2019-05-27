import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { UserCommunicationComponent, userCommunicationLocators as locators } from './user-communication.component';
import { GroupModel, UserDetailsModel, UserModel } from '../../core/store/models';
import { By } from '@angular/platform-browser';
import { BundlesService, SijilModule } from 'sijil';
import { Component, DebugElement, Input } from '@angular/core';
import { UxModule } from '../../shared/ux/ux.module';
import { Column, CommunicationRule } from './communication-rules.component';
import { clickOn, generateGroup, getText } from '../../shared/utils';

describe('UserCommunicationComponent', () => {
    let component: UserCommunicationComponent;
    let fixture: ComponentFixture<UserCommunicationComponent>;
    let axellePotier: UserCommunicationTestingData;
    let harryPotter: UserCommunicationTestingData;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                UserCommunicationComponent,
                MockCommunicationRulesComponent
            ],
            providers: [],
            imports: [
                SijilModule.forRoot(),
                UxModule.forRoot(null)
            ]

        }).compileComponents();
        fixture = TestBed.createComponent(UserCommunicationComponent);
        component = fixture.debugElement.componentInstance;
        const bundlesService = TestBed.get(BundlesService);
        bundlesService.addToBundle({
            "user.communication.back-to-user-details": "Retour à la fiche",
            "user.communication.title": "Communication de {{ lastName }} {{ firstName }}"
        });

        axellePotier = generateTestingData(
            'Axelle',
            'Potier',
            [generateGroup('c1')],
            [generateGroup('groupf1'), generateGroup('groupf2')],
            [generateGroup('groupm1')]);
        harryPotter = generateTestingData(
            'Harry',
            'Potter',
            [generateGroup('c1')],
            null,
            null);

        component.user = axellePotier.user;
        component.userSendingCommunicationRules = axellePotier.communicationRules;
        component.addCommunicationPickableGroups = [generateGroup('group1')];
        fixture.detectChanges();
    }));

    it('should create the UserCommunicationComponent component', async(() => {
        expect(component).toBeTruthy();
    }));

    it('should have the title "Communication de POTIER Axelle" given user Axelle Potier', async(() => {
        expect(getText(getTitle(fixture))).toBe('Communication de POTIER Axelle');
    }));

    it('should have the title "Communication de POTTER Harry" given user Harry Potter', async(() => {
        component.user = harryPotter.user;
        component.userSendingCommunicationRules = harryPotter.communicationRules;
        fixture.detectChanges();
        expect(getText(getTitle(fixture))).toBe('Communication de POTTER Harry');
    }));

    it('should emit a "close" event with clicking on the back button', async(() => {
        let closed = false;
        component.close.subscribe(() => closed = true);
        clickOn(getBackButton(fixture));
        expect(closed).toBeTruthy();
    }));
});

interface UserCommunicationTestingData {
    user: UserModel,
    communicationRules: CommunicationRule[]
}

function generateTestingData(firstName: string, lastName: string, classes: GroupModel[], functionalGroups: GroupModel[], manualGroups: GroupModel[]): UserCommunicationTestingData {
    const userDetails: UserDetailsModel = {functionalGroups, manualGroups} as UserDetailsModel;
    const user: UserModel = {
        firstName,
        lastName,
        userDetails
    } as UserModel;
    const groups = [];
    groups.push(...classes);
    groups.push(...functionalGroups);
    groups.push(...manualGroups);
    const communicationRules: CommunicationRule[] = groups.map(mg => ({sender: mg, receivers: []}));
    return {user, communicationRules};
}

function getTitle(fixture: ComponentFixture<UserCommunicationComponent>): DebugElement {
    return fixture.debugElement.query(By.css(locators.title));
}

function getBackButton(fixture: ComponentFixture<UserCommunicationComponent>): DebugElement {
    return fixture.debugElement.query(By.css(locators.backButton));
}

@Component({
    selector: 'communication-rules',
    template: ''
})
class MockCommunicationRulesComponent {
    @Input()
    sendingHeaderLabel: string;

    @Input()
    receivingHeaderLabel: string;

    @Input()
    communicationRules: CommunicationRule[];

    @Input()
    public addCommunicationPickableGroups: GroupModel[];

    @Input()
    activeColumn: Column;
}
