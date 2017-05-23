import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SaveSearchComponent } from './save-search.component';

describe('SaveSearchComponent', () => {
  let component: SaveSearchComponent;
  let fixture: ComponentFixture<SaveSearchComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SaveSearchComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SaveSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
