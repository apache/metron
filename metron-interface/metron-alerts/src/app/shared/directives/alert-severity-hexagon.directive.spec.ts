import { ElementRef } from '@angular/core';
import { AlertSeverityHexagonDirective } from './alert-severity-hexagon.directive';

describe('AlertSeverityHexagonDirective', () => {
  it('should be created', () => {
    const directive = new AlertSeverityHexagonDirective(new ElementRef({}));
    expect(directive).toBeTruthy();
  });
});
