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
import { async, ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';

import { AlertDetailsComponent, AlertCommentWrapper } from './alert-details.component';
import { SharedModule } from 'app/shared/shared.module';
import { AlertDetailsKeysPipe } from './alert-details-keys.pipe';
import { AuthenticationService } from 'app/service/authentication.service';
import { AlertsService } from 'app/service/alerts.service';
import { UpdateService } from 'app/service/update.service';
import { RouterTestingModule } from '@angular/router/testing';
import { SearchService } from 'app/service/search.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AppConfigService } from 'app/service/app-config.service';
import { GlobalConfigService } from 'app/service/global-config.service';
import { DataSource } from 'app/service/data-source';
import { ElasticSearchLocalstorageImpl } from 'app/service/elasticsearch-localstorage-impl';
import { DialogService } from 'app/service/dialog.service';
import { By } from '@angular/platform-browser';
import { AlertComment } from './alert-comment';
import { Subject } from 'rxjs';
import { ConfirmationType } from 'app/model/confirmation-type';
import {CommentAddRemoveRequest} from "../../model/comment-add-remove-request";
import {AlertSource} from "../../model/alert-source";
import {of} from "rxjs/index";

const alertDetail = {
  'enrichments:geo:ip_dst_addr:locID': '5308655',
  'bro_timestamp': '1554222181.202211',
  'status_code': 404,
  'enrichments:geo:ip_dst_addr:location_point': '33.4589,-112.0709',
  'ip_dst_port': 80,
  'enrichments:geo:ip_dst_addr:dmaCode': '753',
  'adapter:geoadapter:begin:ts': '1554727717061',
  'enrichments:geo:ip_dst_addr:latitude': '33.4589',
  'parallelenricher:enrich:end:ts': '1554727717078',
  'uid': 'CBt5FP2TVetZJjaZbi',
  'resp_mime_types': ['text/html'],
  'trans_depth': 1,
  'protocol': 'http',
  'source:type': 'bro',
  'adapter:threatinteladapter:end:ts': '1554727717076',
  'original_string': 'HTTP | id.orig_p:49199',
  'ip_dst_addr': '204.152.254.221',
  'adapter:hostfromjsonlistadapter:end:ts': '1554727717069',
  'host': 'runlove.us',
  'adapter:geoadapter:end:ts': '1554727717069',
  'ip_src_addr': '192.168.138.158',
  'enrichments:geo:ip_dst_addr:longitude': '-112.0709',
  'user_agent': '',
  'resp_fuids': ['FeDAUx1IIW621Aw6Y8'],
  'timestamp': 1554222181202,
  'method': 'POST',
  'parallelenricher:enrich:begin:ts': '1554727717075',
  'request_body_len': 96,
  'enrichments:geo:ip_dst_addr:city': 'Phoenix',
  'enrichments:geo:ip_dst_addr:postalCode': '85004',
  'adapter:hostfromjsonlistadapter:begin:ts': '1554727717061',
  'orig_mime_types': ['text/plain'],
  'uri': '/wp-content/themes/twentyfifteen/img5.php?l=8r1gf1b2t1kuq42',
  'tags': [],
  'parallelenricher:splitter:begin:ts': '1554727717075',
  'alert_status': 'RESOLVE',
  'orig_fuids': ['FTTvSE5Asee5tJr99'],
  'ip_src_port': 49199,
  'parallelenricher:splitter:end:ts': '1554727717075',
  'adapter:threatinteladapter:begin:ts': '1554727717075',
  'status_msg': 'Not Found',
  'guid': 'fe9e058e-6d5a-4ba5-8b79-d8e6a2792931',
  'enrichments:geo:ip_dst_addr:country': 'US',
  'response_body_len': 357
};

describe('AlertDetailsComponent', () => {
  let component: AlertDetailsComponent;
  let fixture: ComponentFixture<AlertDetailsComponent>;
  let updateService: UpdateService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        SharedModule,
        HttpClientTestingModule,
        RouterTestingModule.withRoutes([])
      ],
      declarations: [ AlertDetailsComponent, AlertDetailsKeysPipe ],
      providers: [
        SearchService,
        AuthenticationService,
        AlertsService,
        UpdateService,
        { provide: GlobalConfigService, useValue: {
          get: () => { return of({})}
        }},
        {
          provide: DialogService,
          useValue: {
            launchDialog: () => {
              const confirmed = new Subject<ConfirmationType>();
              setTimeout(() => {
                confirmed.next(ConfirmationType.Confirmed);
              });
              return confirmed;
            }
          }
        },
        {
          provide: AppConfigService, useValue: {
          appConfigStatic: {},
          getApiRoot: () => {},
        } },
        {
          provide: DataSource,
          useClass: ElasticSearchLocalstorageImpl
        },
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AlertDetailsComponent);
    component = fixture.componentInstance;
    updateService = fixture.debugElement.injector.get(UpdateService);
    fixture.detectChanges();
  });

  it('should be in a loading state if no alertSources loaded', () => {
    component.alertSources = [];
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('[data-qe-id="preloader"]'))).toBeTruthy();
  });

  it('should show details if alertSources loaded', () => {
    component.alertSources = [alertDetail];
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('[data-qe-id="preloader"]'))).toBeFalsy();
  });

  it('should delete a comment.', fakeAsync(() => {
    const responseMock = new AlertSource();
    responseMock.guid = 'guid';
    const removeCommentSpy = spyOn(updateService, 'removeComment').and.returnValue(
            of(responseMock)
    );
    const setAlertSpy = spyOn(component, 'setAlert');

    expect(component).toBeTruthy();
    component.alertSource = new AlertSource();
    component.alertSource.guid = 'guid';
    component.alertSourceType = 'sourceType';
    const now = Date.now();

    component.alertSources = [alertDetail];
    fixture.detectChanges();

    component.alertCommentsWrapper = [
      new AlertCommentWrapper(
        new AlertComment('lorem ipsum', 'user', now),
        (new Date()).toString()
      )
    ];
    const element = fixture.debugElement.query(By.css('[data-qe-id="comments"]'));
    element.nativeElement.click();
    fixture.detectChanges();
    const deleteComment = fixture.debugElement.query(By.css('[data-qe-id="delete-comment"]'));
    deleteComment.nativeElement.click();
    tick(500);
    fixture.detectChanges();

    const expectedCommentRequest = new CommentAddRemoveRequest();
    expectedCommentRequest.guid = 'guid';
    expectedCommentRequest.comment = 'lorem ipsum';
    expectedCommentRequest.username = 'user';
    expectedCommentRequest.sensorType = 'sourceType';
    expectedCommentRequest.timestamp = now;

    const expectedAlertSource = new AlertSource();
    expectedAlertSource.guid = 'guid';

    expect(removeCommentSpy).toHaveBeenCalledWith(expectedCommentRequest);
    expect(removeCommentSpy).toHaveBeenCalledTimes(1);
    expect(setAlertSpy).toHaveBeenCalledWith(expectedAlertSource);
    expect(setAlertSpy).toHaveBeenCalledTimes(1);
  }));
});
