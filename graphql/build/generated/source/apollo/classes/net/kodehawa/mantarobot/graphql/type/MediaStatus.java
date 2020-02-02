/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.graphql.type;

import java.lang.String;
import javax.annotation.Generated;

/**
 * The current releasing status of the media
 */
@Generated("Apollo GraphQL")
public enum MediaStatus {
  /**
   * Has completed and is no longer being released
   */
  FINISHED("FINISHED"),

  /**
   * Currently releasing
   */
  RELEASING("RELEASING"),

  /**
   * To be released at a later date
   */
  NOT_YET_RELEASED("NOT_YET_RELEASED"),

  /**
   * Ended before the work could be finished
   */
  CANCELLED("CANCELLED"),

  /**
   * Auto generated constant for unknown enum values
   */
  $UNKNOWN("$UNKNOWN");

  private final String rawValue;

  MediaStatus(String rawValue) {
    this.rawValue = rawValue;
  }

  public String rawValue() {
    return rawValue;
  }

  public static MediaStatus safeValueOf(String rawValue) {
    for (MediaStatus enumValue : values()) {
      if (enumValue.rawValue.equals(rawValue)) {
        return enumValue;
      }
    }
    return MediaStatus.$UNKNOWN;
  }
}
