import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { UserOverviewComponent } from './user-overview.component';
import { SijilModule } from 'sijil';

describe('UserOverviewComponent', () => {
    let fixture: ComponentFixture<UserOverviewComponent>;
    let component: UserOverviewComponent;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [UserOverviewComponent],
            imports: [SijilModule.forRoot()]

        }).compileComponents();
        fixture = TestBed.createComponent(UserOverviewComponent);
        component = fixture.componentInstance;
    }));

    it('should create the UserOverviewComponent component', async(() => {
        expect(component).toBeTruthy();
    }));
});
