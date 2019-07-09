import { ShowHideAlertEntriesComponent, ShowHideChanged } from './show-hide-alert-entries.component';
import { ComponentFixture, async, TestBed, getTestBed } from '@angular/core/testing';
import { SwitchComponent } from 'app/shared/switch/switch.component';
import { QueryBuilder } from 'app/alerts/alerts-list/query-builder';
import { By } from '@angular/platform-browser';
import { Filter } from 'app/model/filter';
import { Spy } from 'jasmine-core';

class QueryBuilderMock {
  addOrUpdateFilter = () => {};
  removeFilter = () => {};
}

describe('ShowHideAlertEntriesComponent', () => {

  let component: ShowHideAlertEntriesComponent;
  let fixture: ComponentFixture<ShowHideAlertEntriesComponent>;
  let queryBuilderMock: QueryBuilderMock;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ShowHideAlertEntriesComponent,
        SwitchComponent
      ],
      providers: [
        { provide: QueryBuilder, useClass: QueryBuilderMock },
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    queryBuilderMock = getTestBed().get(QueryBuilder);
    fixture = TestBed.createComponent(ShowHideAlertEntriesComponent);
    component = fixture.componentInstance;

    spyOn(localStorage, 'getItem').and.returnValues('true', 'false');
    spyOn(localStorage, 'setItem');
  });

  it('should get persisted state from localStorage onNgInit', () => {
    fixture.detectChanges();

    expect(localStorage.getItem).toHaveBeenCalledWith(component.HIDE_RESOLVE_STORAGE_KEY);
    expect(localStorage.getItem).toHaveBeenCalledWith(component.HIDE_DISMISS_STORAGE_KEY);
  });

  it('should set initial filter state onNgInit', () => {
    spyOn(component, 'onVisibilityChanged');

    fixture.detectChanges();

    expect((component.onVisibilityChanged as Spy).calls.argsFor(0)[0]).toBe('RESOLVE');
    expect((component.onVisibilityChanged as Spy).calls.argsFor(0)[1]).toBe(true);
    expect((component.onVisibilityChanged as Spy).calls.argsFor(1)[0]).toBe('DISMISS');
    expect((component.onVisibilityChanged as Spy).calls.argsFor(1)[1]).toBe(false);
  });

  it('should save state to localStorage on change for RESOLVE', () => {
    component.onVisibilityChanged('RESOLVE', true);

    expect(localStorage.setItem).toHaveBeenCalledWith(component.HIDE_RESOLVE_STORAGE_KEY, true);
  });

  it('should save state to localStorage on change for DISMISS', () => {
    component.onVisibilityChanged('DISMISS', true);

    expect(localStorage.setItem).toHaveBeenCalledWith(component.HIDE_DISMISS_STORAGE_KEY, true);
  });

  it('should listen to change event on hide resolved toggle', () => {
    fixture.detectChanges(); // triggering ngInit to not disturb this test
    spyOn(component, 'onVisibilityChanged');

    fixture.debugElement.query(By.css('[data-qe-id="hideResolvedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    // it set true by localStorage.getItem, so after first click is false
    expect(component.onVisibilityChanged).toHaveBeenCalledWith('RESOLVE', false);

    fixture.debugElement.query(By.css('[data-qe-id="hideResolvedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    expect(component.onVisibilityChanged).toHaveBeenCalledWith('RESOLVE', true);
  });

  it('should listen to change event on hide dismissed toggle', () => {
    fixture.detectChanges(); // triggering ngInit to not disturb this test
    spyOn(component, 'onVisibilityChanged');

    fixture.debugElement.query(By.css('[data-qe-id="hideDismissedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    expect(component.onVisibilityChanged).toHaveBeenCalledWith('DISMISS', true);

    fixture.debugElement.query(By.css('[data-qe-id="hideDismissedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    expect(component.onVisibilityChanged).toHaveBeenCalledWith('DISMISS', false);
  });

  it('should be able to add RESOLVE filter to QueryBuilder', () => {
    spyOn(queryBuilderMock, 'addOrUpdateFilter');
    spyOn(queryBuilderMock, 'removeFilter');

    component.onVisibilityChanged('RESOLVE', true);
    expect(queryBuilderMock.addOrUpdateFilter).toHaveBeenCalledWith(new Filter('-alert_status', 'RESOLVE', false));
    expect(queryBuilderMock.removeFilter).not.toHaveBeenCalled();
  });

  it('should be able to remove RESOLVE filter to QueryBuilder', () => {
    spyOn(queryBuilderMock, 'addOrUpdateFilter');
    spyOn(queryBuilderMock, 'removeFilter');

    component.onVisibilityChanged('RESOLVE', false);
    expect(queryBuilderMock.removeFilter).toHaveBeenCalledWith(new Filter('-alert_status', 'RESOLVE', false));
    expect(queryBuilderMock.addOrUpdateFilter).not.toHaveBeenCalled();
  });

  it('should be able to add DISMISS filter to QueryBuilder', () => {
    spyOn(queryBuilderMock, 'addOrUpdateFilter');
    spyOn(queryBuilderMock, 'removeFilter');

    component.onVisibilityChanged('DISMISS', true);
    expect(queryBuilderMock.addOrUpdateFilter).toHaveBeenCalledWith(new Filter('-alert_status', 'DISMISS', false));
    expect(queryBuilderMock.removeFilter).not.toHaveBeenCalled();
  });

  it('should be able to remove DISMISS filter to QueryBuilder', () => {
    spyOn(queryBuilderMock, 'addOrUpdateFilter');
    spyOn(queryBuilderMock, 'removeFilter');

    component.onVisibilityChanged('DISMISS', false);
    expect(queryBuilderMock.removeFilter).toHaveBeenCalledWith(new Filter('-alert_status', 'DISMISS', false));
    expect(queryBuilderMock.addOrUpdateFilter).not.toHaveBeenCalled();
  });

  it('should trigger changed event on any toggle changes', () => {
    spyOn(component.changed, 'emit');
    fixture.detectChanges();

    fixture.debugElement.query(By.css('[data-qe-id="hideDismissedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    // onVisibilityChanged called two times from ngInit therefore we start with argsFor(2)
    expect((component.changed.emit as Spy).calls.argsFor(2)[0]).toEqual(new ShowHideChanged('DISMISS', true));

    fixture.debugElement.query(By.css('[data-qe-id="hideResolvedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    // it set true by localStorage.getItem, so after first click is false
    expect((component.changed.emit as Spy).calls.argsFor(3)[0]).toEqual(new ShowHideChanged('RESOLVE', false));

    fixture.debugElement.query(By.css('[data-qe-id="hideDismissedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    expect((component.changed.emit as Spy).calls.argsFor(4)[0]).toEqual(new ShowHideChanged('DISMISS', false));

    fixture.debugElement.query(By.css('[data-qe-id="hideResolvedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    expect((component.changed.emit as Spy).calls.argsFor(5)[0]).toEqual(new ShowHideChanged('RESOLVE', true));
  })

});
