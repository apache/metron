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

const getLayoutOrder = (state) => {
  return state.sensors.layout.order;
};

export const getMergedConfigs = createSelector(
  getGroups,
  getParsers,
  getStatuses,
  getLayoutOrder,
  (
    groups: ParserMetaInfoModel[],
    parsers: ParserMetaInfoModel[],
    statuses: TopologyStatus[],
    order: string[]
  ): ParserMetaInfoModel[] => {
    let result: ParserMetaInfoModel[] = [];
    result = order.map((id: string) => {
      const group = groups.find(g => g.getName() === id);
      if (group) {
        return group;
      }
      const parserConfig = parsers.find(p => p.getName() === id);
      if (parserConfig) {
        return parserConfig;
      }
      return null;
    }).filter(Boolean);

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
