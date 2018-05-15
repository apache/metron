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
import { Component, OnInit } from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import * as moment from 'moment/moment';

import {SearchService} from '../../service/search.service';
import {UpdateService} from '../../service/update.service';
import {Alert} from '../../model/alert';
import {AlertsService} from '../../service/alerts.service';
import {AlertSource} from '../../model/alert-source';
import {PatchRequest} from '../../model/patch-request';
import {Patch} from '../../model/patch';
import {AlertComment} from './alert-comment';
import {AuthenticationService} from '../../service/authentication.service';
import {MetronDialogBox} from '../../shared/metron-dialog-box';
import {META_ALERTS_INDEX, META_ALERTS_SENSOR_TYPE} from '../../utils/constants';

export enum AlertState {
  NEW, OPEN, ESCALATE, DISMISS, RESOLVE
}

export enum Tabs {
  DETAILS, COMMENTS
}

class AlertCommentWrapper {
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

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private searchService: SearchService,
              private updateService: UpdateService,
              private alertsService: AlertsService,
              private authenticationService: AuthenticationService,
              private metronDialogBox: MetronDialogBox) {

  }

  goBack() {
    this.router.navigateByUrl('/alerts-list');
    return false;
  }

  getData(fireToggleEditor = false) {
    this.alertCommentStr = '';
    this.searchService.getAlert(this.alertSourceType, this.alertId).subscribe(alertSource => {
      this.alertSource = alertSource;
      this.selectedAlertState = this.getAlertState(alertSource['alert_status']);
      this.alertSources = (alertSource.alert && alertSource.alert.length > 0) ? alertSource.alert : [alertSource];
      this.setComments(alertSource);

      if (fireToggleEditor) {
        this.toggleNameEditor();
      }
    });
  }

  setComments(alert) {
    let alertComments = alert['comments'] ? alert['comments'] : [];
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
    this.activatedRoute.params.subscribe(params => {
      this.alertId = params['guid'];
      this.alertSourceType = params['source.type.field'];
      this.alertIndex = params['index'];
      this.isMetaAlert = (this.alertIndex === META_ALERTS_INDEX && this.alertSourceType !== META_ALERTS_SENSOR_TYPE) ? true : false;
      this.getData();
    });
  };

  processOpen() {
    let tAlert = new Alert();
    tAlert.source = this.alertSource;

    this.selectedAlertState = AlertState.OPEN;
    this.updateService.updateAlertState([tAlert], 'OPEN').subscribe(results => {
      this.getData();
    });
  }

  processNew() {
    let tAlert = new Alert();
    tAlert.source = this.alertSource;

    this.selectedAlertState = AlertState.NEW;
    this.updateService.updateAlertState([tAlert], 'NEW').subscribe(results => {
      this.getData();
    });
  }

  processEscalate() {
    let tAlert = new Alert();
    tAlert.source = this.alertSource;

    this.selectedAlertState = AlertState.ESCALATE;
    this.updateService.updateAlertState([tAlert], 'ESCALATE').subscribe(results => {
      this.getData();
    });
    this.alertsService.escalate([tAlert]).subscribe();
  }

  processDismiss() {
    let tAlert = new Alert();
    tAlert.source = this.alertSource;

    this.selectedAlertState = AlertState.DISMISS;
    this.updateService.updateAlertState([tAlert], 'DISMISS').subscribe(results => {
      this.getData();
    });
  }

  processResolve() {
    let tAlert = new Alert();
    tAlert.source = this.alertSource;

    this.selectedAlertState = AlertState.RESOLVE;
    this.updateService.updateAlertState([tAlert], 'RESOLVE').subscribe(results => {
      this.getData();
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
      patchRequest.index = META_ALERTS_INDEX;
      patchRequest.patch = [new Patch('add', '/name', this.alertName)];

      this.updateService.patch(patchRequest).subscribe(rep => {
        this.getData(true);
      });
    }
  }

  onAddComment() {
    let alertComment = new AlertComment(this.alertCommentStr, this.authenticationService.getCurrentUserName(), new Date().getTime());
    let tAlertComments = this.alertCommentsWrapper.map(alertsWrapper => alertsWrapper.alertComment);
    tAlertComments.unshift(alertComment);
    this.patchAlert(new Patch('add', '/comments', tAlertComments));
  }

  patchAlert(patch: Patch) {
    let patchRequest = new PatchRequest();
    patchRequest.guid = this.alertSource.guid;
    patchRequest.index = this.alertIndex;
    patchRequest.patch = [patch];
    patchRequest.sensorType = this.alertSourceType;

    this.updateService.patch(patchRequest).subscribe(() => {
      this.getData();
    });
  }

  onDeleteComment(index: number) {
    let commentText =  'Do you wish to delete the comment ';
    if (this.alertCommentsWrapper[index].alertComment.comment.length > 25 ) {
      commentText += ' \'' + this.alertCommentsWrapper[index].alertComment.comment.substr(0, 25) + '...\'';
    } else {
      commentText += ' \'' + this.alertCommentsWrapper[index].alertComment.comment + '\'';
    }

    this.metronDialogBox.showConfirmationMessage(commentText).subscribe(response => {
      if (response) {
        this.alertCommentsWrapper.splice(index, 1);
        this.patchAlert(new Patch('add', '/comments', this.alertCommentsWrapper.map(alertsWrapper => alertsWrapper.alertComment)));
      }
    });
  }
}


