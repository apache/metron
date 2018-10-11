import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MetronDialogComponent } from './metron-dialog.component';

describe('MetronDialogComponent', () => {
  let component: MetronDialogComponent;
  let fixture: ComponentFixture<MetronDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MetronDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MetronDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show when ')
});
