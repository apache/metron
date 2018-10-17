import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MetronDialogComponent } from './metron-dialog.component';
import { DialogService, DialogType } from 'app/service/dialog.service';

fdescribe('MetronDialogComponent', () => {
  let component: MetronDialogComponent;
  let fixture: ComponentFixture<MetronDialogComponent>;
  let dialogService: DialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MetronDialogComponent],
      providers: [DialogService]
    });
    fixture = TestBed.createComponent(MetronDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    dialogService = TestBed.get(DialogService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show when showModal is true', () => {
    component.showModal = false;
    fixture.detectChanges();
    let modal = fixture.nativeElement.querySelector('[data-qe-id="modal"]');
    expect(modal).toBeNull();

    component.showModal = true;
    fixture.detectChanges();
    modal = fixture.nativeElement.querySelector('[data-qe-id="modal"]');
    expect(modal).toBeTruthy();
  });

  it('should display the passed message', () => {
    const testMessage = 'This is a confirmation message';
    dialogService.confirm(testMessage);
    fixture.detectChanges();
    const modalMessage = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-message"]'
    );
    expect(modalMessage.textContent).toBe(testMessage);
  });

  it('should display the correct title based on the dialog type', () => {
    dialogService.confirm('');
    fixture.detectChanges();
    let modalTitle = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-title"]'
    );
    expect(modalTitle.textContent).toBe('Confirmation');

    dialogService.confirm('', DialogType.Error);
    fixture.detectChanges();
    modalTitle = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-title"]'
    );
    expect(modalTitle.textContent).toBe('Error');
  });

  it('should execute cancel() when the cancel button is clicked', () => {
    dialogService.confirm('');
    const cancelSpy = spyOn(component, 'cancel');
    fixture.detectChanges();

    const cancelButton = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-cancel"]'
    );
    cancelButton.click();
    expect(cancelSpy).toHaveBeenCalled();
  });

  it('should execute confirm() when the ok button is clicked', () => {
    dialogService.confirm('');
    const confirmSpy = spyOn(component, 'confirm');
    fixture.detectChanges();

    const confirmButton = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-confirm"]'
    );
    confirmButton.click();
    expect(confirmSpy).toHaveBeenCalled();
  });

  it('should only display a cancel() button when the dialog type is Error', () => {
    dialogService.confirm('', DialogType.Error);
    fixture.detectChanges();

    const errorCancelButton = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-error-cancel"]'
    );
    expect(errorCancelButton).toBeTruthy();

    const confirmCancelButton = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-cancel"]'
    );
    expect(confirmCancelButton).toBeNull();

    const confirmApproveButton = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-confirm"]'
    );
    expect(confirmApproveButton).toBeNull();
  });
});
