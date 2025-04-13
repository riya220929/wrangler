/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.directives.aggregates;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.Collections;
import java.util.List;

/**
 * A directive for aggregating byte sizes and time durations.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Description("Aggregates byte sizes and time durations from specified columns.")
public class AggregateStats implements Directive {
  public static final String NAME = "aggregate-stats";
  private String sizeColumn;
  private String timeColumn;
  private String totalSizeColumn;
  private String totalTimeColumn;
  private String sizeUnit;
  private String timeUnit;
  private boolean isAverage;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("size_column", TokenType.COLUMN_NAME);
    builder.define("time_column", TokenType.COLUMN_NAME);
    builder.define("total_size_column", TokenType.COLUMN_NAME);
    builder.define("total_time_column", TokenType.COLUMN_NAME);
    builder.define("size_unit", TokenType.TEXT, "MB");
    builder.define("time_unit", TokenType.TEXT, "s");
    builder.define("average", TokenType.BOOLEAN, "false");
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.sizeColumn = args.value("size_column");
    this.timeColumn = args.value("time_column");
    this.totalSizeColumn = args.value("total_size_column");
    this.totalTimeColumn = args.value("total_time_column");
    this.sizeUnit = args.value("size_unit").toString().toUpperCase();
    this.timeUnit = args.value("time_unit").toString().toLowerCase();
    this.isAverage = Boolean.parseBoolean(args.value("average").toString());
  }

  @Override
  public void destroy() {
    // no-op
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    if (rows.isEmpty()) {
      return rows;
    }

    long totalBytes = 0;
    long totalNanos = 0;
    int count = 0;

    // Accumulate totals
    for (Row row : rows) {
      Object sizeObj = row.getValue(sizeColumn);
      Object timeObj = row.getValue(timeColumn);

      if (sizeObj instanceof ByteSize) {
        totalBytes += ((ByteSize) sizeObj).getBytes();
      }

      if (timeObj instanceof TimeDuration) {
        totalNanos += ((TimeDuration) timeObj).getNanoseconds();
      }

      count++;
    }

    // Convert totals to requested units
    double finalSize;
    double finalTime;

    if (isAverage && count > 0) {
      totalBytes /= count;
      totalNanos /= count;
    }

    // Convert bytes to requested unit
    switch (sizeUnit) {
      case "B":
        finalSize = totalBytes;
        break;
      case "KB":
        finalSize = totalBytes / 1024.0;
        break;
      case "MB":
        finalSize = totalBytes / (1024.0 * 1024.0);
        break;
      case "GB":
        finalSize = totalBytes / (1024.0 * 1024.0 * 1024.0);
        break;
      case "TB":
        finalSize = totalBytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
        break;
      case "PB":
        finalSize = totalBytes / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0);
        break;
      default:
        throw new DirectiveExecutionException(
          String.format("Invalid size unit '%s'. Must be one of: B, KB, MB, GB, TB, PB", sizeUnit));
    }

    // Convert nanoseconds to requested unit
    switch (timeUnit) {
      case "ns":
        finalTime = totalNanos;
        break;
      case "ms":
        finalTime = totalNanos / 1_000_000.0;
        break;
      case "s":
        finalTime = totalNanos / 1_000_000_000.0;
        break;
      case "m":
        finalTime = totalNanos / (60.0 * 1_000_000_000.0);
        break;
      case "h":
        finalTime = totalNanos / (60.0 * 60.0 * 1_000_000_000.0);
        break;
      case "d":
        finalTime = totalNanos / (24.0 * 60.0 * 60.0 * 1_000_000_000.0);
        break;
      default:
        throw new DirectiveExecutionException(
          String.format("Invalid time unit '%s'. Must be one of: ns, ms, s, m, h, d", timeUnit));
    }

    // Create result row
    Row result = new Row();
    result.add(totalSizeColumn, finalSize);
    result.add(totalTimeColumn, finalTime);

    return Collections.singletonList(result);
  }
} 