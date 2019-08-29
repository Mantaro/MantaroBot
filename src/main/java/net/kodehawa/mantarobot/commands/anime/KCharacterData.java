package net.kodehawa.mantarobot.commands.anime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

@Getter
//Kitsu API character data.
public class KCharacterData {
    private String id;
    private String type;
    private Attributes attributes;

    @Getter
    public static class Attributes {
        private Names names;
        private String name;

        private String description;

        private Image image;
    }

    @Getter
    public static class Names {
        private String en;
        private String ja_jp;
    }

    @Getter
    public static class Image {
        private String original;
    }

    @JsonIgnore
    public String getURL() {
        return "https://kitsu.io/character/" + id;
    }

}
