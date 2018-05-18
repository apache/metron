import { NgModule } from '@angular/core';

import { MetaAlertsRoutingModule } from './meta-alerts.routing';
import { MetaAlertsComponent } from './meta-alerts.component';
import { MetaAlertService } from '../../service/meta-alert.service';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  imports: [
    MetaAlertsRoutingModule,
    SharedModule
  ],
  declarations: [
    MetaAlertsComponent
  ],
  providers: [
    MetaAlertService
  ]
})
export class MetaAlertsModule { }
