import { Injectable } from '@angular/core';
import { SensorParserConfigService } from 'app/service/sensor-parser-config.service';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { MetaParserConfigItem } from './meta-parser-config-item';
import { ParserGroupModel } from 'app/model/parser-group';

@Injectable ()
export class MetaParserConfigItemFactory {

  constructor(private parserConfigService: SensorParserConfigService) {}

  create(item: SensorParserConfigHistory | ParserGroupModel) {
    return new MetaParserConfigItem(item, this.parserConfigService);
  }
}
