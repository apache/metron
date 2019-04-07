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

describe('AlertDetailsComponent', () => {
  let component: AlertDetailsComponent;
  let fixture: ComponentFixture<AlertDetailsComponent>;

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
        GlobalConfigService,
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
    fixture.detectChanges();
  });

  it('should delete a comment.', fakeAsync(() => {
    expect(component).toBeTruthy();
    component.alertCommentsWrapper = [
      new AlertCommentWrapper(
        new AlertComment('lorem ipsum', 'user', Date.now()),
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
    expect(component.alertCommentsWrapper.length).toEqual(0);
    const comments = fixture.debugElement.queryAll(By.css('[data-qe-id="comment"]'));
    expect(comments.length).toEqual(0);
  }));
});
