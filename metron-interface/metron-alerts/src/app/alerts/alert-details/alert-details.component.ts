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
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import * as moment from 'moment/moment';
import {Subscription} from 'rxjs';

import {SearchService} from '../../service/search.service';
import {UpdateService} from '../../service/update.service';
import {Alert} from '../../model/alert';
import {AlertsService} from '../../service/alerts.service';
import {AlertSource} from '../../model/alert-source';
import {PatchRequest} from '../../model/patch-request';
import {Patch} from '../../model/patch';
import {AlertComment} from './alert-comment';
import {AuthenticationService} from '../../service/authentication.service';
import {CommentAddRemoveRequest} from "../../model/comment-add-remove-request";
import {META_ALERTS_SENSOR_TYPE} from '../../utils/constants';
import {GlobalConfigService} from '../../service/global-config.service';
import { DialogService } from 'app/service/dialog.service';
import { ConfirmationType } from 'app/model/confirmation-type';

export enum AlertState {
  NEW, OPEN, ESCALATE, DISMISS, RESOLVE
}
export enum Tabs {
  DETAILS, COMMENTS
}

export class AlertCommentWrapper {
  alertComment: AlertComment;
  displayTime: string;

  constructor(alertComment: AlertComment, displayTime: string) {
    this.alertComment = alertComment;
    this.displayTime = displayTime;
  }
}

@Component({
  selector: 'app-alert-details',
  templateUrl: './alert-details.component.html',
  styleUrls: ['./alert-details.component.scss']
})
export class AlertDetailsComponent implements OnInit {

