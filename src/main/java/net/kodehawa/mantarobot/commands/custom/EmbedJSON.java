package net.kodehawa.mantarobot.commands.custom;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class EmbedJSON {
	public static class EmbedField {
		public boolean inline;
		public String name, value;
	}

	public String author, authorImg, authorUrl;
	public String color;
	public String description;
	public final List<EmbedField> fields = new ArrayList<>();
	public String footer, footerImg;
	public String image;
	public String thumbnail;
	public String title, titleUrl;

	public MessageEmbed gen(GuildMessageReceivedEvent event) {
		EmbedBuilder embedBuilder = new EmbedBuilder();
		if (title != null) embedBuilder.setTitle(title, titleUrl);
		if (description != null) embedBuilder.setDescription(description);
		if (author != null) embedBuilder.setAuthor(author, authorUrl, authorImg);
		if (footer != null) embedBuilder.setFooter(footer, footerImg);
		if (image != null) embedBuilder.setImage(image);
		if (thumbnail != null) embedBuilder.setThumbnail(thumbnail);
		if (color != null) {
			Color col = null;
			try {
				col = (Color) Color.class.getField(color).get(null);
			} catch (Exception ignored) {
				String colorLower = color.toLowerCase();
				if (colorLower.equals("member")) {
					col = event.getMember().getColor();
				} else if (colorLower.matches("#?(0x)?[0123456789abcdef]{1,6}")) {
					try {
						col = Color.decode(colorLower.startsWith("0x") ? colorLower : "0x" + colorLower);
					} catch (Exception ignored2) {}
				}
			}
			if (col != null) embedBuilder.setColor(col);
		}

		fields.forEach(f -> {
			if (f == null) {
				embedBuilder.addBlankField(false);
			} else if (f.value == null) {
				embedBuilder.addBlankField(f.inline);
			} else {
				embedBuilder.addField(f.name == null ? "" : f.name, f.value, f.inline);
			}
		});

		return embedBuilder.build();
	}

	public MessageEmbed gen(GenericGuildMemberEvent event) {
		EmbedBuilder embed = new EmbedBuilder();
		if (title != null) embed.setTitle(title, titleUrl);
		if (description != null) embed.setDescription(description);
		if (author != null) embed.setAuthor(author, authorUrl, authorImg);
		if (footer != null) embed.setFooter(footer, footerImg);
		if (image != null && urlExists(image)) embed.setImage(image);
		if (thumbnail != null) embed.setThumbnail(thumbnail);
		if (color != null) {
			Color c = null;
			try {
				c = (Color) Color.class.getField(color).get(null);
			} catch (Exception ignored) {
				String colorLower = color.toLowerCase();
				if (colorLower.equals("member")) {
					c = event.getMember().getColor();
				} else if (colorLower.matches("#?(0x)?[0123456789abcdef]{1,6}")) {
					try {
						c = Color.decode(colorLower.startsWith("0x") ? colorLower : "0x" + colorLower);
					} catch (Exception ignored2) {}
				}
			}
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

	public boolean urlExists(String URLName) {
		try {
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection con = (HttpURLConnection) new URL(URLName).openConnection();
			con.setRequestMethod("HEAD");
			return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
		}
		catch (Exception e) {
			return false;
		}
	}

}
