package net.kodehawa.mantarobot.commands.anime;

public class CharacterData {
    public Boolean favourite = null;
    public Integer id = null;
    public String image_url_lge = null;
    public String image_url_med = null;
    public String info = null;
    public String name_alt = null;
    public String name_first = null;
    public String name_japanese = null;
    public String name_last = null;

    public Integer getId() {
        return id;
    }

    public String getImage_url_lge() {
        return image_url_lge;
    }

    public String getImage_url_med() {
        return image_url_med;
    }

    public String getInfo() {
        return info;
    }

    public String getName_alt() {
        return name_alt;
    }

    public String getName_first() {
        return name_first;
    }

    public String getName_japanese() {
        return name_japanese;
    }

    public String getName_last() {
        return name_last;
    }
}
