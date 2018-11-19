import { createSelector } from '@ngrx/store';
import { MetaParserConfigItem } from './sensor-aggregate/meta-parser-config-item';
import { SensorParserConfigHistory } from '../model/sensor-parser-config-history';
import { ParserGroupModel } from '../model/parser-group';
import { TopologyStatus } from '../model/topology-status';

const getGroups = (state) => {
  return state.groupConfigs.map((group: ParserGroupModel) => {
    const historyWrapper = new SensorParserConfigHistory();
    historyWrapper.sensorName = group.name;
    historyWrapper.setConfig(group);

    const belongingStatus: TopologyStatus = state.parserStatus.find((status) => {
      return group.name === status.name;
    });

    if (belongingStatus) {
      historyWrapper.status = belongingStatus.status;
      historyWrapper.latency = belongingStatus.latency.toString();
      historyWrapper.throughput = belongingStatus.throughput.toString();
      // FIXME where edit date and edited by information coming from?
    }

    return historyWrapper;
  });
};

const getParsers = (state) => {
  return state.parserConfigs.map((config) => {
    const belongingStatus: TopologyStatus = state.parserStatus.find((status) => {
      return config.sensorName === status.name;
    });

    if (belongingStatus) {
      config.status = belongingStatus.status;
      config.latency = belongingStatus.latency.toString();
      config.throughput = belongingStatus.throughput.toString();
      // FIXME where edit date and edited by information coming from?
    }

    return config;
  });


};

const getStatus = (state) => {
  return state.parserStatus;
};

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
