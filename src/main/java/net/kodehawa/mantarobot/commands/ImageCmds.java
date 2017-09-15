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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.imageboards.ImageboardAPI;
import net.kodehawa.lib.imageboards.entities.*;
import net.kodehawa.lib.imageboards.util.Imageboards;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.events.PostLoadEvent;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.json.JSONObject;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Module
public class ImageCmds {

    private final String[] responses = {
            "Aww, take a cat.", "%mention%, are you sad? ;w;, take a cat!", "You should all have a cat in your life, but a image will do.",
            "Am I cute yet?", "%mention%, I think you should have a cat."
    };

    private final URLCache CACHE = new URLCache(20);

    private final String BASEURL = "http://catgirls.brussell98.tk/api/random";
    private final String NSFWURL = "http://catgirls.brussell98.tk/api/nsfw/random"; //this actually returns more questionable images than explicit tho

    private final BidiMap<String, String> nRating = new DualHashBidiMap<>();
    private final Random r = new Random();

    private final ImageboardAPI<FurryImage> e621 = Imageboards.E621;
    private final ImageboardAPI<KonachanImage> konachan = Imageboards.KONACHAN;
    private final ImageboardAPI<Rule34Image> rule34 = Imageboards.RULE34;
    private final ImageboardAPI<YandereImage> yandere = Imageboards.YANDERE;
    private final ImageboardAPI<DanbooruImage> danbooru = Imageboards.DANBOORU;

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
                            new MessageBuilder().append(CollectionUtils.random(responses).replace("%mention%", event.getAuthor().getAsMention())).build()).queue();
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
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");
                if(nsfw && !nsfwCheck(event, true, true, null)) return;

                try {
                    JSONObject obj = new JSONObject(Utils.wgetResty(nsfw ? NSFWURL : BASEURL, event));
                    if(!obj.has("url")) {
                        event.getChannel().sendMessage("Unable to find image.").queue();
                    } else {
                        event.getChannel().sendFile(CACHE.getInput(obj.getString("url")), "catgirl.png", null).queue();
                    }
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
                                "\n´`~>catgirl nsfw` - **Returns lewd or questionable cargirl images.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void e621(CommandRegistry cr) {
        cr.register("e621", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                TextChannelGround.of(event).dropItemWithChance(13, 3);
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(e621, "get", true, "e621", args, content, event);
                        break;
                    case "tags":
                        getImage(e621, "tags", true, "e621", args, content, event);
                        break;
                    case "":
                        getImage(e621, "", true, "e621", args, content, event);
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
                        getImage(konachan, "get", false, "konachan", args, content, event);
                        break;
                    case "tags":
                        getImage(konachan, "tags", false, "konachan", args, content, event);
                        break;
                    case "":
                        getImage(konachan, "", false, "konachan", args, content, event);
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
    public void danbooru(CommandRegistry cr) {
        cr.register("danbooru", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "get":
                        getImage(danbooru, "get", false, "danbooru", args, content, event);
                        break;
                    case "tags":
                        getImage(danbooru, "tags", false, "danbooru", args, content, event);
                        break;
                    case "":
                        getImage(danbooru, "", false, "danbooru", args, content, event);
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
                TextChannelGround.of(event).dropItemWithChance(13, 3);
                switch(noArgs) {
                    case "get":
                        getImage(rule34, "get", true, "rule34", args, content, event);
                        break;
                    case "tags":
                        getImage(rule34, "tags", true, "rule34", args, content, event);
                        break;
                    case "":
                        getImage(rule34, "", true, "rule34", args, content, event);
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
                        getImage(yandere, "get", false, "yandere", args, content, event);
                        break;
                    case "tags":
                        getImage(yandere, "tags", false, "yandere", args, content, event);
                        break;
                    case "":
                        getImage(yandere, "", false, "yandere", args, content, event);
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

    private boolean nsfwCheck(GuildMessageReceivedEvent event, boolean isGlobal, boolean sendMessage, String rating) {
        if(event.getChannel().isNSFW()) return true;

        String nsfwChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildUnsafeChannels().stream()
                .filter(channel -> channel.equals(event.getChannel().getId())).findFirst().orElse(null);
        String rating1 = rating == null ? "s" : rating;
        boolean trigger = !isGlobal ? ((rating1.equals("s") || (nsfwChannel == null)) ? rating1.equals("s") : nsfwChannel.equals(event.getChannel().getId())) :
                nsfwChannel != null && nsfwChannel.equals(event.getChannel().getId());

        if(!trigger) {
            if(sendMessage) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "Not on a NSFW channel. Cannot send lewd images.\n" +
                        "**Reminder:** You can set this channel as NSFW by doing `~>opts nsfw toggle` if you are an administrator on this server.").queue();
            }
            return false;
        }

        return true;
    }

    private boolean foundMinorTags(GuildMessageReceivedEvent event, String tags, String rating) {
        boolean trigger = tags.contains("loli") || tags.contains("lolis") ||
                tags.contains("shota") || tags.contains("shotas") ||
                tags.contains("lolicon") || tags.contains("shotacon") &&
                (rating == null || rating.equals("q") || rating.equals("e"));

        if(!trigger) {
            return false;
        }

        event.getChannel().sendMessage(EmoteReference.WARNING + "Sadly we cannot display images that allegedly contain `loli` or `shota` lewd/NSFW content because discord" +
                " prohibits it. (Filter ran: Image contains a loli or shota tag and it's NSFW)").queue();
        return true;
    }

    private boolean boom(List<?> l, GuildMessageReceivedEvent event) {
        if(l == null) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Oops... something went wrong when searching...").queue();
            return true;
        }

        return false;
    }

    private void getImage(ImageboardAPI<?> api, String type, boolean nsfwOnly, String imageboard, String[] args, String content, GuildMessageReceivedEvent event) {
        String rating = "s";
        boolean needRating = args.length >= 3;
        TextChannel channel = event.getChannel();

        if(nsfwOnly)
            rating = "e";

        try {
            if(needRating) rating = nRating.get(args[2]);
        } catch(Exception e) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "You provided an invalid rating (Avaliable types: questionable, explicit, safe)!").queue();
            return;
        }

        final String fRating = rating;

        if(!nsfwCheck(event, false, false, fRating)) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot send a NSFW image in a non-nsfw channel :(").queue();
            return;
        }
        int page = Math.max(1, r.nextInt(25));

        switch(type) {
            case "get":
                try {
                    String whole1 = content.replace("get ", "");
                    String[] wholeBeheaded = whole1.split(" ");

                    api.get(page, images1 -> {
                        if(boom(images1, event)) return;

                        try {
                            int number;
                            List<BoardImage> images = (List<BoardImage>) images1;
                            if(!nsfwOnly)
                                images = images1.stream().filter(data -> data.getRating().equals(fRating)).collect(Collectors.toList());

                            try {
                                number = Integer.parseInt(wholeBeheaded[0]);
                            } catch(Exception e) {
                                number = r.nextInt(images.size());
                            }
                            BoardImage image = images.get(number);
                            String tags = image.getTags().stream().collect(Collectors.joining(", "));
                            if(foundMinorTags(event, tags, image.getRating())) {
                                return;
                            }

                            imageEmbed(image.getImageUrl(), String.valueOf(image.getWidth()), String.valueOf(image.getHeight()), tags, fRating, imageboard, channel);
                            TextChannelGround.of(event).dropItemWithChance(13, 3);
                        } catch(Exception e) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "**There aren't any more images or no results found**! Please try with a lower " +
                                    "number or another search.").queue();
                        }
                    });
                } catch(Exception exception) {
                    if(exception instanceof NumberFormatException)
                        channel.sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help " + imageboard).queue(
                                message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
                        );
                }
                break;
            case "tags":
                try {
                    String sNoArgs = content.replace("tags ", "");
                    String[] expectedNumber = sNoArgs.split(" ");
                    String tags = expectedNumber[0];
                    api.onSearch(tags, images -> {
                        //account for this
                        if(boom(images, event)) return;

                        try {
                            List<BoardImage> filter = (List<BoardImage>) images;
                            if(!nsfwOnly)
                                filter = images.stream().filter(data -> data.getRating().equals(fRating)).collect(Collectors.toList());

                            int number1;
                            try {
                                number1 = Integer.parseInt(expectedNumber[1]);
                            } catch(Exception e) {
                                number1 = r.nextInt(filter.size() > 0 ? filter.size() - 1 : filter.size());
                            }
                            BoardImage image = filter.get(number1);
                            String tags1 = image.getTags().stream().collect(Collectors.joining(", "));

                            if(foundMinorTags(event, tags1, image.getRating())) {
                                return;
                            }

                            imageEmbed(image.getImageUrl(), String.valueOf(image.getWidth()), String.valueOf(image.getHeight()), tags1, fRating, imageboard, channel);
                            TextChannelGround.of(event).dropItemWithChance(13, 3);
                        } catch(Exception e) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "**There aren't any more images or no results found**! Please try with a lower " +
                                    "number or another search.").queue();
                        }
                    });
                } catch(Exception exception) {
                    if(exception instanceof NumberFormatException)
                        channel.sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help " + imageboard).queue(
                                message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
                        );

                    exception.printStackTrace();
                }
                break;
            case "":
                api.get(page, images -> {
                    if(boom(images, event)) return;

                    List<BoardImage> filter = (List<BoardImage>) images;
                    if(!nsfwOnly)
                        filter = images.stream().filter(data -> data.getRating().equals(fRating)).collect(Collectors.toList());

                    int number = r.nextInt(filter.size());
                    BoardImage image = filter.get(number);
                    String tags1 = image.getTags().stream().collect(Collectors.joining(", "));
                    imageEmbed(image.getImageUrl(), String.valueOf(image.getWidth()), String.valueOf(image.getHeight()), tags1, fRating, imageboard, channel);
                    TextChannelGround.of(event).dropItemWithChance(13, 3);
                });
                break;
        }
    }

    private void imageEmbed(String url, String width, String height, String tags, String rating, String imageboard, TextChannel channel) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor("Found image", url, null)
                .setImage(url)
                .setDescription("Rating: **" + nRating.getKey(rating) + "**, Imageboard: **" + imageboard + "**" )
                .addField("Width", width, true)
                .addField("Height", height, true)
                .addField("Tags", "`" + (tags == null ? "None" : tags) + "`", false)
                .setFooter("If the image doesn't load, click the title.", null);

        channel.sendMessage(builder.build()).queue();
    }

    @Subscribe
    public void onPostLoad(PostLoadEvent e) {
        nRating.put("safe", "s");
        nRating.put("questionable", "q");
        nRating.put("explicit", "e");
    }
}
