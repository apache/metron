import { ShowHideAlertEntriesComponent, ShowHideChanged } from './show-hide-alert-entries.component';
import { ComponentFixture, async, TestBed } from '@angular/core/testing';
import { SwitchComponent } from 'app/shared/switch/switch.component';
import { By } from '@angular/platform-browser';
import { Spy } from 'jasmine-core';
import { ShowHideService } from './show-hide.service';

describe('ShowHideAlertEntriesComponent', () => {

  let component: ShowHideAlertEntriesComponent;
  let fixture: ComponentFixture<ShowHideAlertEntriesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ShowHideAlertEntriesComponent,
        SwitchComponent
      ],
      providers: [
        { provide: ShowHideService, useClass: () => {
          return {
            hideDismissed: false,
            hideResolved: false,
            setFilterFor: jasmine.createSpy('setFilterFor')
          }
        } },
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ShowHideAlertEntriesComponent);
    component = fixture.componentInstance;
  });

  it('should have ShowHideService injected', () => {
    expect(component.showHideService).toBeTruthy();
  });

  it('should have ShowHideService.hideDismissed bounded to the dismissed toggle', () => {
    expect(fixture.debugElement.query(By.css('[data-qe-id="hideResolvedAlertsToggle"] input')).nativeElement.checked).toBe(false);

    component.showHideService.hideResolved = true;
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('[data-qe-id="hideResolvedAlertsToggle"] input')).nativeElement.checked).toBe(true);
  });

  it('should have ShowHideService.hideResolved bounded to the resolved toggle', () => {
    expect(fixture.debugElement.query(By.css('[data-qe-id="hideDismissedAlertsToggle"] input')).nativeElement.checked).toBe(false);

    component.showHideService.hideDismissed = true;
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('[data-qe-id="hideDismissedAlertsToggle"] input')).nativeElement.checked).toBe(true);
  });

  it('should listen to change event on hide resolved toggle', () => {
    fixture.detectChanges(); // triggering ngInit to not disturb this test
    spyOn(component, 'onVisibilityChanged');

    fixture.debugElement.query(By.css('[data-qe-id="hideResolvedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    // it set true by localStorage.getItem, so after first click is false
    expect(component.onVisibilityChanged).toHaveBeenCalledWith('RESOLVE', true);

    fixture.debugElement.query(By.css('[data-qe-id="hideResolvedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    expect(component.onVisibilityChanged).toHaveBeenCalledWith('RESOLVE', false);
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

  it('should trigger changed event on any toggle changes', () => {
    spyOn(component.changed, 'emit');
    fixture.detectChanges();

    fixture.debugElement.query(By.css('[data-qe-id="hideDismissedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    expect((component.changed.emit as Spy).calls.argsFor(0)[0]).toEqual(new ShowHideChanged('DISMISS', true));

    fixture.debugElement.query(By.css('[data-qe-id="hideResolvedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    expect((component.changed.emit as Spy).calls.argsFor(1)[0]).toEqual(new ShowHideChanged('RESOLVE', true));

    fixture.debugElement.query(By.css('[data-qe-id="hideDismissedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    expect((component.changed.emit as Spy).calls.argsFor(2)[0]).toEqual(new ShowHideChanged('DISMISS', false));

    fixture.debugElement.query(By.css('[data-qe-id="hideResolvedAlertsToggle"] input')).nativeElement.click();
    fixture.detectChanges();

    expect((component.changed.emit as Spy).calls.argsFor(3)[0]).toEqual(new ShowHideChanged('RESOLVE', false));
  })

});
