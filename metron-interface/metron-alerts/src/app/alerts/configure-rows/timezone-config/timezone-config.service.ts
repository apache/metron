import { Injectable } from '@angular/core';
import { QueryBuilder } from 'app/alerts/alerts-list/query-builder';

@Injectable({
  providedIn: 'root'
})
export class TimezoneConfigService {

  public readonly CONVERT_UTC_TO_LOCAL_KEY = 'convertUTCtoLocal';

  showLocal = false;

  constructor(public queryBuilder: QueryBuilder) {
    this.showLocal = localStorage.getItem(this.CONVERT_UTC_TO_LOCAL_KEY) === 'true';
    this.toggleUTCtoLocal(this.showLocal);
  }

  toggleUTCtoLocal(isLocal: boolean) {
    this.showLocal = isLocal;
    localStorage.setItem(this.CONVERT_UTC_TO_LOCAL_KEY, isLocal.toString());
  }

  getTimezoneConfig() {
    return this.showLocal;
  }
}
