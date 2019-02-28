/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { SimpleChange, SimpleChanges } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { async, TestBed, ComponentFixture } from '@angular/core/testing';
import { SensorThreatTriageComponent } from './sensor-threat-triage.component';
import { SensorEnrichmentConfigService } from '../../service/sensor-enrichment-config.service';
import { Observable } from 'rxjs';
import { SensorThreatTriageModule } from './sensor-threat-triage.module';

class MockSensorEnrichmentConfigService {
  public getAvailableThreatTriageAggregators(): Observable<string[]> {
    return Observable.create(observer => {
      observer.next(['MAX', 'MIN', 'SUM', 'MEAN', 'POSITIVE_MEAN']);
      observer.complete();
    });
  }
}

describe('Component: SensorThreatTriageComponent', () => {
  let component: SensorThreatTriageComponent;
  let fixture: ComponentFixture<SensorThreatTriageComponent>;
  let sensorEnrichmentConfigService: SensorEnrichmentConfigService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SensorThreatTriageModule],
      providers: [
        { provide: HttpClient },
        {
          provide: SensorEnrichmentConfigService,
          useClass: MockSensorEnrichmentConfigService
        }
      ]
    })
      .compileComponents()
      .then(() => {
        fixture = TestBed.createComponent(SensorThreatTriageComponent);
        component = fixture.componentInstance;
        sensorEnrichmentConfigService = fixture.debugElement.injector.get(
          SensorEnrichmentConfigService
        );
      });
  }));

  it('should create an instance', () => {
    expect(component).toBeDefined();
    fixture.destroy();
  });

  it('should create an instance', async(() => {
    spyOn(component, 'init');
    let changes: SimpleChanges = {
      showThreatTriage: new SimpleChange(false, true, true)
    };

    component.ngOnChanges(changes);
    expect(component.init).toHaveBeenCalled();

    changes = { showStellar: new SimpleChange(true, false, false) };
    component.ngOnChanges(changes);
    expect(component.init['calls'].count()).toEqual(1);

    fixture.destroy();
  }));

  it('should close panel', async(() => {
    let numClosed = 0;
    component.hideThreatTriage.subscribe((closed: boolean) => {
      numClosed++;
    });

    component.onClose();
    expect(numClosed).toEqual(1);

    fixture.destroy();
  }));
});
