import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {UserOverviewComponent} from './user-overview.component';
import {SijilModule} from 'sijil';

describe('UserOverviewComponent', () => {
    let fixture: ComponentFixture<UserOverviewComponent>;
    let component: UserOverviewComponent;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [UserOverviewComponent],
            imports: [SijilModule.forRoot()]

        }).compileComponents();
        fixture = TestBed.createComponent(UserOverviewComponent);
        component = fixture.componentInstance;
    }));

    it('should create the UserOverviewComponent component', waitForAsync(() => {
        expect(component).toBeTruthy();
    }));
});
