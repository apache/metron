package org.apache.metron.stellar.common.utils.validation.validators;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.utils.validation.StellarConfiguredStatementContainer;
import org.apache.metron.stellar.common.utils.validation.StellarValidator;
import org.apache.metron.stellar.common.utils.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseValidator implements StellarValidator {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String FAILED_COMPILE = "Failed to compile";

  public BaseValidator() {
  }

  @Override
  public abstract Iterable<ValidationResult> validate();

  protected List<ValidationResult> handleContainers(
      List<StellarConfiguredStatementContainer> containers) {
    ArrayList<ValidationResult> results = new ArrayList<>();
    containers.forEach((container) -> {
      try {
        container.discover();
        container.visit((path, statement) -> {
          try {
            if (StellarProcessor.compile(statement) == null) {
              results.add(new ValidationResult(path, statement, FAILED_COMPILE, false));
            } else {
              results.add(new ValidationResult(path, statement, null, true));
            }
          } catch (RuntimeException e) {
            results.add(new ValidationResult(path, statement, e.getMessage(), false));
          }
        }, (path, error) -> {
          results.add(new ValidationResult(path, null, error.getMessage(), false));
        });
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    });
    return results;
  }
}
