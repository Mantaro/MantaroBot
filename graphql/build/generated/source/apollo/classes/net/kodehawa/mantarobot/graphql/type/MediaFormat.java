package net.kodehawa.mantarobot.graphql.type;

import java.lang.String;
import javax.annotation.Generated;

/**
 * The format the media was released in
 */
@Generated("Apollo GraphQL")
public enum MediaFormat {
  /**
   * Anime broadcast on television
   */
  TV("TV"),

  /**
   * Anime which are under 15 minutes in length and broadcast on television
   */
  TV_SHORT("TV_SHORT"),

  /**
   * Anime movies with a theatrical release
   */
  MOVIE("MOVIE"),

  /**
   * Special episodes that have been included in DVD/Blu-ray releases, picture dramas, pilots, etc
   */
  SPECIAL("SPECIAL"),

  /**
   * (Original Video Animation) Anime that have been released directly on DVD/Blu-ray without originally going through a theatrical release or television broadcast
   */
  OVA("OVA"),

  /**
   * (Original Net Animation) Anime that have been originally released online or are only available through streaming services.
   */
  ONA("ONA"),

  /**
   * Short anime released as a music video
   */
  MUSIC("MUSIC"),

  /**
   * Professionally published manga with more than one chapter
   */
  MANGA("MANGA"),

  /**
   * Written books released as a novel or series of light novels
   */
  NOVEL("NOVEL"),

  /**
   * Manga with just one chapter
   */
  ONE_SHOT("ONE_SHOT"),

  /**
   * Auto generated constant for unknown enum values
   */
  $UNKNOWN("$UNKNOWN");

  private final String rawValue;

  MediaFormat(String rawValue) {
    this.rawValue = rawValue;
  }

  public String rawValue() {
    return rawValue;
  }

  public static MediaFormat safeValueOf(String rawValue) {
    for (MediaFormat enumValue : values()) {
      if (enumValue.rawValue.equals(rawValue)) {
        return enumValue;
      }
    }
    return MediaFormat.$UNKNOWN;
  }
}
