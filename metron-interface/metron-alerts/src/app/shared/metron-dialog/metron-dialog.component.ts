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
