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
    this.data.map(collapsableData => collapsableData.groupItems = []);

    for (let key of Object.keys(this.facets)) {
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
