import { TestBed } from '@angular/core/testing';

import { TimezoneConfigService } from './timezone-config.service';
import { SwitchComponent } from 'app/shared/switch/switch.component';

describe('TimezoneConfigService', () => {
  let service: TimezoneConfigService;

  beforeEach(() => {
    spyOn(localStorage, 'getItem').and.returnValues('true');

    TestBed.configureTestingModule({
      declarations: [ SwitchComponent ],
      providers: [ TimezoneConfigService ]
    });

    spyOn(TimezoneConfigService.prototype, 'toggleUTCtoLocal').and.callThrough();
    service = TestBed.get(TimezoneConfigService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get persisted state from localStorage', () => {
    expect(localStorage.getItem).toHaveBeenCalledWith(service.CONVERT_UTC_TO_LOCAL_KEY);
  });

  it('should set initial switch state', () => {
    expect(service.toggleUTCtoLocal).toHaveBeenCalledWith(true);
  });
});
