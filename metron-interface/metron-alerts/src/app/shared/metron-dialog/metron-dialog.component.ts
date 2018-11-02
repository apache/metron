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
import {
  Component,
  OnInit,
  OnDestroy
} from '@angular/core';
import { DialogService } from '../../service/dialog.service';
import { Subscription } from 'rxjs';

export enum DialogType {
  Confirmation,
  Error
}

@Component({
  selector: 'app-metron-dialog',
  templateUrl: './metron-dialog.component.html',
  styleUrls: ['./metron-dialog.component.scss']
})
export class MetronDialogComponent implements OnInit, OnDestroy {
  public message: string;
  public showModal = false;
  public dialogType: string;
  private subscription: Subscription;

  constructor(private dialogService: DialogService) {}

  ngOnInit(): void {
    this.subscription = this.dialogService.message.subscribe(r => {
      this.showModal = r.show;
      this.message = r.message;
      this.dialogType = DialogType[r.dialogType];
    });

  }

  confirm(): void {
    this.dialogService.approve();
    this.showModal = false;
  }

  cancel(): void {
    this.dialogService.cancel();
    this.showModal = false;
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
