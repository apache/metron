import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import 'bootstrap';

export interface CollapseComponentData {
  getName(): string;
  getBuckets(): any[];
}

@Component({
  selector: 'metron-collapse',
  templateUrl: './collapse.component.html',
  styleUrls: ['./collapse.component.scss']
})
export class CollapseComponent implements OnInit {

  static counter = 0;
  uniqueId = '';

  @Input() data: any;
  @Input() fontSize = 14;
  @Input() titleSeperator = false;
  @Input() deleteOption = false;
  @Input() show = false;
  @Input() strLength = 30;

  @Output() onSelect = new EventEmitter<{name: string, key: string}>();
  @Output() onDelete = new EventEmitter<{name: string, key: string}>();

  constructor() {
    this.uniqueId = 'CollapseComponent' + '_' + ++CollapseComponent.counter;
  }

  ngOnInit() {
  }

  onDeleteClick($event, key: string) {
    this.onDelete.emit({name: this.data.getName(), key: key});
    $event.stopPropagation();
    return false;
  }

  onSelectClick(key: string) {
    this.onSelect.emit({name: this.data.getName(), key: key});
  }
}
