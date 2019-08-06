import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalLoadingIndicatorComponent } from './modal-loading-indicator.component';

describe('ModalLoadingIndicatorComponent', () => {
  let component: ModalLoadingIndicatorComponent;
  let fixture: ComponentFixture<ModalLoadingIndicatorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ModalLoadingIndicatorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ModalLoadingIndicatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
