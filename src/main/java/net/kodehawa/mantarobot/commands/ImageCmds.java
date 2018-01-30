/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.collections.CollectionUtils;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.imageboards.DefaultImageBoards;
import net.kodehawa.lib.imageboards.ImageBoard;
import net.kodehawa.lib.imageboards.entities.impl.*;
import net.kodehawa.mantarobot.commands.action.WeebAPIRequester;
import net.kodehawa.mantarobot.commands.image.ImageRequestType;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import java.awt.*;

import static net.kodehawa.mantarobot.commands.image.ImageboardUtils.getImage;
import static net.kodehawa.mantarobot.commands.image.ImageboardUtils.nsfwCheck;

@Module
public class ImageCmds {

    private final URLCache CACHE = new URLCache(20);
    private final String[] catResponses = {
            "Aww, here, take a cat.", "%mention%, are you sad? ;w; take a cat!", "You should all have a cat in your life, but an image will do.",
            "Am I cute yet?", "I think you should have a cat, %mention%."
    };
    private final ImageBoard<DanbooruImage> danbooru = DefaultImageBoards.DANBOORU;
    private final ImageBoard<FurryImage> e621 = DefaultImageBoards.E621;
    private final ImageBoard<KonachanImage> konachan = DefaultImageBoards.KONACHAN;
    private final ImageBoard<Rule34Image> rule34 = DefaultImageBoards.RULE34;
    private final ImageBoard<SafebooruImage> safebooru = DefaultImageBoards.SAFEBOORU; //safebooru.org, not the danbooru one.
    private final ImageBoard<YandereImage> yandere = DefaultImageBoards.YANDERE;

