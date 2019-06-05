import { TestBed, getTestBed, inject } from '@angular/core/testing';

import { AppConfigService } from './app-config.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

describe('AppConfigService', () => {

  let mockBackend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ HttpClientTestingModule ],
      providers: [ AppConfigService ],
    });

    mockBackend = getTestBed().get(HttpTestingController);
  });

  it('should be created', inject([AppConfigService], (service: AppConfigService) => {
    expect(service).toBeTruthy();
  }));

  it('should expose apiRoot', inject([AppConfigService], (service: AppConfigService) => {
    expect(typeof service.getApiRoot).toBe('function');
  }));

  it('should expose loginPath', inject([AppConfigService], (service: AppConfigService) => {
    expect(typeof service.getLoginPath).toBe('function');
  }));

  it('should expose contextMenuConfigURL', inject([AppConfigService], (service: AppConfigService) => {
    expect(typeof service.getContextMenuConfigURL).toBe('function');
  }));

  it('should load app-config.json', inject([AppConfigService], (service: AppConfigService) => {
    service.loadAppConfig();

    const req = mockBackend.expectOne('assets/app-config.json');
    expect(req.request.method).toEqual('GET');
    req.flush({});

    mockBackend.verify();
  }));

  it('getApiRoot() should return with apiRoot value', function(done) {
    inject([AppConfigService], (service: AppConfigService) => {
      service.loadAppConfig().then(() => {
        expect(service.getApiRoot()).toBe('/api/v1');
        done();
      }, (error) => {
        throw error;
      });

      const req = mockBackend.expectOne('assets/app-config.json');
      req.flush({ apiRoot: '/api/v1' });
    })();
  });

  it('getApiRoot() should log error on the console if apiRoot is undefined', function(done) {
    inject([AppConfigService], (service: AppConfigService) => {
      spyOn(console, 'error');

      service.loadAppConfig().then(() => {
        service.getApiRoot();
        expect(console.error).toHaveBeenCalledWith('[AppConfigService] apiRoot entry is missing from /assets/app-config.json');
        done();
      }, (error) => {
        throw error;
      });

      const req = mockBackend.expectOne('assets/app-config.json');
      req.flush({});
    })();
  });

  it('getLoginPath() should return with loginPath value', function(done) {
    inject([AppConfigService], (service: AppConfigService) => {
      service.loadAppConfig().then(() => {
        expect(service.getLoginPath()).toBe('/login');
        done();
      }, (error) => {
        throw error;
      });

      const req = mockBackend.expectOne('assets/app-config.json');
      req.flush({ loginPath: '/login' });
    })();
  });

  it('getLoginPath() should log error on the console if loginPath is undefined', function(done) {
    inject([AppConfigService], (service: AppConfigService) => {
      spyOn(console, 'error');

      service.loadAppConfig().then(() => {
        service.getLoginPath();
        expect(console.error).toHaveBeenCalledWith('[AppConfigService] loginPath entry is missing from /assets/app-config.json');
        done();
      }, (error) => {
        throw error;
      });

      const req = mockBackend.expectOne('assets/app-config.json');
      req.flush({});
    })();
  });

  it('getContextMenuConfigURL() should return with contextMenuConfigURL value', function(done) {
    inject([AppConfigService], (service: AppConfigService) => {
      service.loadAppConfig().then(() => {
        expect(service.getContextMenuConfigURL()).toBe('/contextMenuConfigURL');
        done();
      }, (error) => {
        throw error;
      });

      const req = mockBackend.expectOne('assets/app-config.json');
      req.flush({ contextMenuConfigURL: '/contextMenuConfigURL' });
    })();
  });

  it('getContextMenuConfigURL() should log error on the console if contextMenuConfigURL is undefined', function(done) {
    inject([AppConfigService], (service: AppConfigService) => {
      spyOn(console, 'error');

      service.loadAppConfig().then(() => {
        service.getContextMenuConfigURL();
        expect(console.error).toHaveBeenCalledWith('[AppConfigService] contextMenuConfigURL entry is missing from /assets/app-config.json');
        done();
      }, (error) => {
        throw error;
      });

      const req = mockBackend.expectOne('assets/app-config.json');
      req.flush({});
    })();
  });
});
