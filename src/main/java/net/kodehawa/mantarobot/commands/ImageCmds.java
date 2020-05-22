/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.MessageBuilder;
import net.kodehawa.lib.imageboards.DefaultImageBoards;
import net.kodehawa.lib.imageboards.ImageBoard;
import net.kodehawa.lib.imageboards.entities.impl.*;
import net.kodehawa.mantarobot.commands.action.WeebAPIRequester;
import net.kodehawa.mantarobot.commands.image.ImageRequestType;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Random;

import static net.kodehawa.mantarobot.commands.image.ImageboardUtils.getImage;
import static net.kodehawa.mantarobot.commands.image.ImageboardUtils.nsfwCheck;

@Module
@SuppressWarnings("unused")
public class ImageCmds {
    private final URLCache CACHE = new URLCache(20);
    private final String[] catResponses = {
            "Aww, here, take a cat.", "%mention%, are you sad? ;w; take a cat!", "You should all have a cat in your life, but an image will do.",
            "Am I cute yet?", "I think you should have a cat, %mention%.", "Meow~ %mention%", "Nya~ %mention%"
    };
    private final ImageBoard<DanbooruImage> danbooru = DefaultImageBoards.DANBOORU;
    private final ImageBoard<FurryImage> e621 = DefaultImageBoards.E621;
    private final ImageBoard<KonachanImage> konachan = DefaultImageBoards.KONACHAN;
    private final ImageBoard<Rule34Image> rule34 = DefaultImageBoards.RULE34;
    private final ImageBoard<SafebooruImage> safebooru = DefaultImageBoards.SAFEBOORU; //safebooru.org, not the danbooru one.
    private final ImageBoard<YandereImage> yandere = DefaultImageBoards.YANDERE;
    private final ImageBoard<GelbooruImage> gelbooru = DefaultImageBoards.GELBOORU;
    private final WeebAPIRequester weebAPIRequester = new WeebAPIRequester();
    private final Random random = new Random();

