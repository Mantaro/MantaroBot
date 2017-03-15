package net.kodehawa.mantarobot.commands.custom;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class EmbedJSON {
	public static class EmbedField {
		public String name, value;
		public boolean inline;
	}

	public String author, authorImg, authorUrl;
	public String color;
	public String description;
	public String footer, footerUrl;
	public String image;
	public String thumbnail;
	public String title, titleUrl;
	public List<EmbedField> fields = new ArrayList<>();

	public MessageEmbed gen() {
		EmbedBuilder embed = new EmbedBuilder();
		if (title != null) embed.setTitle(title, titleUrl);
		if (description != null) embed.setDescription(description);
		if (author != null) embed.setAuthor(author, authorUrl, authorImg);
		if (footer != null) embed.setFooter(footer, footerUrl);
		if (image != null) embed.setImage(image);
		if (thumbnail != null) embed.setThumbnail(thumbnail);
		if (color != null) {
			Color c = Color.getColor(color);
			if (c != null) embed.setColor(c);
		}

		fields.forEach(f -> {
			if (f == null) {
				embed.addBlankField(false);
			} else if (f.value == null) {
				embed.addBlankField(f.inline);
			} else {
				embed.addField(f.name == null ? "" : f.name, f.value, f.inline);
			}
		});

		return embed.build();
	}
}
