import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-modal-loading-indicator',
  templateUrl: './modal-loading-indicator.component.html',
  styleUrls: ['./modal-loading-indicator.component.scss']
})
export class ModalLoadingIndicatorComponent {

  @Input() show = false;

}
