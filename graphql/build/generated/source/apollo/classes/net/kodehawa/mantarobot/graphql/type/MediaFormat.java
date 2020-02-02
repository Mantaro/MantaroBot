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
