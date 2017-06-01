import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigureRowsComponent } from './configure-rows.component';

describe('ConfigureRowsComponent', () => {
  let component: ConfigureRowsComponent;
  let fixture: ComponentFixture<ConfigureRowsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ConfigureRowsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ConfigureRowsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
