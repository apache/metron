import { createSelector } from '@ngrx/store';
import { ParserMetaInfoModel } from './models/parser-meta-info.model';
import { SensorParserConfigHistory } from '../model/sensor-parser-config-history';
import { ParserGroupModel } from './models/parser-group.model';
import { TopologyStatus } from '../model/topology-status';

const getGroups = (state) => {
  const historyInstances = state.sensors.groups.items.map((group: ParserGroupModel) => {
    const historyWrapper = new SensorParserConfigHistory();
    historyWrapper.sensorName = group.name;
    historyWrapper.setConfig(group);
    return historyWrapper;
  });
  return enrichWithStatusInfo(historyInstances, state.sensors.statuses.items, 'name');
}

const getParsers = (state) => {
  return enrichWithStatusInfo(state.sensors.parsers.items, state.sensors.statuses.items);
};

const getStatuses = (state) => {
  return state.sensors.statuses;
};

function enrichWithStatusInfo(items = [], statuses = [], nameField = 'sensorName') {
  return items.map((config) => {
    const belongingStatus: TopologyStatus = statuses.find((status) => {
      return config[nameField] === status.name;
    });

    if (belongingStatus) {
      config.status = belongingStatus.status;
      config.latency = belongingStatus.latency.toString();
      config.throughput = belongingStatus.throughput.toString();
      // FIXME where edit date and edited by information coming from?
    }

    return config;
  });
}

export const getMergedConfigs = createSelector(
  getGroups,
  getParsers,
  getStatuses,
  (groups, parsers, statuses): ParserMetaInfoModel[] => {
    let result: ParserMetaInfoModel[] = [];

    groups.forEach((group, i) => {
      const metaGroupItem = new ParserMetaInfoModel(group);
      metaGroupItem.setIsGroup(true);
      result = result.concat(metaGroupItem);

      const configsForGroup = parsers
        .filter(parser => parser.config && parser.config.group === group.sensorName)
        .map(parser => new ParserMetaInfoModel(parser));

      result = result.concat(configsForGroup);
    });

    result = result.concat(
      parsers
        .filter(parser => !parser.config || !parser.config.group)
        .map(parser => new ParserMetaInfoModel(parser))
      );

    result = result.map((item) => {
      let status: TopologyStatus = statuses.items.find(stat => {
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
