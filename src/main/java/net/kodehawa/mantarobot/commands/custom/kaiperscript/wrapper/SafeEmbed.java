package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;

import java.awt.Color;

public class SafeEmbed {
    public static EmbedBuilder builder(SafeEmbed e) {
        return e.builder;
    }

    private final EmbedBuilder builder = new EmbedBuilder();

    public SafeEmbed addBlankField(boolean inline) {
        builder.addBlankField(inline);
        return this;
    }

    public SafeEmbed addField(Field field) {
        builder.addField(field);
        return this;
    }

    public SafeEmbed addField(String name, String value, boolean inline) {
        builder.addField(name, value, inline);
        return this;
    }

    public SafeEmbed appendDescription(CharSequence description) {
        builder.appendDescription(description);
        return this;
    }

    public SafeEmbed author(String name) {
        return author(name, null);
    }

    public SafeEmbed author(String name, String url) {
        return author(name, url, null);
    }

    public SafeEmbed author(String name, String url, String iconUrl) {
        builder.setAuthor(name, url, iconUrl);
        return this;
    }

    public SafeEmbed clear() {
        builder.clearFields();
        return this;
    }

    public SafeEmbed color(Color color) {
        builder.setColor(color);
        return this;
    }

    public SafeEmbed description(CharSequence description) {
        builder.setDescription(description);
        return this;
    }

    public SafeEmbed footer(String text) {
        return footer(text, null);
    }

    public SafeEmbed footer(String text, String iconUrl) {
        builder.setFooter(text, iconUrl);
        return this;
    }

    public SafeEmbed image(String url) {
        builder.setImage(url);
        return this;
    }

    public SafeEmbed thumbnail(String url) {
        builder.setThumbnail(url);
        return this;
    }

    public SafeEmbed title(String title) {
        return title(title, null);
    }

    public SafeEmbed title(String title, String url) {
        builder.setTitle(title, url);
        return this;
    }
}