    @Subscribe
    public void cat(CommandRegistry cr) {
        cr.register("cat", new SimpleCommand(Category.IMAGE) {
            final OkHttpClient httpClient = new OkHttpClient();

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                try {
                    Request r = new Request.Builder()
                            .url("http://random.cat/meow")
                            .build();

                    Response response = httpClient.newCall(r).execute();

                    String url = new JSONObject(response.body().string()).getString("file");
                    response.close();
                    event.getChannel().sendFile(CACHE.getFile(url), "cat.jpg",
                            new MessageBuilder().append(EmoteReference.TALKING).append(
                                    CollectionUtils.random(catResponses).replace("%mention%", event.getAuthor().getName())
                            ).build()).queue();
                } catch(Exception e) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Error retrieving cute cat images :<").queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Cat command")
                        .setDescription("Sends a random cat image.")
                        .build();
            }
        });
    }

    @Subscribe
    public void catgirls(CommandRegistry cr) {
        cr.register("catgirl", new SimpleCommand(Category.IMAGE) {

            final WeebAPIRequester requester = new WeebAPIRequester();

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");

                if(nsfw && !nsfwCheck(event, languageContext, true, true, null))
                    return;

                try {
                    Pair<String, String> result = requester.getRandomImageByType("neko", nsfw, null);
                    String image = result.getKey();

                    if(image == null) {
                        event.getChannel().sendMessage("Unable to get image.").queue();
                        return;
                    }

                    event.getChannel().sendFile(CACHE.getInput(image), "catgirl-" + result.getValue() + ".png", null).queue();
                } catch(Exception e) {
                    event.getChannel().sendMessage("Unable to get image.").queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Catgirl command")
                        .setDescription("**Sends catgirl images**")
                        .addField("Usage", "`~>catgirl` - **Sends a catgirl image.**" +
                                "\n`~>catgirl nsfw` - **Sends a lewd or questionable catgirl image.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void e621(CommandRegistry cr) {
        cr.register("e621", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(e621, ImageRequestType.GET, true, "e621", args, content, event, languageContext);
                        break;
                    case "tags":
                        getImage(e621, ImageRequestType.TAGS, true, "e621", args, content, event, languageContext);
                        break;
                    case "":
                        getImage(e621, ImageRequestType.RANDOM, true, "e621", args, content, event, languageContext);
                        break;
                    default:
                        onHelp(event);
                        break;
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "e621 commmand")
                        .setColor(Color.PINK)
                        .setDescription("**Retrieves images from the e621 (furry) image board.**")
                        .addField("Usage",
                                "`~>e621` - **Gets you a completely random image.**\n"
                                        + "`~>e621 get <imagenumber>` - **Gets you an image with the specified parameters.**\n"
                                        + "`~>e621 tags <tag>` - **Gets you an image with the respective tag and specified parameters.**\n\n"
                                        + "**WARNING**: This command can be only used in NSFW channels!", false)
                        .addField("Parameters",
                                "`tag` - **Any valid image tag. For example animal_ears or yuri. (only one tag, spaces are separated by underscores)**\n", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void kona(CommandRegistry cr) {
        cr.register("konachan", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(konachan, ImageRequestType.GET, false, "konachan", args, content, event, languageContext);
                        break;
                    case "tags":
                        getImage(konachan, ImageRequestType.TAGS, false, "konachan", args, content, event, languageContext);
                        break;
                    case "":
                        getImage(konachan, ImageRequestType.RANDOM, false, "konachan", args, content, event, languageContext);
                        break;
                    default:
                        onHelp(event);
                        break;
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Konachan commmand")
                        .setColor(Color.PINK)
                        .setDescription("**Retrieves images from the Konachan image board.**")
                        .addField("Usage",
                                "`~>konachan` - **Gets you a completely random image.**\n"
                                        + "`~>konachan get <imagenumber> <rating>` - **Gets you an image with the specified parameters.**\n"
                                        + "`~>konachan tags <tag> <rating>` - **Gets you an image with the respective tag and specified parameters.**\n\n"
                                        + "**WARNING**: If the rating is explicit/questionable this command can be only used in NSFW channels! (Unless rating has been specified as safe or not specified at all)", false)
                        .addField("Parameters",
                                "`tag` - **Any valid image tag. For example animal_ears or yuri. (only one tag, spaces are separated by underscores)**\n"
                                        + "`rating` - **(OPTIONAL) Can be either safe, questionable or explicit, depends on the type of image you want to get.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void safebooru(CommandRegistry cr) {
        cr.register("safebooru", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(safebooru, ImageRequestType.GET, false, "safebooru", args, content, event, languageContext);
                        break;
                    case "tags":
                        getImage(safebooru, ImageRequestType.TAGS, false, "safebooru", args, content, event, languageContext);
                        break;
                    case "":
                        getImage(safebooru, ImageRequestType.RANDOM, false, "safebooru", args, content, event, languageContext);
                        break;
                    default:
                        onHelp(event);
                        break;
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Safebooru commmand")
                        .setColor(Color.PINK)
                        .setDescription("**Retrieves images from the Safebooru image board.**")
                        .addField("Usage",
                                "`~>safebooru` - **Gets you a completely random image.**\n"
                                        + "`~>safebooru get <imagenumber>` - **Gets you an image with the specified parameters.**\n"
                                        + "`~>safebooru tags <tag>` - **Gets you an image with the respective tag and specified parameters.**\n\n", false)
                        .addField("Parameters",
                                "`tag` - **Any valid image tag. For example animal_ears or yuri. (only one tag, spaces are separated by underscores)**\n", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void danbooru(CommandRegistry cr) {
        cr.register("danbooru", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(danbooru, ImageRequestType.GET, false, "danbooru", args, content, event, languageContext);
                        break;
                    case "tags":
                        getImage(danbooru, ImageRequestType.TAGS, false, "danbooru", args, content, event, languageContext);
                        break;
                    case "":
                        getImage(danbooru, ImageRequestType.RANDOM, false, "danbooru", args, content, event, languageContext);
                        break;
                    default:
                        onHelp(event);
                        break;
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Danbooru commmand")
                        .setColor(Color.PINK)
                        .setDescription("**Retrieves images from the danbooru image board.**")
                        .addField("Usage",
                                "`~>danbooru` - **Gets you a completely random image.**\n"
                                        + "`~>danbooru get <imagenumber> <rating>` - **Gets you an image with the specified parameters.**\n"
                                        + "`~>danbooru tags <tag> <rating>` - **Gets you an image with the respective tag and specified parameters.**\n\n"
                                        + "**WARNING**: If rating is explicit/questionable, the command can be only used in NSFW channels! (Unless rating has been specified as safe or not specified at all)", false)
                        .addField("Parameters",
                                "`tag` - **Any valid image tag. For example animal_ears or yuri. (only one tag, spaces are separated by underscores)**\n"
                                        + "`rating` - **(OPTIONAL) Can be either safe, questionable or explicit, depends on the type of image you want to get.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void rule34(CommandRegistry cr) {
        cr.register("rule34", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(rule34, ImageRequestType.GET, true, "rule34", args, content, event, languageContext);
                        break;
                    case "tags":
                        getImage(rule34, ImageRequestType.TAGS, true, "rule34", args, content, event, languageContext);
                        break;
                    case "":
                        getImage(rule34, ImageRequestType.RANDOM, true, "rule34", args, content, event, languageContext);
                        break;
                    default:
                        onHelp(event);
                        break;
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "rule34.xxx commmand")
                        .setColor(Color.PINK)
                        .setDescription("**Retrieves images from the rule34 (hentai) image board.**")
                        .addField("Usage",
                                "`~>rule34` - **Gets you a completely random image.**\n"
                                        + "`~>rule34 get <imagenumber>` - **Gets you an image with the specified parameters.**\n"
                                        + "`~>rule34 tags <tag>` - **Gets you an image with the respective tag and specified parameters.**\n\n"
                                        + "**WARNING**: This command can be only used in NSFW channels!", false)
                        .addField("Parameters",
                                "`tag` - **Any valid image tag. For example animal_ears or yuri. (only one tag, spaces are separated by underscores)**\n", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void yandere(CommandRegistry cr) {
        cr.register("yandere", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!event.getChannel().isNSFW()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.imageboard.yandere_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(yandere, ImageRequestType.GET, false, "yandere", args, content, event, languageContext);
                        break;
                    case "tags":
                        getImage(yandere, ImageRequestType.TAGS, false, "yandere", args, content, event, languageContext);
                        break;
                    case "":
                        getImage(yandere, ImageRequestType.RANDOM, false, "yandere", args, content, event, languageContext);
                        break;
                    default:
                        onHelp(event);
                        break;
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Yande.re command")
                        .setColor(Color.DARK_GRAY)
                        .setDescription("**This command fetches images from the image board yande.re. Normally used to store NSFW images, "
                                + "but tags can be set to safe if you so desire.**")
                        .addField("Usage",
                                "`~>yandere` - **Gets you a completely random image.**\n"
                                        + "`~>yandere get <imagenumber> <rating>` - **Gets you an image with the specified parameters.**\n"
                                        + "`~>yandere tags <tag> <rating>` - **Gets you an image with the respective tag and specified parameters.**\n\n"
                                        + "**WARNING**: This command can be only used in NSFW channels! (Unless rating has been specified as safe or not specified at all)", false)
                        .addField("Parameters",
                                    "`tag` - **Any valid image tag. For example animal_ears or yuri. (only one tag, spaces are separated by underscores)**\n"
                                        + "`rating` - **(OPTIONAL) Can be either safe, questionable or explicit, depends on the type of image you want to get.**", false)
                        .build();
            }
        });
    }
}
