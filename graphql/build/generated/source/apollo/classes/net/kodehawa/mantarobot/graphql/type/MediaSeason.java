package net.kodehawa.mantarobot.graphql.type;

import java.lang.String;
import javax.annotation.Generated;

@Generated("Apollo GraphQL")
public enum MediaSeason {
  /**
   * Months December to February
   */
  WINTER("WINTER"),

  /**
   * Months March to May
   */
  SPRING("SPRING"),

  /**
   * Months June to August
   */
  SUMMER("SUMMER"),

  /**
   * Months September to November
   */
  FALL("FALL"),

  /**
   * Auto generated constant for unknown enum values
   */
  $UNKNOWN("$UNKNOWN");

  private final String rawValue;

  MediaSeason(String rawValue) {
    this.rawValue = rawValue;
  }

  public String rawValue() {
    return rawValue;
  }

  public static MediaSeason safeValueOf(String rawValue) {
    for (MediaSeason enumValue : values()) {
      if (enumValue.rawValue.equals(rawValue)) {
        return enumValue;
      }
    }
    return MediaSeason.$UNKNOWN;
  }
}