  alertId = '';
  alertName = '';
  alertSourceType = '';
  showEditor = false;
  isMetaAlert = false;
  alertIndex = '';
  alertState = AlertState;
  tabs = Tabs;
  activeTab = Tabs.DETAILS;
  selectedAlertState: AlertState = AlertState.NEW;
  alertSource: AlertSource = new AlertSource();
  alertSources = [];
  alertFields: string[] = [];
  alertCommentStr = '';
  alertCommentsWrapper: AlertCommentWrapper[] = [];
  globalConfig: {} = {};
  globalConfigService: GlobalConfigService;
  configSubscription: Subscription;

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private searchService: SearchService,
              private updateService: UpdateService,
              private alertsService: AlertsService,
              private authenticationService: AuthenticationService,
              private dialogService: DialogService,
              globalConfigService: GlobalConfigService) {
    this.globalConfigService = globalConfigService;
  }

  goBack() {
    this.router.navigateByUrl('/alerts-list');
    return false;
  }

  getData() {
    this.alertCommentStr = '';
    this.searchService.getAlert(this.alertSourceType, this.alertId).subscribe(alertSource => {
      this.setAlert(alertSource);
      this.setComments(alertSource['comments'] || []);
    });
  }

  setAlert(alertSource) {
    this.alertSource = alertSource;
    this.alertSources = (alertSource.metron_alert && alertSource.metron_alert.length > 0) ? alertSource.metron_alert : [alertSource];
    this.selectedAlertState = this.getAlertState(alertSource['alert_status']);
  }

  setComments(alertComments) {
    this.alertCommentsWrapper = alertComments.map(alertComment =>
        new AlertCommentWrapper(alertComment, moment(new Date(alertComment.timestamp)).fromNow()));
  }

  getAlertState(alertStatus) {
    if (alertStatus === 'OPEN') {
      return AlertState.OPEN;
    } else if (alertStatus === 'ESCALATE') {
      return AlertState.ESCALATE;
    } else if (alertStatus === 'DISMISS') {
      return AlertState.DISMISS;
    } else if (alertStatus === 'RESOLVE') {
      return AlertState.RESOLVE;
    } else {
      return AlertState.NEW;
    }
  }

  ngOnInit() {
    this.configSubscription = this.globalConfigService.get().subscribe((config: {}) => {
      this.globalConfig = config;
    });

    this.activatedRoute.params.subscribe(params => {
      this.alertId = params['guid'];
      this.alertSourceType = params['source.type.field'];
      this.alertIndex = params['index'];
      this.isMetaAlert = this.alertSourceType === META_ALERTS_SENSOR_TYPE;
      this.getData();
    });
  };

  ngOnDestroy() {
    this.configSubscription.unsubscribe();
  }

  getScore(alertSource) {
    return alertSource[this.globalConfig['threat.triage.score.field']];
  }

  processOpen() {
    this.updateAlertState('OPEN');
  }

  processNew() {
    this.updateAlertState('NEW');
  }

  processEscalate() {
    this.updateAlertState('ESCALATE');

    let tAlert = new Alert();
    tAlert.source = this.alertSource;
    this.alertsService.escalate([tAlert]).subscribe();
  }

  processDismiss() {
    this.updateAlertState('DISMISS');
  }

  processResolve() {
    this.updateAlertState('RESOLVE');
  }

  updateAlertState(state: string) {
    let tAlert = new Alert();
    tAlert.source = this.alertSource;

    let previousAlertStatus = this.alertSource['alert_status'];
    this.alertSource['alert_status'] = state;
    this.setAlert(this.alertSource);
    this.updateService.updateAlertState([tAlert], state).subscribe(() => {}, () => {
      this.alertSource['alert_status'] = previousAlertStatus;
      this.setAlert(this.alertSource);
    });
  }

  toggleNameEditor() {
    if (this.alertSources.length > 1) {
      this.alertName = '';
      this.showEditor = !this.showEditor;
    }
  }

  saveName() {
    if (this.alertName.length > 0) {
      let patchRequest = new PatchRequest();
      patchRequest.guid = this.alertId;
      patchRequest.sensorType = 'metaalert';
      patchRequest.patch = [new Patch('add', '/name', this.alertName)];

      let previousName = this.alertSource['name'];
      this.alertSource['name'] = this.alertName;
      this.updateService.patch(patchRequest).subscribe(rep => {
        this.toggleNameEditor();
      }, () => {
        this.alertSource['name'] = previousName;
        this.alertName = previousName;
        this.toggleNameEditor();
      });
    }
  }

  onAddComment() {
    let newComment = new AlertComment(this.alertCommentStr, this.authenticationService.getCurrentUserName(), new Date().getTime());
    let alertComments = this.alertCommentsWrapper.map(alertsWrapper => alertsWrapper.alertComment);
    alertComments.unshift(newComment);
    this.setComments(alertComments);
    let commentRequest = new CommentAddRemoveRequest();
    commentRequest.guid = this.alertSource.guid;
    commentRequest.comment = this.alertCommentStr;
    commentRequest.username = this.authenticationService.getCurrentUserName();
    commentRequest.timestamp = new Date().getTime();
    commentRequest.sensorType = this.alertSourceType;
    this.updateService.addComment(commentRequest).subscribe(
        () => {},
        () => {
          let previousComments = this.alertCommentsWrapper.map(alertsWrapper => alertsWrapper.alertComment)
          .filter(alertComment => alertComment !== newComment);
          this.setComments(previousComments);
        });
  }

  patchAlert(patch: Patch, onPatchError) {
    let patchRequest = new PatchRequest();
    patchRequest.guid = this.alertSource.guid;
    patchRequest.index = this.alertIndex;
    patchRequest.patch = [patch];
    patchRequest.sensorType = this.alertSourceType;

    this.updateService.patch(patchRequest).subscribe(() => {}, onPatchError);
  }

  onDeleteComment(index: number) {
    let commentText =  'Do you wish to delete the comment ';
    if (this.alertCommentsWrapper[index].alertComment.comment.length > 25 ) {
      commentText += ' \'' + this.alertCommentsWrapper[index].alertComment.comment.substr(0, 25) + '...\'';
    } else {
      commentText += ' \'' + this.alertCommentsWrapper[index].alertComment.comment + '\'';
    }

    const confirmedSubscription = this.dialogService.launchDialog(commentText).subscribe(action => {
      if (action === ConfirmationType.Confirmed) {
        let deletedCommentWrapper = this.alertCommentsWrapper.splice(index, 1)[0];
        let commentRequest = new CommentAddRemoveRequest();
        commentRequest.guid = this.alertSource.guid;
        commentRequest.comment = deletedCommentWrapper.alertComment.comment;
        commentRequest.username = deletedCommentWrapper.alertComment.username;
        commentRequest.timestamp = deletedCommentWrapper.alertComment.timestamp;
        commentRequest.sensorType = this.alertSourceType;
        this.updateService.removeComment(commentRequest).subscribe(
            null,
            () => {
              // add the deleted comment back
              this.alertCommentsWrapper.unshift(deletedCommentWrapper);
              this.alertCommentsWrapper.sort((a, b) => b.alertComment.timestamp - a.alertComment.timestamp);
            });
      }
      confirmedSubscription.unsubscribe();
    });
  }
}
