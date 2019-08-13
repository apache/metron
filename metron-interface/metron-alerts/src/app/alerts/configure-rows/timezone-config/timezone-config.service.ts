import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TimezoneConfigService {

  public readonly CONVERT_UTC_TO_LOCAL_KEY = 'convertUTCtoLocal';

  showLocal = false;

  constructor() {
    this.showLocal = localStorage.getItem(this.CONVERT_UTC_TO_LOCAL_KEY) === 'true';
    this.toggleUTCtoLocal(this.showLocal);
  }

  toggleUTCtoLocal(isLocal: boolean) {
    localStorage.setItem(this.CONVERT_UTC_TO_LOCAL_KEY, isLocal.toString());
  }
}
