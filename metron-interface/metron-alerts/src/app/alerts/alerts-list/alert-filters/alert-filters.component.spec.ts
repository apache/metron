import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CollapseComponent } from '../../../shared/collapse/collapse.component';
import { AlertFiltersComponent } from './alert-filters.component';
import { CenterEllipsesPipe } from '../../../shared/pipes/center-ellipses.pipe';
import { ColumnNameTranslatePipe } from '../../../shared/pipes/column-name-translate.pipe';

describe('AlertFiltersComponent', () => {
  let component: AlertFiltersComponent;
  let fixture: ComponentFixture<AlertFiltersComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ 
        CollapseComponent,
        CenterEllipsesPipe,
        ColumnNameTranslatePipe,
        AlertFiltersComponent,
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AlertFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
