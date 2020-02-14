package net.kodehawa.mantarobot.graphql.type;

import java.lang.String;
import javax.annotation.Generated;

/**
 * Media type enum, anime or manga.
 */
@Generated("Apollo GraphQL")
public enum MediaType {
  /**
   * Japanese Anime
   */
  ANIME("ANIME"),

  /**
   * Asian comic
   */
  MANGA("MANGA"),

  /**
   * Auto generated constant for unknown enum values
   */
  $UNKNOWN("$UNKNOWN");

  private final String rawValue;

  MediaType(String rawValue) {
    this.rawValue = rawValue;
  }

  public String rawValue() {
    return rawValue;
  }

  public static MediaType safeValueOf(String rawValue) {
    for (MediaType enumValue : values()) {
      if (enumValue.rawValue.equals(rawValue)) {
        return enumValue;
      }
    }
    return MediaType.$UNKNOWN;
  }
}
