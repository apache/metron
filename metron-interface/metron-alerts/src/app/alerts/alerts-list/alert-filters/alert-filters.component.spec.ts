import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AlertFiltersComponent } from './alert-filters.component';

describe('AlertFiltersComponent', () => {
  let component: AlertFiltersComponent;
  let fixture: ComponentFixture<AlertFiltersComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AlertFiltersComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AlertFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
