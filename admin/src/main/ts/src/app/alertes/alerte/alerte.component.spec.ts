import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AlerteComponent } from './alerte.component';

describe('AlerteComponent', () => {
  let component: AlerteComponent;
  let fixture: ComponentFixture<AlerteComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AlerteComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AlerteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
