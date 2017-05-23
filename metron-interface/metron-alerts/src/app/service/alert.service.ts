import {Injectable, Inject} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import 'rxjs/add/observable/interval';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/onErrorResumeNext';

import {Alert} from '../model/alert';
import {Http, Headers, RequestOptions} from '@angular/http';
import {HttpUtil} from '../utils/httpUtil';
import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';
import {QueryBuilder} from '../model/query-builder';

@Injectable()
export class AlertService {

  interval = 80000;
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};
  types = ['bro_doc', 'snort_doc'];

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {
  }

  public search(queryBuilder: QueryBuilder): Observable<{}> {
    let url = '/search/*,-*kibana/_search';
    return this.http.post(url, queryBuilder.getESSearchQuery(), new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public pollSearch(queryBuilder: QueryBuilder): Observable<{}> {
    let url = '/search/*,-*kibana/_search';
    return Observable.interval(this.interval * 1000).switchMap(() => {
      return this.http.post(url, queryBuilder.getESSearchQuery(), new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError)
        .onErrorResumeNext();
    });
  }

  public getAlert(index: string, type: string, alertId: string): Observable<Alert> {
    return this.http.get('/search/' + index + '/' + type + '/' + alertId, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData);
  }

  public updateAlertState(alerts: Alert[], state: string, workflowId: string) {
    let request = '';
    for (let alert of alerts) {
      request += '{ "update" : { "_index" : "' + alert._index + '", "_type" : "' + alert._type + '", "_id" : "' + alert._id + '" } }\n' +
                  '{ "doc": { "alert_status": "' + state + '"';
      if (workflowId) {
        request += ', "workflow_id": "' + workflowId + '"';
      }
      request += ' }}\n';
    }
    return this.http.post('/search/_bulk', request, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }
}
