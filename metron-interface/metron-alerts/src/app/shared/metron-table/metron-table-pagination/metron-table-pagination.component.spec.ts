import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MetronTablePaginationComponent } from './metron-table-pagination.component';

describe('MetronTablePaginationComponent', () => {
  let component: MetronTablePaginationComponent;
  let fixture: ComponentFixture<MetronTablePaginationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MetronTablePaginationComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MetronTablePaginationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
