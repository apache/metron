import { Injectable } from '@angular/core';
import { SensorParserConfigService } from 'app/service/sensor-parser-config.service';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { MetaParserConfigItem } from './meta-parser-config-item';

@Injectable ()
export class MetaParserConfigItemFactory {

  constructor(private parserConfigService: SensorParserConfigService) {}

  create(sensor: SensorParserConfigHistory) {
    return new MetaParserConfigItem(sensor, this.parserConfigService);
  }
}
