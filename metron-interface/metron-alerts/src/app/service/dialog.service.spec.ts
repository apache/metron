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

    it('should emit a message with the correct params', () => {
      const messageSpy = spyOn(dialogService.message, 'next');
      const testMessage = 'this is a test';
      let messageEmit: DialogParams = {
        message: testMessage,
        show: true,
        dialogType: DialogType.Confirmation
      };

      dialogService.launchDialog(testMessage);
      expect(messageSpy).toHaveBeenCalledWith(messageEmit);

      messageEmit.dialogType = DialogType.Error;
      dialogService.launchDialog(testMessage, DialogType.Error);
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
