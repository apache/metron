import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})

export class DialogParams {
  show = false;
  message = '';
  dialogType = DialogType.Confirmation;
}

export enum DialogType {
  Confirmation,
  Error
}

export enum ConfirmationType {
  Initial = 'Initial',
  Confirmed = 'Confirmed',
  Rejected = 'Rejected'
}

export class DialogService {
  message = new BehaviorSubject<DialogParams>(new DialogParams());
  message$ = this.message.asObservable();
  confirmed = new BehaviorSubject<ConfirmationType>(ConfirmationType.Initial);
  confirmed$ = this.confirmed.asObservable();

  constructor() {}

  confirm(message: string, dialogType = DialogType.Confirmation) {
    this.confirmed.next(ConfirmationType.Initial);
    this.message.next({
      'message': message,
      'show': true,
      'dialogType': dialogType,
    });
    return this.confirmed$;
  }

  approve() {
    this.confirmed.next(ConfirmationType.Confirmed);
  }

  cancel() {
    this.confirmed.next(ConfirmationType.Rejected);
  }

}
