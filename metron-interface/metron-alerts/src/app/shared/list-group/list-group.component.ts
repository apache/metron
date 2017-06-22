import { Component, OnInit, Input, HostListener, ElementRef } from '@angular/core';

export interface ListGroupComponentData {
  name: string;
  icon?: string;
}

@Component({
  selector: 'metron-list-group',
  templateUrl: './list-group.component.html',
  styleUrls: ['./list-group.component.scss']
})
export class ListGroupComponent implements OnInit {

  @Input() data: ListGroupComponentData[] = [];

  constructor(private element: ElementRef) { }

  ngOnInit() {
  }

  @HostListener('click', ['$event']) onClick($event) {
    this.element.nativeElement.getElementsByClassName('active')[0].classList.remove('active');
    if ($event.target.nodeName === 'LI') {
      $event.target.classList.add('active');
    } else {
      $event.target.parentNode.classList.add('active');
    }
  }
}
