import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { TraitementAlerteModalComponent } from './traitement-alerte-modal.component';

describe('TraitementAlerteModalComponent', () => {
  let component: TraitementAlerteModalComponent;
  let fixture: ComponentFixture<TraitementAlerteModalComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ TraitementAlerteModalComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TraitementAlerteModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