    @Subscribe
    public void cat(CommandRegistry cr) {
        cr.register("cat", new SimpleCommand(Category.IMAGE) {
            final OkHttpClient httpClient = new OkHttpClient();

            @Override
            protected void call(Context ctx, String content, String[] args) {
                try {
                    Pair<String, String> result = weebAPIRequester.getRandomImageByType("animal_cat", false, null);
                    String url = result.getKey();
                    ctx.getChannel().sendMessage(
                            new MessageBuilder().append(EmoteReference.TALKING).append(
                                    catResponses[random.nextInt(catResponses.length)].replace("%mention%", ctx.getAuthor().getName()))
                                    .build()
                    ).addFile(CACHE.getFile(url), "cat-" + result.getValue() + ".png")
                            .queue();
                } catch (Exception e) {
                    ctx.sendLocalized("commands.imageboard.cat.error", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Sends a random cat image. Really cute stuff, you know?")
                        .build();
            }
        });
    }

    @Subscribe
    public void catgirls(CommandRegistry cr) {
        cr.register("catgirl", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");

                if (nsfw && !nsfwCheck(ctx, true, true, null))
                    return;

                try {
                    Pair<String, String> result = weebAPIRequester.getRandomImageByType("neko", nsfw, null);
                    String image = result.getKey();

                    if (image == null) {
                        ctx.sendLocalized("commands.imageboard.catgirl.error");
                        return;
                    }

                    ctx.getChannel().sendFile(CACHE.getInput(image), "catgirl-" + result.getValue() + ".png").queue();
                } catch (Exception e) {
                    ctx.sendLocalized("commands.imageboard.catgirl.error");
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Sends images of catgirl(s). Maybe.")
                        .setUsage("`~>catgirl` - Sends images of normal catgirls.\n" +
                                "\"`~>catgirl nsfw` - Sends images of lewd catgirls. (Only works on NSFW channels)")
                        .build();
            }
        });
    }

    @Subscribe
    public void e621(CommandRegistry cr) {
        cr.register("e621", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getChannel().isNSFW()) {
                    ctx.sendLocalized("commands.imageboard.e621_nsfw_notice", EmoteReference.ERROR);
                    return;
                }

                sendImage(ctx, e621, true, "e621", content, args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves images from the e621 (furry) image board. (Why is the IB name so unrelated?).\n" +
                                "This command can be only used in NSFW channels.")
                        .setUsage("`~>e621` - Retrieves a random image.\n" +
                                "`~>e621 <tag>` - Fetches an image with the respective tag and specified parameters.")
                        .addParameter("tag", "The image tag you're looking for. You can see a list of valid tags on e621's website (NSFW).")
                        .build();
            }
        });
    }

    @Subscribe
    public void kona(CommandRegistry cr) {
        cr.register("konachan", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                sendImage(ctx, konachan, false, "konachan", content, args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves images from the Konachan image board.\n" +
                                "If the rating is explicit/questionable this command can be only used in NSFW channels.")
                        .setUsage("`~>konachan` - Retrieves a random image.\n" +
                                "`~>konachan <tag> <rating>` - Fetches an image with the respective tag and specified parameters.")
                        .addParameter("tag", "The image tag you're looking for. You can see a list of valid tags on konachan's website.")
                        .addParameter("rating", "The image rating, can be either safe, questionable or explicit. You can also use this in place of the tags.")
                        .build();
            }
        });
    }

    @Subscribe
    public void safebooru(CommandRegistry cr) {
        cr.register("safebooru", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                sendImage(ctx, safebooru, false, "safebooru", content, args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves images from the Safebooru image board.")
                        .setUsage("`~>safebooru` - Retrieves a random image.\n" +
                                "`~>safebooru <tag>` - Fetches an image with the respective tag and specified parameters.")
                        .addParameter("tag", "The image tag you're looking for. You can see a list of valid tags on safebooru's website.")
                        .build();
            }
        });
    }

    @Subscribe
    public void danbooru(CommandRegistry cr) {
        cr.register("danbooru", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                sendImage(ctx, danbooru, false, "danbooru", content, args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves images from the Danbooru image board.\n" +
                                "If the rating is explicit/questionable this command can be only used in NSFW channels.")
                        .setUsage("`~>danbooru` - Retrieves a random image.\n" +
                                "`~>danbooru <tag> <rating>` - Fetches an image with the respective tag and specified parameters.")
                        .addParameter("tag", "The image tag you're looking for. You can see a list of valid tags on danbooru's website.")
                        .addParameter("rating", "The image rating, can be either safe, questionable or explicit. You can also use this in place of the tags.")
                        .build();
            }
        });
    }

    @Subscribe
    public void rule34(CommandRegistry cr) {
        cr.register("rule34", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                sendImage(ctx, rule34, true, "rule34", content, args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves images from the Danbooru image board.\n" +
                                "This command only works in NSFW channels. You could guess it from the name though ;)")
                        .setUsage("`~>rule34` - Retrieves a random image.\n" +
                                "`~>rule34 <tag>` - Fetches an image with the respective tag and specified parameters.")
                        .addParameter("tag", "The image tag you're looking for. You can see a list of valid tags on rule34's website (NSFW).")
                        .build();
            }
        });
    }

    @Subscribe
    public void yandere(CommandRegistry cr) {
        cr.register("yandere", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getChannel().isNSFW()) {
                    ctx.sendLocalized("commands.imageboard.yandere_nsfw_notice", EmoteReference.ERROR);
                    return;
                }

                sendImage(ctx, yandere, false, "yandere", content, args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves images from the Yande.re image board.\n" +
                                "This command only works on NSFW channels, regarding of rating " +
                                "(because of course the maintainers think really harsh sexual acts qualify as enough to give it a safe rating I mean, sure).")
                        .setUsage("`~>yandere` - Retrieves a random image.\n" +
                                "`~>yandere <tag> <rating>` - Fetches an image with the respective tag and specified parameters.")
                        .addParameter("tag", "The image tag you're looking for. You can see a list of valid tags on yande.re's website.")
                        .addParameter("rating", "The image rating, can be either safe, questionable or explicit. You can also use this in place of the tags.")
                        .build();
            }
        });
    }

    @Subscribe
    public void gelbooru(CommandRegistry cr) {
        cr.register("gelbooru", new SimpleCommand(Category.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getChannel().isNSFW()) {
                    ctx.sendLocalized("commands.imageboard.yandere_nsfw_notice", EmoteReference.ERROR);
                    return;
                }

                sendImage(ctx, gelbooru, false, "gelbooru", content, args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves images from the Gelbooru image board.\n" +
                                "This command only works on NSFW channels, regarding of rating " +
                                "(because we're not sure if it'll really put safe images all the time, rating is still left to the user).")
                        .setUsage("`~>gelbooru` - Retrieves a random image.\n" +
                                "`~>gelbooru <tag> <rating>` - Fetches an image with the respective tag and specified parameters.")
                        .addParameter("tag", "The image tag you're looking for. You can see a list of valid tags on gelbooru's website.")
                        .addParameter("rating", "The image rating, can be either safe, questionable or explicit. You can also use this in place of the tags.")
                        .build();
            }
        });
    }

    private void sendImage(Context ctx, ImageBoard<?> image, boolean nsfwOnly, String name, String content, String[] args) {
        String firstArg = args.length == 0 ? "" : args[0];
        if(firstArg.isEmpty() || firstArg.equalsIgnoreCase("random"))
            getImage(image, ImageRequestType.RANDOM, nsfwOnly, name, args, content, ctx);
        else
            getImage(image, ImageRequestType.TAGS, nsfwOnly, name, args, content, ctx);
    }
}
