import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { UserCommunicationComponent, userCommunicationLocators as locators } from './user-communication.component';
import { UserDetailsModel } from '../../core/store/models';
import { By } from '@angular/platform-browser';
import { BundlesService, SijilModule } from 'sijil';
import { DebugElement } from '@angular/core';
import { UxModule } from '../../shared/ux/ux.module';

describe('UserCommunicationComponent', () => {
    let component: UserCommunicationComponent;
    let fixture: ComponentFixture<UserCommunicationComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                UserCommunicationComponent
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

        component.user = generateUser('Axelle', 'Potier');
        fixture.detectChanges();
    }));

    it('should create the UserCommunicationComponent component', async(() => {
        expect(component).toBeTruthy();
    }));

    it('should have the title "Communication de POTIER Axelle" given user Axelle Potier', async(() => {
        expect(getText(getTitle(fixture))).toBe('Communication de POTIER Axelle');
    }));

    it('should have the title "Communication de POTTER Harry" given user Harry Potter', async(() => {
        component.user = generateUser('Harry', 'Potter');
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

function generateUser(firstName: string, lastName: string): UserDetailsModel {
    return {firstName, lastName} as UserDetailsModel;
}

function getTitle(fixture: ComponentFixture<UserCommunicationComponent>): DebugElement {
    return fixture.debugElement.query(By.css(locators.title));
}

function getBackButton(fixture: ComponentFixture<UserCommunicationComponent>): DebugElement {
    return fixture.debugElement.query(By.css(locators.backButton));
}

function getText(el: DebugElement): string {
    return el.nativeElement.textContent;
}

function clickOn(el: DebugElement): void {
    return el.triggerEventHandler('click', null);
}
