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

package net.kodehawa.mantarobot.graphql;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.api.internal.Utils;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import net.kodehawa.mantarobot.graphql.type.MediaFormat;
import net.kodehawa.mantarobot.graphql.type.MediaSeason;
import net.kodehawa.mantarobot.graphql.type.MediaStatus;
import net.kodehawa.mantarobot.graphql.type.MediaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public final class MediaSearchQuery implements Query<MediaSearchQuery.Data, MediaSearchQuery.Data, MediaSearchQuery.Variables> {
  public static final String OPERATION_DEFINITION = "query MediaSearch($query: String) {\n"
      + "  Page {\n"
      + "    __typename\n"
      + "    media(search: $query) {\n"
      + "      __typename\n"
      + "      title {\n"
      + "        __typename\n"
      + "        english(stylised: false)\n"
      + "        romaji(stylised: false)\n"
      + "        native(stylised: false)\n"
      + "      }\n"
      + "      coverImage {\n"
      + "        __typename\n"
      + "        medium\n"
      + "        large\n"
      + "      }\n"
      + "      format\n"
      + "      status\n"
      + "      startDate {\n"
      + "        __typename\n"
      + "        year\n"
      + "        month\n"
      + "        day\n"
      + "      }\n"
      + "      endDate {\n"
      + "        __typename\n"
      + "        year\n"
      + "        month\n"
      + "        day\n"
      + "      }\n"
      + "      season\n"
      + "      popularity\n"
      + "      siteUrl\n"
      + "      isAdult\n"
      + "      idMal\n"
      + "      type\n"
      + "      description(asHtml: false)\n"
      + "      episodes\n"
      + "      duration\n"
      + "      chapters\n"
      + "      volumes\n"
      + "      genres\n"
      + "      averageScore\n"
      + "      meanScore\n"
      + "      format\n"
      + "      nextAiringEpisode {\n"
      + "        __typename\n"
      + "        airingAt\n"
      + "        episode\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String OPERATION_ID = "788c4e6d52d9c9caef5f31fab034d08b5951f1bf9d0d590baa45f2f10283e164";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  public static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "MediaSearch";
    }
  };

  private final MediaSearchQuery.Variables variables;

  public MediaSearchQuery(@NotNull Input<String> query) {
    Utils.checkNotNull(query, "query == null");
    variables = new MediaSearchQuery.Variables(query);
  }

  @Override
  public String operationId() {
    return OPERATION_ID;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public MediaSearchQuery.Data wrapData(MediaSearchQuery.Data data) {
    return data;
  }

  @Override
  public MediaSearchQuery.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<MediaSearchQuery.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public OperationName name() {
    return OPERATION_NAME;
  }

  public static final class Builder {
    private Input<String> query = Input.absent();

    Builder() {
    }

    public Builder query(@Nullable String query) {
      this.query = Input.fromNullable(query);
      return this;
    }

    public Builder queryInput(@NotNull Input<String> query) {
      this.query = Utils.checkNotNull(query, "query == null");
      return this;
    }

    public MediaSearchQuery build() {
      return new MediaSearchQuery(query);
    }
  }

  public static final class Variables extends Operation.Variables {
    private final Input<String> query;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(Input<String> query) {
      this.query = query;
      if (query.defined) {
        this.valueMap.put("query", query.value);
      }
    }

    public Input<String> query() {
      return query;
    }

    @Override
    public Map<String, Object> valueMap() {
      return Collections.unmodifiableMap(valueMap);
    }

    @Override
    public InputFieldMarshaller marshaller() {
      return new InputFieldMarshaller() {
        @Override
        public void marshal(InputFieldWriter writer) throws IOException {
          if (query.defined) {
            writer.writeString("query", query.value);
          }
        }
      };
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("Page", "Page", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nullable Page Page;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Data(@Nullable Page Page) {
      this.Page = Page;
    }

    public @Nullable Page Page() {
      return this.Page;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], Page != null ? Page.marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "Page=" + Page
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.Page == null) ? (that.Page == null) : this.Page.equals(that.Page));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (Page == null) ? 0 : Page.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Page.Mapper pageFieldMapper = new Page.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final Page Page = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<Page>() {
          @Override
          public Page read(ResponseReader reader) {
            return pageFieldMapper.map(reader);
          }
        });
        return new Data(Page);
      }
    }
  }

  public static class Page {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("media", "media", new UnmodifiableMapBuilder<String, Object>(1)
      .put("search", new UnmodifiableMapBuilder<String, Object>(2)
        .put("kind", "Variable")
        .put("variableName", "query")
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @Nullable List<Medium> media;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Page(@NotNull String __typename, @Nullable List<Medium> media) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.media = media;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    public @Nullable List<Medium> media() {
      return this.media;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeList($responseFields[1], media, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeObject(((Medium) item).marshaller());
              }
            }
          });
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Page{"
          + "__typename=" + __typename + ", "
          + "media=" + media
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Page) {
        Page that = (Page) o;
        return this.__typename.equals(that.__typename)
         && ((this.media == null) ? (that.media == null) : this.media.equals(that.media));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= (media == null) ? 0 : media.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Page> {
      final Medium.Mapper mediumFieldMapper = new Medium.Mapper();

      @Override
      public Page map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final List<Medium> media = reader.readList($responseFields[1], new ResponseReader.ListReader<Medium>() {
          @Override
          public Medium read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<Medium>() {
              @Override
              public Medium read(ResponseReader reader) {
                return mediumFieldMapper.map(reader);
              }
            });
          }
        });
        return new Page(__typename, media);
      }
    }
  }

  public static class Medium {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("title", "title", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("coverImage", "coverImage", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("format", "format", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("status", "status", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("startDate", "startDate", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("endDate", "endDate", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("season", "season", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("popularity", "popularity", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("siteUrl", "siteUrl", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forBoolean("isAdult", "isAdult", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("idMal", "idMal", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("type", "type", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("description", "description", new UnmodifiableMapBuilder<String, Object>(1)
      .put("asHtml", false)
      .build(), true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("episodes", "episodes", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("duration", "duration", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("chapters", "chapters", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("volumes", "volumes", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("genres", "genres", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("averageScore", "averageScore", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("meanScore", "meanScore", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("nextAiringEpisode", "nextAiringEpisode", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @Nullable Title title;

    final @Nullable CoverImage coverImage;

    final @Nullable MediaFormat format;

    final @Nullable MediaStatus status;

    final @Nullable StartDate startDate;

    final @Nullable EndDate endDate;

    final @Nullable MediaSeason season;

    final @Nullable Integer popularity;

    final @Nullable String siteUrl;

    final @Nullable Boolean isAdult;

    final @Nullable Integer idMal;

    final @Nullable MediaType type;

    final @Nullable String description;

    final @Nullable Integer episodes;

    final @Nullable Integer duration;

    final @Nullable Integer chapters;

    final @Nullable Integer volumes;

    final @Nullable List<String> genres;

    final @Nullable Integer averageScore;

    final @Nullable Integer meanScore;

    final @Nullable NextAiringEpisode nextAiringEpisode;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Medium(@NotNull String __typename, @Nullable Title title,
        @Nullable CoverImage coverImage, @Nullable MediaFormat format, @Nullable MediaStatus status,
        @Nullable StartDate startDate, @Nullable EndDate endDate, @Nullable MediaSeason season,
        @Nullable Integer popularity, @Nullable String siteUrl, @Nullable Boolean isAdult,
        @Nullable Integer idMal, @Nullable MediaType type, @Nullable String description,
        @Nullable Integer episodes, @Nullable Integer duration, @Nullable Integer chapters,
        @Nullable Integer volumes, @Nullable List<String> genres, @Nullable Integer averageScore,
        @Nullable Integer meanScore, @Nullable NextAiringEpisode nextAiringEpisode) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.title = title;
      this.coverImage = coverImage;
      this.format = format;
      this.status = status;
      this.startDate = startDate;
      this.endDate = endDate;
      this.season = season;
      this.popularity = popularity;
      this.siteUrl = siteUrl;
      this.isAdult = isAdult;
      this.idMal = idMal;
      this.type = type;
      this.description = description;
      this.episodes = episodes;
      this.duration = duration;
      this.chapters = chapters;
      this.volumes = volumes;
      this.genres = genres;
      this.averageScore = averageScore;
      this.meanScore = meanScore;
      this.nextAiringEpisode = nextAiringEpisode;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * The official titles of the media in various languages
     */
    public @Nullable Title title() {
      return this.title;
    }

    /**
     * The cover images of the media
     */
    public @Nullable CoverImage coverImage() {
      return this.coverImage;
    }

    /**
     * The format the media was released in
     */
    public @Nullable MediaFormat format() {
      return this.format;
    }

    /**
     * The current releasing status of the media
     */
    public @Nullable MediaStatus status() {
      return this.status;
    }

    /**
     * The first official release date of the media
     */
    public @Nullable StartDate startDate() {
      return this.startDate;
    }

    /**
     * The last official release date of the media
     */
    public @Nullable EndDate endDate() {
      return this.endDate;
    }

    /**
     * The season the media was initially released in
     */
    public @Nullable MediaSeason season() {
      return this.season;
    }

    /**
     * The number of users with the media on their list
     */
    public @Nullable Integer popularity() {
      return this.popularity;
    }

    /**
     * The url for the media page on the AniList website
     */
    public @Nullable String siteUrl() {
      return this.siteUrl;
    }

    /**
     * If the media is intended only for 18+ adult audiences
     */
    public @Nullable Boolean isAdult() {
      return this.isAdult;
    }

    /**
     * The mal id of the media
     */
    public @Nullable Integer idMal() {
      return this.idMal;
    }

    /**
     * The type of the media; anime or manga
     */
    public @Nullable MediaType type() {
      return this.type;
    }

    /**
     * Short description of the media's story and characters
     */
    public @Nullable String description() {
      return this.description;
    }

    /**
     * The amount of episodes the anime has when complete
     */
    public @Nullable Integer episodes() {
      return this.episodes;
    }

    /**
     * The general length of each anime episode in minutes
     */
    public @Nullable Integer duration() {
      return this.duration;
    }

    /**
     * The amount of chapters the manga has when complete
     */
    public @Nullable Integer chapters() {
      return this.chapters;
    }

    /**
     * The amount of volumes the manga has when complete
     */
    public @Nullable Integer volumes() {
      return this.volumes;
    }

    /**
     * The genres of the media
     */
    public @Nullable List<String> genres() {
      return this.genres;
    }

    /**
     * A weighted average score of all the user's scores of the media
     */
    public @Nullable Integer averageScore() {
      return this.averageScore;
    }

    /**
     * Mean score of all the user's scores of the media
     */
    public @Nullable Integer meanScore() {
      return this.meanScore;
    }

    /**
     * The media's next episode airing schedule
     */
    public @Nullable NextAiringEpisode nextAiringEpisode() {
      return this.nextAiringEpisode;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeObject($responseFields[1], title != null ? title.marshaller() : null);
          writer.writeObject($responseFields[2], coverImage != null ? coverImage.marshaller() : null);
          writer.writeString($responseFields[3], format != null ? format.rawValue() : null);
          writer.writeString($responseFields[4], status != null ? status.rawValue() : null);
          writer.writeObject($responseFields[5], startDate != null ? startDate.marshaller() : null);
          writer.writeObject($responseFields[6], endDate != null ? endDate.marshaller() : null);
          writer.writeString($responseFields[7], season != null ? season.rawValue() : null);
          writer.writeInt($responseFields[8], popularity);
          writer.writeString($responseFields[9], siteUrl);
          writer.writeBoolean($responseFields[10], isAdult);
          writer.writeInt($responseFields[11], idMal);
          writer.writeString($responseFields[12], type != null ? type.rawValue() : null);
          writer.writeString($responseFields[13], description);
          writer.writeInt($responseFields[14], episodes);
          writer.writeInt($responseFields[15], duration);
          writer.writeInt($responseFields[16], chapters);
          writer.writeInt($responseFields[17], volumes);
          writer.writeList($responseFields[18], genres, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeString((String) item);
              }
            }
          });
          writer.writeInt($responseFields[19], averageScore);
          writer.writeInt($responseFields[20], meanScore);
          writer.writeObject($responseFields[21], nextAiringEpisode != null ? nextAiringEpisode.marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Medium{"
          + "__typename=" + __typename + ", "
          + "title=" + title + ", "
          + "coverImage=" + coverImage + ", "
          + "format=" + format + ", "
          + "status=" + status + ", "
          + "startDate=" + startDate + ", "
          + "endDate=" + endDate + ", "
          + "season=" + season + ", "
          + "popularity=" + popularity + ", "
          + "siteUrl=" + siteUrl + ", "
          + "isAdult=" + isAdult + ", "
          + "idMal=" + idMal + ", "
          + "type=" + type + ", "
          + "description=" + description + ", "
          + "episodes=" + episodes + ", "
          + "duration=" + duration + ", "
          + "chapters=" + chapters + ", "
          + "volumes=" + volumes + ", "
          + "genres=" + genres + ", "
          + "averageScore=" + averageScore + ", "
          + "meanScore=" + meanScore + ", "
          + "nextAiringEpisode=" + nextAiringEpisode
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Medium) {
        Medium that = (Medium) o;
        return this.__typename.equals(that.__typename)
         && ((this.title == null) ? (that.title == null) : this.title.equals(that.title))
         && ((this.coverImage == null) ? (that.coverImage == null) : this.coverImage.equals(that.coverImage))
         && ((this.format == null) ? (that.format == null) : this.format.equals(that.format))
         && ((this.status == null) ? (that.status == null) : this.status.equals(that.status))
         && ((this.startDate == null) ? (that.startDate == null) : this.startDate.equals(that.startDate))
         && ((this.endDate == null) ? (that.endDate == null) : this.endDate.equals(that.endDate))
         && ((this.season == null) ? (that.season == null) : this.season.equals(that.season))
         && ((this.popularity == null) ? (that.popularity == null) : this.popularity.equals(that.popularity))
         && ((this.siteUrl == null) ? (that.siteUrl == null) : this.siteUrl.equals(that.siteUrl))
         && ((this.isAdult == null) ? (that.isAdult == null) : this.isAdult.equals(that.isAdult))
         && ((this.idMal == null) ? (that.idMal == null) : this.idMal.equals(that.idMal))
         && ((this.type == null) ? (that.type == null) : this.type.equals(that.type))
         && ((this.description == null) ? (that.description == null) : this.description.equals(that.description))
         && ((this.episodes == null) ? (that.episodes == null) : this.episodes.equals(that.episodes))
         && ((this.duration == null) ? (that.duration == null) : this.duration.equals(that.duration))
         && ((this.chapters == null) ? (that.chapters == null) : this.chapters.equals(that.chapters))
         && ((this.volumes == null) ? (that.volumes == null) : this.volumes.equals(that.volumes))
         && ((this.genres == null) ? (that.genres == null) : this.genres.equals(that.genres))
         && ((this.averageScore == null) ? (that.averageScore == null) : this.averageScore.equals(that.averageScore))
         && ((this.meanScore == null) ? (that.meanScore == null) : this.meanScore.equals(that.meanScore))
         && ((this.nextAiringEpisode == null) ? (that.nextAiringEpisode == null) : this.nextAiringEpisode.equals(that.nextAiringEpisode));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= (title == null) ? 0 : title.hashCode();
        h *= 1000003;
        h ^= (coverImage == null) ? 0 : coverImage.hashCode();
        h *= 1000003;
        h ^= (format == null) ? 0 : format.hashCode();
        h *= 1000003;
        h ^= (status == null) ? 0 : status.hashCode();
        h *= 1000003;
        h ^= (startDate == null) ? 0 : startDate.hashCode();
        h *= 1000003;
        h ^= (endDate == null) ? 0 : endDate.hashCode();
        h *= 1000003;
        h ^= (season == null) ? 0 : season.hashCode();
        h *= 1000003;
        h ^= (popularity == null) ? 0 : popularity.hashCode();
        h *= 1000003;
        h ^= (siteUrl == null) ? 0 : siteUrl.hashCode();
        h *= 1000003;
        h ^= (isAdult == null) ? 0 : isAdult.hashCode();
        h *= 1000003;
        h ^= (idMal == null) ? 0 : idMal.hashCode();
        h *= 1000003;
        h ^= (type == null) ? 0 : type.hashCode();
        h *= 1000003;
        h ^= (description == null) ? 0 : description.hashCode();
        h *= 1000003;
        h ^= (episodes == null) ? 0 : episodes.hashCode();
        h *= 1000003;
        h ^= (duration == null) ? 0 : duration.hashCode();
        h *= 1000003;
        h ^= (chapters == null) ? 0 : chapters.hashCode();
        h *= 1000003;
        h ^= (volumes == null) ? 0 : volumes.hashCode();
        h *= 1000003;
        h ^= (genres == null) ? 0 : genres.hashCode();
        h *= 1000003;
        h ^= (averageScore == null) ? 0 : averageScore.hashCode();
        h *= 1000003;
        h ^= (meanScore == null) ? 0 : meanScore.hashCode();
        h *= 1000003;
        h ^= (nextAiringEpisode == null) ? 0 : nextAiringEpisode.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Medium> {
      final Title.Mapper titleFieldMapper = new Title.Mapper();

      final CoverImage.Mapper coverImageFieldMapper = new CoverImage.Mapper();

      final StartDate.Mapper startDateFieldMapper = new StartDate.Mapper();

      final EndDate.Mapper endDateFieldMapper = new EndDate.Mapper();

      final NextAiringEpisode.Mapper nextAiringEpisodeFieldMapper = new NextAiringEpisode.Mapper();

      @Override
      public Medium map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Title title = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Title>() {
          @Override
          public Title read(ResponseReader reader) {
            return titleFieldMapper.map(reader);
          }
        });
        final CoverImage coverImage = reader.readObject($responseFields[2], new ResponseReader.ObjectReader<CoverImage>() {
          @Override
          public CoverImage read(ResponseReader reader) {
            return coverImageFieldMapper.map(reader);
          }
        });
        final String formatStr = reader.readString($responseFields[3]);
        final MediaFormat format;
        if (formatStr != null) {
          format = MediaFormat.safeValueOf(formatStr);
        } else {
          format = null;
        }
        final String statusStr = reader.readString($responseFields[4]);
        final MediaStatus status;
        if (statusStr != null) {
          status = MediaStatus.safeValueOf(statusStr);
        } else {
          status = null;
        }
        final StartDate startDate = reader.readObject($responseFields[5], new ResponseReader.ObjectReader<StartDate>() {
          @Override
          public StartDate read(ResponseReader reader) {
            return startDateFieldMapper.map(reader);
          }
        });
        final EndDate endDate = reader.readObject($responseFields[6], new ResponseReader.ObjectReader<EndDate>() {
          @Override
          public EndDate read(ResponseReader reader) {
            return endDateFieldMapper.map(reader);
          }
        });
        final String seasonStr = reader.readString($responseFields[7]);
        final MediaSeason season;
        if (seasonStr != null) {
          season = MediaSeason.safeValueOf(seasonStr);
        } else {
          season = null;
        }
        final Integer popularity = reader.readInt($responseFields[8]);
        final String siteUrl = reader.readString($responseFields[9]);
        final Boolean isAdult = reader.readBoolean($responseFields[10]);
        final Integer idMal = reader.readInt($responseFields[11]);
        final String typeStr = reader.readString($responseFields[12]);
        final MediaType type;
        if (typeStr != null) {
          type = MediaType.safeValueOf(typeStr);
        } else {
          type = null;
        }
        final String description = reader.readString($responseFields[13]);
        final Integer episodes = reader.readInt($responseFields[14]);
        final Integer duration = reader.readInt($responseFields[15]);
        final Integer chapters = reader.readInt($responseFields[16]);
        final Integer volumes = reader.readInt($responseFields[17]);
        final List<String> genres = reader.readList($responseFields[18], new ResponseReader.ListReader<String>() {
          @Override
          public String read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readString();
          }
        });
        final Integer averageScore = reader.readInt($responseFields[19]);
        final Integer meanScore = reader.readInt($responseFields[20]);
        final NextAiringEpisode nextAiringEpisode = reader.readObject($responseFields[21], new ResponseReader.ObjectReader<NextAiringEpisode>() {
          @Override
          public NextAiringEpisode read(ResponseReader reader) {
            return nextAiringEpisodeFieldMapper.map(reader);
          }
        });
        return new Medium(__typename, title, coverImage, format, status, startDate, endDate, season, popularity, siteUrl, isAdult, idMal, type, description, episodes, duration, chapters, volumes, genres, averageScore, meanScore, nextAiringEpisode);
      }
    }
  }

  public static class Title {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("english", "english", new UnmodifiableMapBuilder<String, Object>(1)
      .put("stylised", false)
      .build(), true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("romaji", "romaji", new UnmodifiableMapBuilder<String, Object>(1)
      .put("stylised", false)
      .build(), true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("native", "native", new UnmodifiableMapBuilder<String, Object>(1)
      .put("stylised", false)
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @Nullable String english;

    final @Nullable String romaji;

    final @Nullable String native_;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Title(@NotNull String __typename, @Nullable String english, @Nullable String romaji,
        @Nullable String native_) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.english = english;
      this.romaji = romaji;
      this.native_ = native_;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * The official english title
     */
    public @Nullable String english() {
      return this.english;
    }

    /**
     * The romanization of the native language title
     */
    public @Nullable String romaji() {
      return this.romaji;
    }

    /**
     * Official title in it's native language
     */
    public @Nullable String native_() {
      return this.native_;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], english);
          writer.writeString($responseFields[2], romaji);
          writer.writeString($responseFields[3], native_);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Title{"
          + "__typename=" + __typename + ", "
          + "english=" + english + ", "
          + "romaji=" + romaji + ", "
          + "native_=" + native_
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Title) {
        Title that = (Title) o;
        return this.__typename.equals(that.__typename)
         && ((this.english == null) ? (that.english == null) : this.english.equals(that.english))
         && ((this.romaji == null) ? (that.romaji == null) : this.romaji.equals(that.romaji))
         && ((this.native_ == null) ? (that.native_ == null) : this.native_.equals(that.native_));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= (english == null) ? 0 : english.hashCode();
        h *= 1000003;
        h ^= (romaji == null) ? 0 : romaji.hashCode();
        h *= 1000003;
        h ^= (native_ == null) ? 0 : native_.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Title> {
      @Override
      public Title map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String english = reader.readString($responseFields[1]);
        final String romaji = reader.readString($responseFields[2]);
        final String native_ = reader.readString($responseFields[3]);
        return new Title(__typename, english, romaji, native_);
      }
    }
  }

  public static class CoverImage {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("medium", "medium", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("large", "large", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @Nullable String medium;

    final @Nullable String large;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public CoverImage(@NotNull String __typename, @Nullable String medium, @Nullable String large) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.medium = medium;
      this.large = large;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * The cover image of media at medium size
     */
    public @Nullable String medium() {
      return this.medium;
    }

    /**
     * The cover image of media at its largest size
     */
    public @Nullable String large() {
      return this.large;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], medium);
          writer.writeString($responseFields[2], large);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "CoverImage{"
          + "__typename=" + __typename + ", "
          + "medium=" + medium + ", "
          + "large=" + large
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof CoverImage) {
        CoverImage that = (CoverImage) o;
        return this.__typename.equals(that.__typename)
         && ((this.medium == null) ? (that.medium == null) : this.medium.equals(that.medium))
         && ((this.large == null) ? (that.large == null) : this.large.equals(that.large));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= (medium == null) ? 0 : medium.hashCode();
        h *= 1000003;
        h ^= (large == null) ? 0 : large.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<CoverImage> {
      @Override
      public CoverImage map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String medium = reader.readString($responseFields[1]);
        final String large = reader.readString($responseFields[2]);
        return new CoverImage(__typename, medium, large);
      }
    }
  }

  public static class StartDate {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("year", "year", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("month", "month", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("day", "day", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @Nullable Integer year;

    final @Nullable Integer month;

    final @Nullable Integer day;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public StartDate(@NotNull String __typename, @Nullable Integer year, @Nullable Integer month,
        @Nullable Integer day) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.year = year;
      this.month = month;
      this.day = day;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * Numeric Year (2017)
     */
    public @Nullable Integer year() {
      return this.year;
    }

    /**
     * Numeric Month (3)
     */
    public @Nullable Integer month() {
      return this.month;
    }

    /**
     * Numeric Day (24)
     */
    public @Nullable Integer day() {
      return this.day;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeInt($responseFields[1], year);
          writer.writeInt($responseFields[2], month);
          writer.writeInt($responseFields[3], day);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "StartDate{"
          + "__typename=" + __typename + ", "
          + "year=" + year + ", "
          + "month=" + month + ", "
          + "day=" + day
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof StartDate) {
        StartDate that = (StartDate) o;
        return this.__typename.equals(that.__typename)
         && ((this.year == null) ? (that.year == null) : this.year.equals(that.year))
         && ((this.month == null) ? (that.month == null) : this.month.equals(that.month))
         && ((this.day == null) ? (that.day == null) : this.day.equals(that.day));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= (year == null) ? 0 : year.hashCode();
        h *= 1000003;
        h ^= (month == null) ? 0 : month.hashCode();
        h *= 1000003;
        h ^= (day == null) ? 0 : day.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<StartDate> {
      @Override
      public StartDate map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Integer year = reader.readInt($responseFields[1]);
        final Integer month = reader.readInt($responseFields[2]);
        final Integer day = reader.readInt($responseFields[3]);
        return new StartDate(__typename, year, month, day);
      }
    }
  }

  public static class EndDate {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("year", "year", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("month", "month", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("day", "day", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @Nullable Integer year;

    final @Nullable Integer month;

    final @Nullable Integer day;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public EndDate(@NotNull String __typename, @Nullable Integer year, @Nullable Integer month,
        @Nullable Integer day) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.year = year;
      this.month = month;
      this.day = day;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * Numeric Year (2017)
     */
    public @Nullable Integer year() {
      return this.year;
    }

    /**
     * Numeric Month (3)
     */
    public @Nullable Integer month() {
      return this.month;
    }

    /**
     * Numeric Day (24)
     */
    public @Nullable Integer day() {
      return this.day;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeInt($responseFields[1], year);
          writer.writeInt($responseFields[2], month);
          writer.writeInt($responseFields[3], day);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "EndDate{"
          + "__typename=" + __typename + ", "
          + "year=" + year + ", "
          + "month=" + month + ", "
          + "day=" + day
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof EndDate) {
        EndDate that = (EndDate) o;
        return this.__typename.equals(that.__typename)
         && ((this.year == null) ? (that.year == null) : this.year.equals(that.year))
         && ((this.month == null) ? (that.month == null) : this.month.equals(that.month))
         && ((this.day == null) ? (that.day == null) : this.day.equals(that.day));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= (year == null) ? 0 : year.hashCode();
        h *= 1000003;
        h ^= (month == null) ? 0 : month.hashCode();
        h *= 1000003;
        h ^= (day == null) ? 0 : day.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<EndDate> {
      @Override
      public EndDate map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Integer year = reader.readInt($responseFields[1]);
        final Integer month = reader.readInt($responseFields[2]);
        final Integer day = reader.readInt($responseFields[3]);
        return new EndDate(__typename, year, month, day);
      }
    }
  }

  public static class NextAiringEpisode {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("airingAt", "airingAt", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("episode", "episode", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final int airingAt;

    final int episode;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public NextAiringEpisode(@NotNull String __typename, int airingAt, int episode) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.airingAt = airingAt;
      this.episode = episode;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * The time the episode airs at
     */
    public int airingAt() {
      return this.airingAt;
    }

    /**
     * The airing episode number
     */
    public int episode() {
      return this.episode;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeInt($responseFields[1], airingAt);
          writer.writeInt($responseFields[2], episode);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "NextAiringEpisode{"
          + "__typename=" + __typename + ", "
          + "airingAt=" + airingAt + ", "
          + "episode=" + episode
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof NextAiringEpisode) {
        NextAiringEpisode that = (NextAiringEpisode) o;
        return this.__typename.equals(that.__typename)
         && this.airingAt == that.airingAt
         && this.episode == that.episode;
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= airingAt;
        h *= 1000003;
        h ^= episode;
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<NextAiringEpisode> {
      @Override
      public NextAiringEpisode map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final int airingAt = reader.readInt($responseFields[1]);
        final int episode = reader.readInt($responseFields[2]);
        return new NextAiringEpisode(__typename, airingAt, episode);
      }
    }
  }
}
