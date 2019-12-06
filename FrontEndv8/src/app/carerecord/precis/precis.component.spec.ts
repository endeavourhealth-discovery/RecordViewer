import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PrecisComponent } from './precis.component';

describe('PrecisComponent', () => {
  let component: PrecisComponent;
  let fixture: ComponentFixture<PrecisComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PrecisComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PrecisComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
