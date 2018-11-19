import { SensorParserConfigHistory } from './model/sensor-parser-config-history';
import { ParserGroupModel } from './model/parser-group';
import { TopologyStatus } from './model/topology-status';

export interface AppState {
  parsers: ParserState;
}

export interface ParserState {
  parserConfigs?: SensorParserConfigHistory[],
  groupConfigs?: ParserGroupModel[],
  parserStatus?: TopologyStatus[],
}
