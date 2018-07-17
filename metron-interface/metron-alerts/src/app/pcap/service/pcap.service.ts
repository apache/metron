import {Injectable, NgZone} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {Http, Headers, RequestOptions} from '@angular/http';
import {HttpUtil} from '../../utils/httpUtil';

import 'rxjs/add/operator/map';

import {PcapRequest} from '../model/pcap.request';
import {Pdml} from '../model/pdml'

export class PcapStatusRespons {
    status: string;
    processPercentage: number;
    totalPages: number;
}

@Injectable()
export class PcapService {

    private statusInterval = 4;
    defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

    constructor(private http: Http, private ngZone: NgZone) {
    }

    public pollStatus(id: string): Observable<{}> {
        return this.ngZone.runOutsideAngular(() => {
            return this.ngZone.run(() => {
                return Observable.interval(this.statusInterval * 1000).switchMap(() => {
                    return this.getStatus(id);
                });
            });
        });
    }

    public submitRequest(pcapRequest: PcapRequest): Observable<string> {
        return this.http.post('/api/v1/pcap/pcapqueryfilterasync/submit', pcapRequest, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
            .map(result => JSON.parse(result.text()).id)
            .catch(HttpUtil.handleError)
            .onErrorResumeNext();
    }

    public getStatus(id: string): Observable<PcapStatusRespons> {
        return this.http.get('/api/v1/pcap/pcapqueryfilterasync/status?idQuery=' + id,
            new RequestOptions({headers: new Headers(this.defaultHeaders)}))
            .map(HttpUtil.extractData)
            .catch(HttpUtil.handleError)
            .onErrorResumeNext();
    }

    public getPackets(id: string): Observable<Pdml> {
        return this.http.get('/api/v1/pcap/pcapqueryfilterasync/resultJson?idQuery=' + id, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
            .map(HttpUtil.extractData)
            .catch(HttpUtil.handleError)
            .onErrorResumeNext();
    }

    public getDownloadUrl(id: string) {
      return `/api/v1/pcap/pcapqueryfilterasync/download?idQuery=${id}`;
    }
}
