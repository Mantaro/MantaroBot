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

package net.kodehawa.mantarobot.commands.custom;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EmbedJSON {
    public final List<EmbedField> fields = new ArrayList<>();
    public String author, authorImg, authorUrl;
    public String color;
    public String description;
    public String footer, footerImg;
    public String image;
    public String thumbnail;
    public String title, titleUrl;
    
    public MessageEmbed gen(Member member) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        if(title != null) embedBuilder.setTitle(title, titleUrl);
        if(description != null) embedBuilder.setDescription(description);
        if(author != null) embedBuilder.setAuthor(author, authorUrl, authorImg);
        if(footer != null) embedBuilder.setFooter(footer, footerImg);
        if(image != null) embedBuilder.setImage(image);
        if(thumbnail != null) embedBuilder.setThumbnail(thumbnail);
        if(color != null) {
            Color col = null;
            try {
                col = (Color) Color.class.getField(color).get(null);
            } catch(Exception ignored) {
                String colorLower = color.toLowerCase();
                if(colorLower.equals("member")) {
                    if(member != null)
                        col = member.getColor();
                } else if(colorLower.matches("#?(0x)?[0123456789abcdef]{1,6}")) {
                    try {
                        col = Color.decode(colorLower.startsWith("0x") ? colorLower : "0x" + colorLower);
                    } catch(Exception ignored2) {
                    }
                }
            }
            if(col != null) embedBuilder.setColor(col);
        }
        
        fields.forEach(f -> {
            if(f == null) {
                embedBuilder.addBlankField(false);
            } else if(f.value == null) {
                embedBuilder.addBlankField(f.inline);
            } else {
                embedBuilder.addField(f.name == null ? "" : f.name, f.value, f.inline);
            }
        });
        
        return embedBuilder.build();
    }
    
    public static class EmbedField {
        public boolean inline;
        public String name, value;
    }
}
