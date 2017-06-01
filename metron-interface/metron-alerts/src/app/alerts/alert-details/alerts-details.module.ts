import { NgModule } from '@angular/core';
import {routing} from './alerts-details.routing';
import {SharedModule} from '../../shared/shared.module';
import {AlertDetailsComponent} from './alert-details.component';
import {WorkflowService} from '../../service/workflow.service';

@NgModule ({
    imports: [ routing,  SharedModule],
    declarations: [ AlertDetailsComponent ],
    providers: [ WorkflowService ],
})
export class AlertDetailsModule { }
