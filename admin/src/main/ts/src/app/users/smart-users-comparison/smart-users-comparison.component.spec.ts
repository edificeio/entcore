import {SmartUsersComparisonComponent} from './smart-users-comparison.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {UsersService} from '../users.service';
import {Component, Input} from '@angular/core';
import {UserOverview} from '../user-overview/user-overview.component';
import {of} from 'rxjs';

describe('SmartUsersComparisonComponent', () => {
    let fixture: ComponentFixture<SmartUsersComparisonComponent>;
    let component: SmartUsersComparisonComponent;
    let markForCheck: jasmine.Spy;
    let userService: UsersService;

    beforeEach(() => {
        userService = jasmine.createSpyObj('UserService', ['fetch']);
        (userService.fetch as jasmine.Spy).and.callFake(userId => of(generateUserOveview(userId)));
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [SmartUsersComparisonComponent, MockUsersComparisonComponent],
            providers: [{useValue: userService, provide: UsersService}]

        }).compileComponents();
        fixture = TestBed.createComponent(SmartUsersComparisonComponent);
        component = fixture.componentInstance;
        markForCheck = spyOn((component as any).changeDetectorRef, 'markForCheck');
    }));

    it('should create the SmartUsersComparisonComponent component', waitForAsync(() => {
        expect(component).toBeTruthy();
    }));

    it('should fetch the user overviews when component inits', waitForAsync(() => {
        component.user1 = 'user1';
        component.user2 = 'user2';
        fixture.detectChanges();
        expect((userService.fetch as jasmine.Spy).calls.count()).toBe(2);
        expect((userService.fetch as jasmine.Spy).calls.argsFor(0)).toEqual(['user1']);
        expect((userService.fetch as jasmine.Spy).calls.argsFor(1)).toEqual(['user2']);
        expect(markForCheck).toHaveBeenCalled();
    }));

    it('should fetch the user overviews when the given user ids change', waitForAsync(() => {
        component.user1 = 'user3';
        component.user2 = 'user4';
        component.ngOnChanges(null);
        expect((userService.fetch as jasmine.Spy).calls.count()).toBe(2);
        expect((userService.fetch as jasmine.Spy).calls.argsFor(0)).toEqual(['user3']);
        expect((userService.fetch as jasmine.Spy).calls.argsFor(1)).toEqual(['user4']);
        expect(markForCheck).toHaveBeenCalled();
    }));

});

@Component({
    selector: 'users-comparison',
    template: ''
})
class MockUsersComparisonComponent {
    @Input()
    user1: UserOverview;

    @Input()
    user2: UserOverview;
}

function generateUserOveview(id: string): UserOverview {
    return {
        activationCode: '',
        type: '',
        source: '',
        login: id,
        lastName: '',
        firstName: '',
        displayName: id,
        email: '',
        birthDate: '',
        structures: []
    };
}
