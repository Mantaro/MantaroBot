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
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import net.kodehawa.mantarobot.graphql.type.MediaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public final class CharacterSearchQuery implements Query<CharacterSearchQuery.Data, CharacterSearchQuery.Data, CharacterSearchQuery.Variables> {
  public static final String OPERATION_DEFINITION = "query CharacterSearch($query: String) {\n"
      + "  Page {\n"
      + "    __typename\n"
      + "    characters(search: $query) {\n"
      + "      __typename\n"
      + "      id\n"
      + "      name {\n"
      + "        __typename\n"
      + "        first\n"
      + "        last\n"
      + "        native\n"
      + "        alternative\n"
      + "      }\n"
      + "      image {\n"
      + "        __typename\n"
      + "        medium\n"
      + "        large\n"
      + "      }\n"
      + "      siteUrl\n"
      + "      description(asHtml: false)\n"
      + "      media {\n"
      + "        __typename\n"
      + "        nodes {\n"
      + "          __typename\n"
      + "          title {\n"
      + "            __typename\n"
      + "            english(stylised: false)\n"
      + "            romaji(stylised: false)\n"
      + "            native(stylised: false)\n"
      + "          }\n"
      + "          type\n"
      + "          description(asHtml: false)\n"
      + "          episodes\n"
      + "          duration\n"
      + "          chapters\n"
      + "          volumes\n"
      + "          genres\n"
      + "          averageScore\n"
      + "          meanScore\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String OPERATION_ID = "30d74d0e9175756161f87386c259e0b2a2a3c28a0aaee87a55fd467b689a9294";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  public static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "CharacterSearch";
    }
  };

  private final CharacterSearchQuery.Variables variables;

  public CharacterSearchQuery(@NotNull Input<String> query) {
    Utils.checkNotNull(query, "query == null");
    variables = new CharacterSearchQuery.Variables(query);
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
  public CharacterSearchQuery.Data wrapData(CharacterSearchQuery.Data data) {
    return data;
  }

  @Override
  public CharacterSearchQuery.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<CharacterSearchQuery.Data> responseFieldMapper() {
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

    public CharacterSearchQuery build() {
      return new CharacterSearchQuery(query);
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
      ResponseField.forList("characters", "characters", new UnmodifiableMapBuilder<String, Object>(1)
      .put("search", new UnmodifiableMapBuilder<String, Object>(2)
        .put("kind", "Variable")
        .put("variableName", "query")
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @Nullable List<Character> characters;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Page(@NotNull String __typename, @Nullable List<Character> characters) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.characters = characters;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    public @Nullable List<Character> characters() {
      return this.characters;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeList($responseFields[1], characters, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeObject(((Character) item).marshaller());
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
          + "characters=" + characters
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
         && ((this.characters == null) ? (that.characters == null) : this.characters.equals(that.characters));
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
        h ^= (characters == null) ? 0 : characters.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Page> {
      final Character.Mapper characterFieldMapper = new Character.Mapper();

      @Override
      public Page map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final List<Character> characters = reader.readList($responseFields[1], new ResponseReader.ListReader<Character>() {
          @Override
          public Character read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<Character>() {
              @Override
              public Character read(ResponseReader reader) {
                return characterFieldMapper.map(reader);
              }
            });
          }
        });
        return new Page(__typename, characters);
      }
    }
  }

  public static class Character {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("id", "id", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("name", "name", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("image", "image", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("siteUrl", "siteUrl", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("description", "description", new UnmodifiableMapBuilder<String, Object>(1)
      .put("asHtml", false)
      .build(), true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("media", "media", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final int id;

    final @Nullable Name name;

    final @Nullable Image image;

    final @Nullable String siteUrl;

    final @Nullable String description;

    final @Nullable Media media;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Character(@NotNull String __typename, int id, @Nullable Name name, @Nullable Image image,
        @Nullable String siteUrl, @Nullable String description, @Nullable Media media) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.id = id;
      this.name = name;
      this.image = image;
      this.siteUrl = siteUrl;
      this.description = description;
      this.media = media;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * The id of the character
     */
    public int id() {
      return this.id;
    }

    /**
     * The names of the character
     */
    public @Nullable Name name() {
      return this.name;
    }

    /**
     * Character images
     */
    public @Nullable Image image() {
      return this.image;
    }

    /**
     * The url for the character page on the AniList website
     */
    public @Nullable String siteUrl() {
      return this.siteUrl;
    }

    /**
     * A general description of the character
     */
    public @Nullable String description() {
      return this.description;
    }

    /**
     * Media that includes the character
     */
    public @Nullable Media media() {
      return this.media;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeInt($responseFields[1], id);
          writer.writeObject($responseFields[2], name != null ? name.marshaller() : null);
          writer.writeObject($responseFields[3], image != null ? image.marshaller() : null);
          writer.writeString($responseFields[4], siteUrl);
          writer.writeString($responseFields[5], description);
          writer.writeObject($responseFields[6], media != null ? media.marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Character{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "name=" + name + ", "
          + "image=" + image + ", "
          + "siteUrl=" + siteUrl + ", "
          + "description=" + description + ", "
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
      if (o instanceof Character) {
        Character that = (Character) o;
        return this.__typename.equals(that.__typename)
         && this.id == that.id
         && ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
         && ((this.image == null) ? (that.image == null) : this.image.equals(that.image))
         && ((this.siteUrl == null) ? (that.siteUrl == null) : this.siteUrl.equals(that.siteUrl))
         && ((this.description == null) ? (that.description == null) : this.description.equals(that.description))
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
        h ^= id;
        h *= 1000003;
        h ^= (name == null) ? 0 : name.hashCode();
        h *= 1000003;
        h ^= (image == null) ? 0 : image.hashCode();
        h *= 1000003;
        h ^= (siteUrl == null) ? 0 : siteUrl.hashCode();
        h *= 1000003;
        h ^= (description == null) ? 0 : description.hashCode();
        h *= 1000003;
        h ^= (media == null) ? 0 : media.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Character> {
      final Name.Mapper nameFieldMapper = new Name.Mapper();

      final Image.Mapper imageFieldMapper = new Image.Mapper();

      final Media.Mapper mediaFieldMapper = new Media.Mapper();

      @Override
      public Character map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final int id = reader.readInt($responseFields[1]);
        final Name name = reader.readObject($responseFields[2], new ResponseReader.ObjectReader<Name>() {
          @Override
          public Name read(ResponseReader reader) {
            return nameFieldMapper.map(reader);
          }
        });
        final Image image = reader.readObject($responseFields[3], new ResponseReader.ObjectReader<Image>() {
          @Override
          public Image read(ResponseReader reader) {
            return imageFieldMapper.map(reader);
          }
        });
        final String siteUrl = reader.readString($responseFields[4]);
        final String description = reader.readString($responseFields[5]);
        final Media media = reader.readObject($responseFields[6], new ResponseReader.ObjectReader<Media>() {
          @Override
          public Media read(ResponseReader reader) {
            return mediaFieldMapper.map(reader);
          }
        });
        return new Character(__typename, id, name, image, siteUrl, description, media);
      }
    }
  }

  public static class Name {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("first", "first", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("last", "last", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("native", "native", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("alternative", "alternative", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @Nullable String first;

    final @Nullable String last;

    final @Nullable String native_;

    final @Nullable List<String> alternative;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Name(@NotNull String __typename, @Nullable String first, @Nullable String last,
        @Nullable String native_, @Nullable List<String> alternative) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.first = first;
      this.last = last;
      this.native_ = native_;
      this.alternative = alternative;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * The character's given name
     */
    public @Nullable String first() {
      return this.first;
    }

    /**
     * The character's surname
     */
    public @Nullable String last() {
      return this.last;
    }

    /**
     * The character's full name in their native language
     */
    public @Nullable String native_() {
      return this.native_;
    }

    /**
     * Other names the character might be referred to as
     */
    public @Nullable List<String> alternative() {
      return this.alternative;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], first);
          writer.writeString($responseFields[2], last);
          writer.writeString($responseFields[3], native_);
          writer.writeList($responseFields[4], alternative, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeString((String) item);
              }
            }
          });
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Name{"
          + "__typename=" + __typename + ", "
          + "first=" + first + ", "
          + "last=" + last + ", "
          + "native_=" + native_ + ", "
          + "alternative=" + alternative
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Name) {
        Name that = (Name) o;
        return this.__typename.equals(that.__typename)
         && ((this.first == null) ? (that.first == null) : this.first.equals(that.first))
         && ((this.last == null) ? (that.last == null) : this.last.equals(that.last))
         && ((this.native_ == null) ? (that.native_ == null) : this.native_.equals(that.native_))
         && ((this.alternative == null) ? (that.alternative == null) : this.alternative.equals(that.alternative));
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
        h ^= (first == null) ? 0 : first.hashCode();
        h *= 1000003;
        h ^= (last == null) ? 0 : last.hashCode();
        h *= 1000003;
        h ^= (native_ == null) ? 0 : native_.hashCode();
        h *= 1000003;
        h ^= (alternative == null) ? 0 : alternative.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Name> {
      @Override
      public Name map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String first = reader.readString($responseFields[1]);
        final String last = reader.readString($responseFields[2]);
        final String native_ = reader.readString($responseFields[3]);
        final List<String> alternative = reader.readList($responseFields[4], new ResponseReader.ListReader<String>() {
          @Override
          public String read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readString();
          }
        });
        return new Name(__typename, first, last, native_, alternative);
      }
    }
  }

  public static class Image {
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

    public Image(@NotNull String __typename, @Nullable String medium, @Nullable String large) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.medium = medium;
      this.large = large;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * The character's image of media at medium size
     */
    public @Nullable String medium() {
      return this.medium;
    }

    /**
     * The character's image of media at its largest size
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
        $toString = "Image{"
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
      if (o instanceof Image) {
        Image that = (Image) o;
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

    public static final class Mapper implements ResponseFieldMapper<Image> {
      @Override
      public Image map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String medium = reader.readString($responseFields[1]);
        final String large = reader.readString($responseFields[2]);
        return new Image(__typename, medium, large);
      }
    }
  }

  public static class Media {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("nodes", "nodes", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @Nullable List<Node> nodes;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Media(@NotNull String __typename, @Nullable List<Node> nodes) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.nodes = nodes;
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    public @Nullable List<Node> nodes() {
      return this.nodes;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeList($responseFields[1], nodes, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeObject(((Node) item).marshaller());
              }
            }
          });
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Media{"
          + "__typename=" + __typename + ", "
          + "nodes=" + nodes
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Media) {
        Media that = (Media) o;
        return this.__typename.equals(that.__typename)
         && ((this.nodes == null) ? (that.nodes == null) : this.nodes.equals(that.nodes));
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
        h ^= (nodes == null) ? 0 : nodes.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Media> {
      final Node.Mapper nodeFieldMapper = new Node.Mapper();

      @Override
      public Media map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final List<Node> nodes = reader.readList($responseFields[1], new ResponseReader.ListReader<Node>() {
          @Override
          public Node read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<Node>() {
              @Override
              public Node read(ResponseReader reader) {
                return nodeFieldMapper.map(reader);
              }
            });
          }
        });
        return new Media(__typename, nodes);
      }
    }
  }

  public static class Node {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("title", "title", null, true, Collections.<ResponseField.Condition>emptyList()),
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
      ResponseField.forInt("meanScore", "meanScore", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @Nullable Title title;

    final @Nullable MediaType type;

    final @Nullable String description;

    final @Nullable Integer episodes;

    final @Nullable Integer duration;

    final @Nullable Integer chapters;

    final @Nullable Integer volumes;

    final @Nullable List<String> genres;

    final @Nullable Integer averageScore;

    final @Nullable Integer meanScore;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Node(@NotNull String __typename, @Nullable Title title, @Nullable MediaType type,
        @Nullable String description, @Nullable Integer episodes, @Nullable Integer duration,
        @Nullable Integer chapters, @Nullable Integer volumes, @Nullable List<String> genres,
        @Nullable Integer averageScore, @Nullable Integer meanScore) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.title = title;
      this.type = type;
      this.description = description;
      this.episodes = episodes;
      this.duration = duration;
      this.chapters = chapters;
      this.volumes = volumes;
      this.genres = genres;
      this.averageScore = averageScore;
      this.meanScore = meanScore;
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

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeObject($responseFields[1], title != null ? title.marshaller() : null);
          writer.writeString($responseFields[2], type != null ? type.rawValue() : null);
          writer.writeString($responseFields[3], description);
          writer.writeInt($responseFields[4], episodes);
          writer.writeInt($responseFields[5], duration);
          writer.writeInt($responseFields[6], chapters);
          writer.writeInt($responseFields[7], volumes);
          writer.writeList($responseFields[8], genres, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeString((String) item);
              }
            }
          });
          writer.writeInt($responseFields[9], averageScore);
          writer.writeInt($responseFields[10], meanScore);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Node{"
          + "__typename=" + __typename + ", "
          + "title=" + title + ", "
          + "type=" + type + ", "
          + "description=" + description + ", "
          + "episodes=" + episodes + ", "
          + "duration=" + duration + ", "
          + "chapters=" + chapters + ", "
          + "volumes=" + volumes + ", "
          + "genres=" + genres + ", "
          + "averageScore=" + averageScore + ", "
          + "meanScore=" + meanScore
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Node) {
        Node that = (Node) o;
        return this.__typename.equals(that.__typename)
         && ((this.title == null) ? (that.title == null) : this.title.equals(that.title))
         && ((this.type == null) ? (that.type == null) : this.type.equals(that.type))
         && ((this.description == null) ? (that.description == null) : this.description.equals(that.description))
         && ((this.episodes == null) ? (that.episodes == null) : this.episodes.equals(that.episodes))
         && ((this.duration == null) ? (that.duration == null) : this.duration.equals(that.duration))
         && ((this.chapters == null) ? (that.chapters == null) : this.chapters.equals(that.chapters))
         && ((this.volumes == null) ? (that.volumes == null) : this.volumes.equals(that.volumes))
         && ((this.genres == null) ? (that.genres == null) : this.genres.equals(that.genres))
         && ((this.averageScore == null) ? (that.averageScore == null) : this.averageScore.equals(that.averageScore))
         && ((this.meanScore == null) ? (that.meanScore == null) : this.meanScore.equals(that.meanScore));
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
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Node> {
      final Title.Mapper titleFieldMapper = new Title.Mapper();

      @Override
      public Node map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Title title = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Title>() {
          @Override
          public Title read(ResponseReader reader) {
            return titleFieldMapper.map(reader);
          }
        });
        final String typeStr = reader.readString($responseFields[2]);
        final MediaType type;
        if (typeStr != null) {
          type = MediaType.safeValueOf(typeStr);
        } else {
          type = null;
        }
        final String description = reader.readString($responseFields[3]);
        final Integer episodes = reader.readInt($responseFields[4]);
        final Integer duration = reader.readInt($responseFields[5]);
        final Integer chapters = reader.readInt($responseFields[6]);
        final Integer volumes = reader.readInt($responseFields[7]);
        final List<String> genres = reader.readList($responseFields[8], new ResponseReader.ListReader<String>() {
          @Override
          public String read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readString();
          }
        });
        final Integer averageScore = reader.readInt($responseFields[9]);
        final Integer meanScore = reader.readInt($responseFields[10]);
        return new Node(__typename, title, type, description, episodes, duration, chapters, volumes, genres, averageScore, meanScore);
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
}
