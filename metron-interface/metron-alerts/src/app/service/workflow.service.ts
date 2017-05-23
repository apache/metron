import {Injectable, Inject} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {Alert} from '../model/alert';
import {Http, Headers, RequestOptions} from '@angular/http';
import {HttpUtil} from '../utils/httpUtil';
import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';

@Injectable()
export class WorkflowService {

  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {
  }

  public start(alerts: Alert[]): Observable<string> {
    return this.http.post('/api/v1/workflow', alerts, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractString);
  }
}
