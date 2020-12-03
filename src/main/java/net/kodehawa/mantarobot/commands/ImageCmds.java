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
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Random;

import static net.kodehawa.mantarobot.commands.image.ImageboardUtils.getImage;
import static net.kodehawa.mantarobot.commands.image.ImageboardUtils.nsfwCheck;

@Module
public class ImageCmds {
    private static final URLCache imageCache = new URLCache(20);
    private static final String[] catResponses = {
            "Aww, here, take a cat.", "%mention%, are you sad? ;w; take a cat!",
            "You should all have a cat in your life, but an image will do.",
            "Am I cute yet?", "I think you should have a cat, %mention%.",
            "Meow~ %mention%", "Nya~ %mention%"
    };

    // I basically repeated those two all the time, so might aswell just
    // make them constants instead.
    private static final String RATING_HELP = """
                                        The image rating, can be either safe, questionable or explicit.
                                        You can also use this in place of the tags.
                                        Rating can be random if you specify it as random, in case you want to play a roulette.
                                        """;

    private static final String TAG_HELP = "The image tag you're looking for. You can see a list of valid tags on the %s website.";

    private static final ImageBoard<DanbooruImage> danbooru = DefaultImageBoards.DANBOORU;
    private static final ImageBoard<FurryImage> e621 = DefaultImageBoards.E621;
    private static final ImageBoard<SafeFurryImage> e926 = DefaultImageBoards.E926;
    private static final ImageBoard<KonachanImage> konachan = DefaultImageBoards.KONACHAN;
    private static final ImageBoard<Rule34Image> rule34 = DefaultImageBoards.RULE34;
    private static final ImageBoard<SafebooruImage> safebooru = DefaultImageBoards.SAFEBOORU;
    private static final ImageBoard<YandereImage> yandere = DefaultImageBoards.YANDERE;
    private static final ImageBoard<GelbooruImage> gelbooru = DefaultImageBoards.GELBOORU;
    private static final WeebAPIRequester weebAPIRequester = new WeebAPIRequester();

    private static final Random random = new Random();

    @Subscribe
    public void cat(CommandRegistry cr) {
        cr.register("cat", new SimpleCommand(CommandCategory.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                try {
                    var result = weebAPIRequester
                            .getRandomImageByType("animal_cat", false, null);

                    var url = result.getKey();
                    var builder = new MessageBuilder()
                            .append(EmoteReference.TALKING).append(catResponses[random.nextInt(catResponses.length)]
                            .replace("%mention%", ctx.getAuthor().getName()))
                            .build();

                    ctx.getChannel().sendMessage(builder)
                            .addFile(imageCache.getFile(url), "cat-" + result.getValue() + ".png")
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
        cr.register("catgirl", new SimpleCommand(CommandCategory.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");

                if (nsfw && !nsfwCheck(ctx, true, true, null)) {
                    return;
                }

                try {
                    var result = weebAPIRequester.getRandomImageByType("neko", nsfw, null);
                    var image = result.getKey();

                    if (image == null) {
                        ctx.sendLocalized("commands.imageboard.catgirl.error");
                        return;
                    }

                    ctx.getChannel().sendFile(
                            imageCache.getInput(image), "catgirl-" + result.getValue() + ".png"
                    ).queue();
                } catch (Exception e) {
                    ctx.sendLocalized("commands.imageboard.catgirl.error");
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Sends images of catgirl(s). Maybe.")
                        .setUsage("""
                                    `~>catgirl` - Sends images of normal catgirls.
                                    `~>catgirl nsfw` - Sends images of lewd catgirls. (Only works on NSFW channels)
                                    """
                        ).build();
            }
        });
    }

    @Subscribe
    public void e621(CommandRegistry cr) {
        cr.register("e621", new SimpleCommand(CommandCategory.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getChannel().isNSFW()) {
                    ctx.sendLocalized("commands.imageboard.e621_nsfw_notice", EmoteReference.ERROR);
                    return;
                }

                sendImage(ctx, e621, true, "e621", args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Retrieves images from the e621 (furry) image board.
                                Keep in mind the only images you'll get from here are furry images.
                                But if you're looking at this command you probably know already :)
                                This command can be only used in NSFW channels, as the imageboard only shows lewd images.
                                """
                        )
                        .setUsage(
                                 """
                                 `~>e621` - Retrieves a random image.
                                 `~>e621 <tag>` - Fetches an image with the respective tag and specified parameters.
                                 """
                        )
                        .addParameter("tag", TAG_HELP.formatted("e621"))
                        .build();
            }
        });
    }


    @Subscribe
    public void e926(CommandRegistry cr) {
        cr.register("e926", new SimpleCommand(CommandCategory.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getChannel().isNSFW()) {
                    ctx.sendLocalized("commands.imageboard.e926_nsfw_notice", EmoteReference.ERROR);
                    return;
                }

                sendImage(ctx, e926, false, "e926", args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Retrieves images from the e926 (furry) image board.
                                This command can be only used in NSFW channels, as the rating is inconsistent.
                                It **should** be a safe mirror of e621, though.
                                """
                        )
                        .setUsage(
                                """
                                `~>e926` - Retrieves a random image.
                                `~>e926 <tag>` - Fetches an image with the respective tag and specified parameters.
                                """
                        )
                        .addParameter("tag", TAG_HELP.formatted("e926"))
                        .build();
            }
        });
    }

    @Subscribe
    public void kona(CommandRegistry cr) {
        cr.register("konachan", new SimpleCommand(CommandCategory.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                sendImage(ctx, konachan, false, "konachan", args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Retrieves images from the Konachan image board.
                                If the rating is explicit/questionable this command can be only used in NSFW channels.
                                """
                        )
                        .setUsage(
                              """
                              `~>konachan` - Retrieves a random image.
                              `~>konachan <tag> <rating>` - Fetches an image with the respective tag and specified parameters
                              """
                        )
                        .addParameter("tag", TAG_HELP.formatted("konachan"))
                        .addParameter("rating", RATING_HELP).build();
            }
        });
    }

    @Subscribe
    public void safebooru(CommandRegistry cr) {
        cr.register("safebooru", new SimpleCommand(CommandCategory.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                sendImage(ctx, safebooru, false, "safebooru", args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves images from the Safebooru image board.")
                        .setUsage(
                                """
                                `~>safebooru` - Retrieves a random image.
                                `~>safebooru <tag>` - Fetches an image with the respective tag and specified parameters.
                                """
                        )
                        .addParameter("tag", TAG_HELP.formatted("safebooru"))
                        .build();
            }
        });
    }

    @Subscribe
    public void danbooru(CommandRegistry cr) {
        cr.register("danbooru", new SimpleCommand(CommandCategory.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                sendImage(ctx, danbooru, false, "danbooru", args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Retrieves images from the Danbooru image board.
                                If the rating is explicit/questionable this command can be only used in NSFW channels.
                                """
                        )
                        .setUsage(
                                """
                                `~>danbooru` - Retrieves a random image.
                                `~>danbooru <tag> <rating>` - Fetches an image with the respective tag and specified parameters.
                                """
                        )
                        .addParameter("tag", TAG_HELP.formatted("danbooru"))
                        .addParameter("rating", RATING_HELP)
                        .build();
            }
        });
    }

