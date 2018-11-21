import { createSelector } from '@ngrx/store';
import { ParserMetaInfoModel } from './models/parser-meta-info.model';
import { SensorParserConfigHistory } from '../model/sensor-parser-config-history';
import { ParserGroupModel } from './models/parser-group.model';
import { TopologyStatus } from '../model/topology-status';

const getGroups = (state) => {
  return state.sensors.groups.items;
}

const getParsers = (state) => {
  return state.sensors.parsers.items;
};

const getStatuses = (state) => {
  return state.sensors.statuses.items;
};

export const getMergedConfigs = createSelector(
  getGroups,
  getParsers,
  getStatuses,
  (groups: ParserMetaInfoModel[], parsers: ParserMetaInfoModel[], statuses: TopologyStatus[]): ParserMetaInfoModel[] => {
    let result: ParserMetaInfoModel[] = [];

    groups.forEach((group, i) => {
      result = result.concat(group);

      const configsForGroup = parsers
        .filter(parser => parser.getConfig() && parser.getConfig().group === group.getName())

      result = result.concat(configsForGroup);
    });

    result = result.concat(
      parsers
        .filter(parser => !parser.getConfig() || !parser.getConfig().group)
      );

    result = result.map((item) => {
      let status: TopologyStatus = statuses.find(stat => {
        return stat.name === item.getName();
      });
      if (status) {
        item.setStatus(status);
      }
      return item;
    });

    return result;
  }
);
