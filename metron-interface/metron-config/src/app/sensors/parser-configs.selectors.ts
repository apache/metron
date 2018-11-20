import { createSelector } from '@ngrx/store';
import { MetaParserConfigItem } from './sensor-aggregate/meta-parser-config-item';
import { SensorParserConfigHistory } from '../model/sensor-parser-config-history';
import { ParserGroupModel } from '../model/parser-group';
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
  (groups, parsers) => {
    let result = [];

    groups.forEach((group, i) => {
      const metaGroupItem = new MetaParserConfigItem(group);
      metaGroupItem.setIsGroup(true);
      result = result.concat(metaGroupItem);

      const configsForGroup = parsers
        .filter(parser => parser.config && parser.config.group === group.sensorName)
        .map(parser => new MetaParserConfigItem(parser));

      result = result.concat(configsForGroup);
    });

    result = result.concat(
      parsers
        .filter(parser => !parser.config || !parser.config.group)
        .map(parser => new MetaParserConfigItem(parser))
      );

    return result;
  }
);