    @Subscribe
    public void rule34(CommandRegistry cr) {
        cr.register("rule34", new SimpleCommand(CommandCategory.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                sendImage(ctx, rule34, true, "rule34", args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Retrieves images from the Rule34 image board.
                                This command only works in NSFW channels. You could guess it from the name though ;)
                                """
                        )
                        .setUsage(
                                """
                                `~>rule34` - Retrieves a random image.
                                `~>rule34  <rating>` - Fetches an image with the respective tag.
                                """
                        )
                        .addParameter("tag", TAG_HELP.formatted("rule34")).build();
            }
        });
    }

    @Subscribe
    public void yandere(CommandRegistry cr) {
        cr.register("yandere", new SimpleCommand(CommandCategory.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getChannel().isNSFW()) {
                    ctx.sendLocalized("commands.imageboard.yandere_nsfw_notice", EmoteReference.ERROR);
                    return;
                }

                sendImage(ctx, yandere, false, "yandere", args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Retrieves images from the Yande.re image board.
                                This command only works on NSFW channels, regardless of the rating specified.
                                (because of course the maintainers think really harsh sexual acts qualify as enough to give it a safe rating I mean, sure).
                                """
                        )
                        .setUsage(
                                """
                                `~>yandere` - Retrieves a random image.
                                `~>yandere <tag> <rating>` - Fetches an image with the respective tag and specified parameters.         
                                """
                        )
                        .addParameter("tag", TAG_HELP.formatted("yande.re"))
                        .addParameter("rating", RATING_HELP)
                        .build();
            }
        });
    }

    @Subscribe
    public void gelbooru(CommandRegistry cr) {
        cr.register("gelbooru", new SimpleCommand(CommandCategory.IMAGE) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getChannel().isNSFW()) {
                    ctx.sendLocalized("commands.imageboard.yandere_nsfw_notice", EmoteReference.ERROR);
                    return;
                }

                sendImage(ctx, gelbooru, false, "gelbooru", args);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Retrieves images from the Gelbooru image board.
                                This command only works on NSFW channels, regardless of rating
                                (because we're not sure if it'll really put safe images all the time, rating is still left to the user).
                                """
                        )
                        .setUsage(
                                """
                                `~>gelbooru` - Retrieves a random image.
                                `~>gelbooru <tag> <rating>` - Fetches an image with the respective tag and specified parameters.
                                """
                        )
                        .addParameter("tag", TAG_HELP.formatted("gelbooru"))
                        .addParameter("rating", RATING_HELP)
                        .build();
            }
        });
    }

    private void sendImage(Context ctx, ImageBoard<?> image,
                           boolean nsfwOnly, String name, String[] args) {
        var firstArg = args.length == 0 ? "" : args[0];
        if (firstArg.isEmpty()) {
            getImage(image, ImageRequestType.RANDOM, nsfwOnly, name, args, ctx);
        } else {
            getImage(image, ImageRequestType.TAGS, nsfwOnly, name, args, ctx);
        }
    }
}
