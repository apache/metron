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
import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { ConfirmationType } from '../model/confirmation-type';
import { DialogType } from '../model/dialog-type';

@Injectable({
  providedIn: 'root'
})

export class DialogParams {
  show = false;
  message = '';
  dialogType = DialogType.Confirmation;
}

export class DialogService {
  message = new BehaviorSubject<DialogParams>(new DialogParams());
  confirmed = new Subject<ConfirmationType>();

  constructor() {}

  launchDialog(message: string, dialogType = DialogType.Confirmation): Subject<ConfirmationType> {
    this.message.next({
      message: message,
      show: true,
      dialogType: dialogType
    });
    return this.confirmed;
  }

  approve() {
    this.confirmed.next(ConfirmationType.Confirmed);
  }

  cancel() {
    this.confirmed.next(ConfirmationType.Rejected);
  }
}
