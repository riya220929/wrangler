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

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link AggregateStats}
 */
public class AggregateStatsTest {

  @Test
  public void testBasicAggregation() throws Exception {
    List<Row> rows = Arrays.asList(
      createRow("10KB", "100ms"),
      createRow("20KB", "200ms"),
      createRow("30KB", "300ms")
    );

    String[] directives = new String[] {
      "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
    };

    List<Row> results = TestingRig.execute(directives, rows);

    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    
    // Expected: 60KB = 0.05859375 MB
    Assert.assertEquals(0.05859375, ((Number) result.getValue("total_size_mb")).doubleValue(), 0.000001);
    // Expected: 600ms = 0.6 seconds
    Assert.assertEquals(0.6, ((Number) result.getValue("total_time_sec")).doubleValue(), 0.000001);
  }

  @Test
  public void testAverageAggregation() throws Exception {
    List<Row> rows = Arrays.asList(
      createRow("30MB", "3s"),
      createRow("60MB", "6s"),
      createRow("90MB", "9s")
    );

    String[] directives = new String[] {
      "aggregate-stats :data_size :response_time total_size_gb total_time_m average true"
    };

    List<Row> results = TestingRig.execute(directives, rows);

    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    
    // Expected average: 60MB = 0.05859375 GB
    Assert.assertEquals(0.05859375, ((Number) result.getValue("total_size_gb")).doubleValue(), 0.000001);
    // Expected average: 6s = 0.1 minutes
    Assert.assertEquals(0.1, ((Number) result.getValue("total_time_m")).doubleValue(), 0.000001);
  }

  @Test
  public void testDifferentUnits() throws Exception {
    List<Row> rows = Arrays.asList(
      createRow("1GB", "1h"),
      createRow("1024MB", "60m"),
      createRow("1048576KB", "3600s")
    );

    String[] directives = new String[] {
      "aggregate-stats :data_size :response_time total_size_gb total_time_h"
    };

    List<Row> results = TestingRig.execute(directives, rows);

    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    
    // Expected: 3GB total
    Assert.assertEquals(3.0, ((Number) result.getValue("total_size_gb")).doubleValue(), 0.000001);
    // Expected: 3 hours total
    Assert.assertEquals(3.0, ((Number) result.getValue("total_time_h")).doubleValue(), 0.000001);
  }

  private Row createRow(String size, String time) {
    Row row = new Row();
    row.add("data_size", new ByteSize(size));
    row.add("response_time", new TimeDuration(time));
    return row;
  }
} 