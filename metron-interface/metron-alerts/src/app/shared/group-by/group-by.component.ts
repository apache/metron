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

import { Component, OnInit, Input, EventEmitter, Output, OnChanges, SimpleChanges } from '@angular/core';
import { DragulaService } from 'ng2-dragula/ng2-dragula';
import {Facets} from '../../model/facets';
import {GroupByComponentData} from './group-by-component-data';


@Component({
  selector: 'app-group-by',
  templateUrl: './group-by.component.html',
  styleUrls: ['./group-by.component.scss']
})
export class GroupByComponent implements OnInit, OnChanges {
  maxGroupCount = 99999999;
  backgroundColor = '#0F4450';
  border = '1px solid #1B596C';

  groupSelected = false;
  data: GroupByComponentData[] = [];
  @Input() facets: Facets = new Facets();

  @Output() groupsChange = new EventEmitter<string[]>();

  constructor(private dragulaService: DragulaService) {}

  fireGroupsChange() {
    let selectedGroupNames = [];
    this.data.reduce((selectedGroups, groupBy) => {
      if (groupBy.selected) {
        selectedGroups.push(groupBy.name);
      }
      return selectedGroups;
    }, selectedGroupNames);
    this.groupsChange.emit(selectedGroupNames);
    this.groupSelected = (selectedGroupNames.length !== 0);
  }

  ngOnInit() {
    this.setTransitStyle();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes && changes['facets'] && this.facets) {
      this.prepareData();
    }
  }

  prepareData() {
    let facetFields = Object.keys(this.facets);
    this.data = this.data.filter(groupByData => facetFields.includes(groupByData.name));
    for (let key of facetFields) {
      let facet = this.facets[key];
      let count = Object.keys(facet).length;
      let groupByItem = this.data.filter(groupByData => groupByData.name === key)[0];
      if (!groupByItem) {
        groupByItem = new GroupByComponentData(key, count);
        this.data.push(groupByItem);
      } else {
        groupByItem.count = count;
      }
    }
  }

  selectGroup(group: GroupByComponentData) {
    group.selected = !group.selected;
    this.fireGroupsChange();
  }

  private setTransitStyle() {
    this.dragulaService.drag.subscribe(value => {
      value[1].style.background = this.backgroundColor;
      value[1].style.border = this.border;
      value[1].style.textAlign = 'Center';

      value[1].querySelector('.count').style.fontSize = '20px';
      value[1].querySelector('.name').style.fontSize = '12px';

    });

    this.dragulaService.dragend.subscribe(value => {
      value[1].style.background = '';
      value[1].style.border = '';
      value[1].style.textAlign = '';
    });

    this.dragulaService.dropModel.subscribe(value => {
      this.fireGroupsChange();
    });
  }

  unGroup() {
    this.data.map(group => group.selected = false);
    this.fireGroupsChange();
  }
}
