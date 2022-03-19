/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.action;

import net.dv8tion.jda.api.EmbedBuilder;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ImageCmdSlash extends SlashCommand {
    public static final URLCache CACHE = new URLCache(10);

    private final String desc;
    private final String imageName;
    private final String toSend;
    private final WeebAPIRequester weebapi = new WeebAPIRequester();
    private final Random rand = new Random();
    private List<String> images;
    private boolean noMentions = false;
    private String type;

    public ImageCmdSlash(String desc, String imageName, List<String> images, String toSend) {
        setCategory(CommandCategory.ACTION);
        this.desc = desc;
        this.imageName = imageName;
        this.images = images;
        this.toSend = toSend;
        setHelp(new HelpContent.Builder()
                .setDescription(desc)
                .build()
        );
    }

    public ImageCmdSlash(String desc, String imageName, String type, String toSend) {
        setCategory(CommandCategory.ACTION);
        this.desc = desc;
        this.imageName = imageName;
        this.images = Collections.emptyList();
        this.toSend = toSend;
        this.type = type;
        setHelp(new HelpContent.Builder()
                .setDescription(desc)
                .build()
        );
    }

    public ImageCmdSlash(String desc, String imageName, String type, String toSend, boolean noMentions) {
        setCategory(CommandCategory.ACTION);
        this.desc = desc;
        this.imageName = imageName;
        this.images = Collections.emptyList();
        this.toSend = toSend;
        this.noMentions = noMentions;
        this.type = type;
        setHelp(new HelpContent.Builder()
                .setDescription(desc)
                .build()
        );
    }

    @Override
    protected void process(SlashContext ctx) {
        ctx.defer();
        final var builder = new EmbedBuilder();
        String random;
        try {
            if (type != null) {
                var result = weebapi.getRandomImageByType(type, false, null);
                images = Collections.singletonList(result.getKey());
                random = images.get(0); //Guaranteed random selection :^).
            } else {
                if (images.isEmpty()) {
                    ctx.reply("commands.action.no_type", EmoteReference.ERROR);
                    return;
                }

                random = images.get(rand.nextInt(images.size()));
            }
        } catch (Exception e) {
            ctx.reply("commands.action.error_retrieving", EmoteReference.ERROR);
            return;
        }

        builder.appendDescription(EmoteReference.TALKING.toString());
        var user = ctx.getOptionAsUser("user");
        if (user != null) {
            var member = ctx.getGuild().getMember(user);
            builder.appendDescription("**%s**, ".formatted(member.getEffectiveName()));
        }

        builder.appendDescription(ctx.getLanguageContext().get(toSend));
        builder.setColor(ctx.getMemberColor())
                .setImage(random)
                .build();

        ctx.send(builder.build());
    }
}
