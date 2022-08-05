import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {AlertesTraiteesListModalComponent} from './alertes-traitees-list-modal.component';


describe('AlertesTraiteesListModalComponent', () => {
  let component: AlertesTraiteesListModalComponent;
  let fixture: ComponentFixture<AlertesTraiteesListModalComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ AlertesTraiteesListModalComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AlertesTraiteesListModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
