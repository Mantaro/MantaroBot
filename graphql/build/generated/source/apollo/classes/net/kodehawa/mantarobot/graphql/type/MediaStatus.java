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
