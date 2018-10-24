/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MetronDialogComponent } from './metron-dialog.component';
import { DialogService } from 'app/service/dialog.service';
import { DialogType } from 'app/model/dialog-type';

describe('MetronDialogComponent', () => {
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
    dialogService.launchDialog(testMessage);
    fixture.detectChanges();
    const modalMessage = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-message"]'
    );
    expect(modalMessage.textContent).toBe(testMessage);
  });

  it('should display the correct title based on the dialog type', () => {
    dialogService.launchDialog('');
    fixture.detectChanges();
    let modalTitle = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-title"]'
    );
    expect(modalTitle.textContent).toBe('Confirmation');

    dialogService.launchDialog('', DialogType.Error);
    fixture.detectChanges();
    modalTitle = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-title"]'
    );
    expect(modalTitle.textContent).toBe('Error');
  });

  it('should execute cancel() when the cancel button is clicked', () => {
    dialogService.launchDialog('');
    const cancelSpy = spyOn(component, 'cancel').and.callThrough();
    fixture.detectChanges();

    const cancelButton = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-cancel"]'
    );
    cancelButton.click();
    expect(cancelSpy).toHaveBeenCalled();
    expect(component.showModal).toBe(false);
  });

  it('should execute confirm() when the ok button is clicked', () => {
    dialogService.launchDialog('');
    const confirmSpy = spyOn(component, 'confirm').and.callThrough();
    fixture.detectChanges();

    const confirmButton = fixture.nativeElement.querySelector(
      '[data-qe-id="modal-confirm"]'
    );
    confirmButton.click();
    expect(confirmSpy).toHaveBeenCalled();
    expect(component.showModal).toBe(false);
  });

  it('should only display a cancel() button when the dialog type is Error', () => {
    dialogService.launchDialog('', DialogType.Error);
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
