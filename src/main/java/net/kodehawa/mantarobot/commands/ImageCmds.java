/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.image.ImageRequestType;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.awt.*;

import static net.kodehawa.mantarobot.commands.image.ImageboardUtils.getImage;
import static net.kodehawa.mantarobot.commands.image.ImageboardUtils.nsfwCheck;

@Module
public class ImageCmds {

    private final String[] catResponses = {
            "Aww, take a cat.", "%mention%, are you sad? ;w;, take a cat!", "You should all have a cat in your life, but a image will do.",
            "Am I cute yet?", "%mention%, I think you should have a cat."
    };

    private final URLCache CACHE = new URLCache(20);

    private final ImageBoard<FurryImage> e621 = DefaultImageBoards.E621;
    private final ImageBoard<KonachanImage> konachan = DefaultImageBoards.KONACHAN;
    private final ImageBoard<Rule34Image> rule34 = DefaultImageBoards.RULE34;
    private final ImageBoard<YandereImage> yandere = DefaultImageBoards.YANDERE;
    private final ImageBoard<DanbooruImage> danbooru = DefaultImageBoards.DANBOORU;
    private final ImageBoard<SafebooruImage> safebooru = DefaultImageBoards.SAFEBOORU; //safebooru.org, not the danbooru one.

    @Subscribe
    public void cat(CommandRegistry cr) {
        cr.register("cat", new SimpleCommand(Category.IMAGE) {
            final OkHttpClient httpClient = new OkHttpClient();

            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                try {
                    Request r = new Request.Builder()
                            .url("http://random.cat/meow")
                            .build();

                    Response response = httpClient.newCall(r).execute();

                    String url = new JSONObject(response.body().string()).getString("file");
                    response.close();
                    event.getChannel().sendFile(CACHE.getFile(url), "cat.jpg",
                            new MessageBuilder().append(EmoteReference.TALKING).append(CollectionUtils.random(catResponses).replace("%mention%", event.getAuthor().getName())).build()).queue();
                } catch(Exception e) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Error retrieving cute cat images :<").queue();
                    e.printStackTrace();
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
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");
                if(nsfw && !nsfwCheck(event, true, true, null)) return;

                try {
                    String image = requester.getRandomImageByType("neko", nsfw, null);

                    if(image == null) {
                        event.getChannel().sendMessage("Unable to get image.").queue();
                        return;
                    }

                    event.getChannel().sendFile(CACHE.getInput(image), "catgirl.png", null).queue();
                } catch(Exception e) {
                    e.printStackTrace();
                    event.getChannel().sendMessage("Unable to get image.").queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Catgirl command")
                        .setDescription("**Sends catgirl images**")
                        .addField("Usage", "`~>catgirl` - **Returns catgirl images.**" +
                                "\n`~>catgirl nsfw` - **Returns lewd or questionable cargirl images.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void e621(CommandRegistry cr) {
        cr.register("e621", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(e621, ImageRequestType.GET, true, "e621", args, content, event);
                        break;
                    case "tags":
                        getImage(e621, ImageRequestType.TAGS, true, "e621", args, content, event);
                        break;
                    case "":
                        getImage(e621, ImageRequestType.RANDOM, true, "e621", args, content, event);
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
                                "`~>e621 get <imagenumber>` - **Gets an image based in parameters.**\n"
                                        + "`~>e621 tags <tag> <imagenumber>` - **Gets an image based in the specified tag and parameters.**", false)
                        .addField("Parameters",
                                "`page` - **Can be any value from 1 to the e621 maximum page. Probably around 4000.**\n"
                                        + "`imagenumber` - **(OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.**\n"
                                        + "`tag` - **Any valid image tag. For example animal_ears or original.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void kona(CommandRegistry cr) {
        cr.register("konachan", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(konachan, ImageRequestType.GET, false, "konachan", args, content, event);
                        break;
                    case "tags":
                        getImage(konachan, ImageRequestType.TAGS, false, "konachan", args, content, event);
                        break;
                    case "":
                        getImage(konachan, ImageRequestType.RANDOM, false, "konachan", args, content, event);
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
                                "`~>konachan get <page> <imagenumber>` - **Gets an image based in parameters.**\n"
                                        + "`~>konachan tags <tag> <imagenumber>` - **Gets an image based in the specified tag and parameters.**\n", false)
                        .addField("Parameters",
                                "`page` - **Can be any value from 1 to the Konachan maximum page. Probably around 4000.**\n"
                                        + "`imagenumber` - **(OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.**\n"
                                        + "`tag` - **Any valid image tag. For example animal_ears or original.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void safebooru(CommandRegistry cr) {
        cr.register("safebooru", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(safebooru, ImageRequestType.GET, false, "safebooru", args, content, event);
                        break;
                    case "tags":
                        getImage(safebooru, ImageRequestType.TAGS, false, "safebooru", args, content, event);
                        break;
                    case "":
                        getImage(safebooru, ImageRequestType.RANDOM, false, "safebooru", args, content, event);
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
                                "`~>safebooru get <page> <imagenumber>` - **Gets an image based in parameters.**\n"
                                        + "`~>safebooru tags <tag> <imagenumber>` - **Gets an image based in the specified tag and parameters.**\n", false)
                        .addField("Parameters",
                                "`page` - **Can be any value from 1 to the Safebooru maximum page. Probably around 4000.**\n"
                                        + "`imagenumber` - **(OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.**\n"
                                        + "`tag` - **Any valid image tag. For example animal_ears or original.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void danbooru(CommandRegistry cr) {
        cr.register("danbooru", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(danbooru, ImageRequestType.GET, false, "danbooru", args, content, event);
                        break;
                    case "tags":
                        getImage(danbooru, ImageRequestType.TAGS, false, "danbooru", args, content, event);
                        break;
                    case "":
                        getImage(danbooru, ImageRequestType.RANDOM, false, "danbooru", args, content, event);
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
                                "`~>danbooru get <page> <imagenumber>` - **Gets an image based in parameters.**\n"
                                        + "`~>danbooru tags <tag> <imagenumber>` - **Gets an image based in the specified tag and parameters.**\n", false)
                        .addField("Parameters",
                                "`page` - **Can be any value from 1 to the danbooru maximum page. Probably around 4000.**\n"
                                        + "`imagenumber` - **(OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.**\n"
                                        + "`tag` - **Any valid image tag. For example animal_ears or original.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void rule34(CommandRegistry cr) {
        cr.register("rule34", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(rule34, ImageRequestType.GET, true, "rule34", args, content, event);
                        break;
                    case "tags":
                        getImage(rule34, ImageRequestType.TAGS, true, "rule34", args, content, event);
                        break;
                    case "":
                        getImage(rule34, ImageRequestType.RANDOM, true, "rule34", args, content, event);
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
                        .addField("Usage", "`~>rule34 get <imagenumber>` - **Gets an image based in parameters.**\n"
                                + "`~>rule34 tags <tag> <imagenumber>` - **Gets an image based in the specified tag and parameters.**\n", false)
                        .addField("Parameters", "`page` - **Can be any value from 1 to the rule34 maximum page. Probably around 4000.**\n"
                                + "`imagenumber` - **(OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.**\n"
                                + "`tag` - **Any valid image tag. For example animal_ears or original.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void yandere(CommandRegistry cr) {
        cr.register("yandere", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(yandere, ImageRequestType.GET, false, "yandere", args, content, event);
                        break;
                    case "tags":
                        getImage(yandere, ImageRequestType.TAGS, false, "yandere", args, content, event);
                        break;
                    case "":
                        getImage(yandere, ImageRequestType.RANDOM, false, "yandere", args, content, event);
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
                                        + "`~>yandere get <page> <rating> <imagenumber>` - **Gets you an image with the specified parameters.**\n"
                                        + "`~>yandere tags <tag> <rating> <imagenumber>` - **Gets you an image with the respective tag and specified parameters.**\n\n"
                                        + "**WARNING**: This command can be only used in NSFW channels! (Unless rating has been specified as safe or not specified at all)", false)
                        .addField("Parameters",
                                "`imagenumber` - **(OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.**\n"
                                        + "`tag` - **Any valid image tag. For example animal_ears or yuri. (only one tag, spaces are separated by underscores)**\n"
                                        + "`rating` - **(OPTIONAL) Can be either safe, questionable or explicit, depends on the type of image you want to get.**", false)
                        .build();
            }
        });
    }
}
