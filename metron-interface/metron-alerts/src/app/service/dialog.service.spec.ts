import { TestBed, inject } from '@angular/core/testing';

import {
  DialogService,
  DialogParams
} from './dialog.service';
import { DialogType } from '../model/dialog-type';
import { ConfirmationType } from '../model/confirmation-type';

describe('DialogService', () => {
  let dialogService: DialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DialogService]
    });
    dialogService = TestBed.get(DialogService);
  });

  it('should be created', inject([DialogService], (service: DialogService) => {
    expect(service).toBeTruthy();
  }));

  describe('confirm()', () => {
    it('should emit confirmed with ConfirmationType.Initial', () => {
      const confirmedSpy = spyOn(dialogService.confirmed, 'next');

      dialogService.confirm('');
      expect(confirmedSpy).toHaveBeenCalledWith(ConfirmationType.Initial);
    });

    it('should emit a message with the correct params', () => {
      const messageSpy = spyOn(dialogService.message, 'next');
      const testMessage = 'this is a test';
      let messageEmit: DialogParams = {
        message: testMessage,
        show: true,
        dialogType: DialogType.Confirmation
      };

      dialogService.confirm(testMessage);
      expect(messageSpy).toHaveBeenCalledWith(messageEmit);

      messageEmit.dialogType = DialogType.Error;
      dialogService.confirm(testMessage, DialogType.Error);
    });

    it('should return a ConfirmationType', () => {
      const responseMock: ConfirmationType = ConfirmationType.Initial;

      dialogService.confirm('').subscribe(r => {
        expect(r).toBe(responseMock);
      });
    });
  });

  describe('cancel()', () => {
    it('should emit ConfirmationType.Rejected', () => {
      const messageSpy = spyOn(dialogService.confirmed, 'next');

      dialogService.cancel();
      expect(messageSpy).toHaveBeenCalledWith(ConfirmationType.Rejected);
    });
  });

  describe('approve()', () => {
    it('should emit ConfirmationType.Confirmed', () => {
      const messageSpy = spyOn(dialogService.confirmed, 'next');

      dialogService.approve();
      expect(messageSpy).toHaveBeenCalledWith(ConfirmationType.Confirmed);
    });
  });
});
