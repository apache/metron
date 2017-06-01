import { NgModule } from '@angular/core';
import {routing} from './configure-table.routing';
import {SharedModule} from '../../shared/shared.module';
import {ConfigureTableComponent} from './configure-table.component';
import {ClusterMetaDataService} from '../../service/cluster-metadata.service';
import {ColumnNamesService} from '../../service/column-names.service';

@NgModule ({
    imports: [ routing,  SharedModule],
    declarations: [ ConfigureTableComponent ],
    providers: [ ClusterMetaDataService, ColumnNamesService ]
})
export class ConfigureTableModule { }
