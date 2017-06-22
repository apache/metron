import { OpaqueToken } from '@angular/core';
import {IAppConfig} from './app.config.interface';

export let APP_CONFIG = new OpaqueToken('app.config');

export const METRON_REST_CONFIG: IAppConfig = {
  apiEndpoint: '/api/v1'
};
