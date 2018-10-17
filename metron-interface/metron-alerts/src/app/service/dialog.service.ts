import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
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
  confirmed = new BehaviorSubject<ConfirmationType>(ConfirmationType.Initial);

  constructor() {}

  confirm(message: string, dialogType = DialogType.Confirmation): BehaviorSubject<ConfirmationType> {
    this.confirmed.next(ConfirmationType.Initial);
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
