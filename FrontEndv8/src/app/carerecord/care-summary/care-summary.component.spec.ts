import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CareSummaryComponent } from './care-summary.component';

describe('CareSummaryComponent', () => {
  let component: CareSummaryComponent;
  let fixture: ComponentFixture<CareSummaryComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CareSummaryComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CareSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
