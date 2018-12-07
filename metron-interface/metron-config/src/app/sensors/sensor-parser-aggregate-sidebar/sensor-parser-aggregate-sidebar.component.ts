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
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { Store, select } from '@ngrx/store';
import { Observable, Subscription } from 'rxjs';
import * as fromReducers from '../reducers';
import * as fromActions from '../actions';
import { getGroups, GroupState } from '../reducers';
import { take } from 'rxjs/operators';

@Component({
  selector: 'metron-config-sensor-aggregate',
  templateUrl: './sensor-parser-aggregate-sidebar.component.html',
  styleUrls: ['./sensor-parser-aggregate-sidebar.component.scss']
})
export class SensorParserAggregateSidebarComponent implements OnInit, OnDestroy {

  private forceCreate = false;
  private draggedId: string;
  private dropTargetId: string;
  private state$: Observable<fromReducers.SensorState>;
  private stateSub: Subscription;
  private targetGroup: string;
  private groupDetails = {
    name: '',
    description: ''
  };
  groups: GroupState;
  name: string;
  description: string;
  existingGroup = null;

  allowMerge = true;

  constructor(
    private router: Router,
    private store: Store<fromReducers.State>,
    private route: ActivatedRoute,
  ) {
    this.state$ = store.pipe(select(fromReducers.getSensorsState));
  }

  ngOnInit() {
    this.stateSub = this.state$.subscribe((state: fromReducers.SensorState) => {
      this.draggedId = state.layout.dnd.draggedId;
      this.dropTargetId = state.layout.dnd.dropTargetId;
      this.targetGroup = state.layout.dnd.targetGroup;
      this.groups = state.groups;
      this.route.params.pipe(take(1)).subscribe(params => {
        if (params['id']) {
          this.existingGroup = this.groups.items.filter(g => g.config.getName() === params['id']);
          this.name = this.existingGroup[0].config.getName();
          this.description = this.existingGroup[0].config.getDescription();
        } else {
          this.name = 'Aggregate: ' + [this.draggedId, this.dropTargetId].join(' + ') ;
          this.description = '';
        }
      })
    });
  }

  close() {
    this.forceCreate = false;
    this.router.navigateByUrl('/sensors');
  }

  createNew(groupName: string, groupDescription: string) {
    this.groupDetails.name = groupName;
    this.groupDetails.description = groupDescription;
    const group = this.groups.items.filter(item => item.config.getName() === this.name);
    if (group.length) {
      this.store.dispatch(new fromActions.UpdateGroupDescription(this.groupDetails));
      this.close();
      return;
    } else {
      this.store.dispatch(new fromActions.CreateGroup(this.groupDetails));
    }

    if (!this.targetGroup) {
      this.store.dispatch(new fromActions.AggregateParsers({
        groupName,
        parserIds: [
          this.dropTargetId,
          this.draggedId,
        ]
      }));
    } else {
      const parserIds = [this.draggedId, this.dropTargetId];
      this.store.dispatch(new fromActions.AddToGroup({
        groupName,
        parserIds
      }));
      parserIds.forEach((parserId) => {
        this.store.dispatch(new fromActions.InjectAfter({
          reference: groupName,
          parserId,
        }));
      });
    }

    this.close();
  }

  mergeOrCreate() {
    if (this.allowMerge) {
      this.addToExisting();
    } else {
      this.forceCreate = true;
    }
  }

  addToExisting() {

    this.store.dispatch(new fromActions.AddToGroup({
      groupName: this.targetGroup,
      parserIds: [this.draggedId]
    }));

    this.store.dispatch(new fromActions.InjectAfter({
      reference: this.dropTargetId,
      parserId: this.draggedId,
    }));

    this.close();
  }

  showCreateForm(): boolean {
    return !this.targetGroup || this.forceCreate;
  }

  ngOnDestroy() {
    this.stateSub.unsubscribe();
  }
}
