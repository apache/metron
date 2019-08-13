import { TestBed } from '@angular/core/testing';

import { TimezoneConfigService } from './timezone-config.service';

describe('TimezoneConfigService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: TimezoneConfigService = TestBed.get(TimezoneConfigService);
    expect(service).toBeTruthy();
  });
});
