import { NgModule } from '@angular/core';

import {routing} from './meta-alerts.routing';
import { MetaAlertsComponent } from './meta-alerts.component';
import {UpdateService} from '../../service/update.service';
import {MetaAlertService} from '../../service/meta-alert.service';
import {SharedModule} from '../../shared/shared.module';

@NgModule({
  imports: [ routing,  SharedModule ],
  declarations: [ MetaAlertsComponent ],
  providers: [ UpdateService, MetaAlertService ],
})
export class MetaAlertsModule { }
