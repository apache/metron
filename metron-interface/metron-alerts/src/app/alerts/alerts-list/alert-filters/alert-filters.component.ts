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

import {Component, OnChanges, Input, SimpleChanges, EventEmitter, Output} from '@angular/core';
import {Facets} from '../../../model/facets';
import {
    CollapseComponentData,
    CollapseComponentDataItems
} from '../../../shared/collapse/collapse-component-data';

@Component({
  selector: 'app-alert-filters',
  templateUrl: './alert-filters.component.html',
  styleUrls: ['./alert-filters.component.scss']
})
export class AlertFiltersComponent implements OnChanges {

  facetMap = new Map<string, CollapseComponentData>();
  data: CollapseComponentData[] = [];
  @Input() facets: Facets = new Facets();
  @Output() facetFilterChange = new EventEmitter<any>();

  ngOnChanges(changes: SimpleChanges) {
    if (changes && changes['facets'] && this.facets) {
      this.prepareData();
    }
  }

  prepareData() {
    let facetFields = Object.keys(this.facets);
    this.data = this.data.filter(collapsableData => facetFields.includes(collapsableData.groupName));
    this.data.map(collapsableData => collapsableData.groupItems = []);

    for (let key of facetFields) {
      let facet = this.facets[key];
      let facetItems: CollapseComponentDataItems[] = [];

      for (let facetVal of Object.keys(facet)) {
        facetItems.push(new CollapseComponentDataItems(facetVal, facet[facetVal]));
      }

      let collapseComponentData = this.data.find(collapsableData => collapsableData.groupName === key);
      if (!collapseComponentData) {
        collapseComponentData = new CollapseComponentData();
        collapseComponentData.groupName = key;
        collapseComponentData.collapsed = true;
        this.data.push(collapseComponentData);
        this.data = this.data.sort((obj1, obj2) => obj1.groupName.localeCompare(obj2.groupName));
      }

      collapseComponentData.groupItems = facetItems;
    }
  }

  onFacetFilterSelect($event) {
    this.facetFilterChange.emit($event);
  }
}
