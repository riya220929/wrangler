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

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * A {@link Token} implementation for representing time durations with units.
 */
@PublicEvolving
public class TimeDuration implements Token {
  private final String raw;
  private final long nanoseconds;

  public TimeDuration(String value) {
    this.raw = value;
    this.nanoseconds = parseNanoseconds(value);
  }

  private long parseNanoseconds(String value) {
    String number = value.replaceAll("[^0-9.]", "");
    String unit = value.replaceAll("[0-9.]", "").toLowerCase();
    double duration = Double.parseDouble(number);

    switch (unit) {
      case "ns":
        return (long) duration;
      case "ms":
        return (long) (duration * 1_000_000);
      case "s":
        return (long) (duration * 1_000_000_000);
      case "m":
        return (long) (duration * 60 * 1_000_000_000);
      case "h":
        return (long) (duration * 60 * 60 * 1_000_000_000);
      case "d":
        return (long) (duration * 24 * 60 * 60 * 1_000_000_000);
      default:
        throw new IllegalArgumentException("Invalid time duration unit: " + unit);
    }
  }

  @Override
  public Object value() {
    return nanoseconds;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", type().name());
    object.addProperty("value", raw);
    object.addProperty("nanoseconds", nanoseconds);
    return object;
  }

  /**
   * Gets the duration in nanoseconds.
   *
   * @return The duration in nanoseconds
   */
  public long getNanoseconds() {
    return nanoseconds;
  }

  /**
   * Gets the original raw string value.
   *
   * @return The original string value with unit
   */
  public String getRawValue() {
    return raw;
  }

  /**
   * Gets the duration in milliseconds.
   *
   * @return The duration in milliseconds
   */
  public long getMilliseconds() {
    return nanoseconds / 1_000_000;
  }

  /**
   * Gets the duration in seconds.
   *
   * @return The duration in seconds
   */
  public long getSeconds() {
    return nanoseconds / 1_000_000_000;
  }
} 