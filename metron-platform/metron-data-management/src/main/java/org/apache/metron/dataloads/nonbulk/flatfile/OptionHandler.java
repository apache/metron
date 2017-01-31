package org.apache.metron.dataloads.nonbulk.flatfile;

import com.google.common.base.Function;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.Optional;

public abstract class OptionHandler implements Function<String, Option>
{
  public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
    return Optional.empty();
  }
}
