import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TimezoneConfigComponent } from './timezone-config.component';

describe('TimezoneConfigComponent', () => {
  let component: TimezoneConfigComponent;
  let fixture: ComponentFixture<TimezoneConfigComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TimezoneConfigComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TimezoneConfigComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
