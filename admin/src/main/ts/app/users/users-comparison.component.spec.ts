import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, Input } from '@angular/core';
import { UsersComparisonComponent } from './users-comparison.component';
import { UserOverview } from './user-overview.component';

describe('UsersComparisonComponent', () => {
    let fixture: ComponentFixture<UsersComparisonComponent>;
    let component: UsersComparisonComponent;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [UsersComparisonComponent, MockUserOverviewComponent],
            imports: []

        }).compileComponents();
        fixture = TestBed.createComponent(UsersComparisonComponent);
        component = fixture.componentInstance;
    }));

    it('should create the UsersComparisonComponent component', async(() => {
        expect(component).toBeTruthy();
    }));
});

@Component({
    selector: 'user-overview',
    template: ''
})
class MockUserOverviewComponent {
    @Input()
    user: UserOverview;
}
