/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;
import net.kodehawa.lib.imageboards.DefaultImageBoards;
import net.kodehawa.lib.imageboards.ImageBoard;
import net.kodehawa.lib.imageboards.entities.impl.DanbooruImage;
import net.kodehawa.lib.imageboards.entities.impl.FurryImage;
import net.kodehawa.lib.imageboards.entities.impl.GelbooruImage;
import net.kodehawa.lib.imageboards.entities.impl.KonachanImage;
import net.kodehawa.lib.imageboards.entities.impl.Rule34Image;
import net.kodehawa.lib.imageboards.entities.impl.SafeFurryImage;
import net.kodehawa.lib.imageboards.entities.impl.SafebooruImage;
import net.kodehawa.lib.imageboards.entities.impl.YandereImage;
import net.kodehawa.mantarobot.commands.action.WeebAPIRequester;
import net.kodehawa.mantarobot.commands.image.ImageRequestType;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.NSFW;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.command.meta.Module;
import net.kodehawa.mantarobot.core.command.helpers.CommandCategory;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Random;

import static net.kodehawa.mantarobot.commands.image.ImageboardUtils.getImage;

@Module
public class ImageCmds {
    private static final URLCache imageCache = new URLCache(20);
    private static final String[] catResponses = {
            "Aww, here, take a cat.", "%mention%, are you sad? ;w; take a cat!",
            "You should all have a cat in your life, but an image will do.",
            "Am I cute yet?", "I think you should have a cat, %mention%.",
            "Meow~ %mention%", "Nya~ %mention%"
    };

    private static final String[] dogResponses = {
            "A cute doggo.",
            "Woah, just look at that cuteness!",
            "Woof? %mention%, Woof!",
    };

    // I basically repeated those two all the time, so might aswell just
    // make them constants instead.
    private static final String RATING_HELP = """
                                        The image rating, can be either safe, questionable or explicit.
                                        You can also use this in place of the tags.
                                        Rating can be random if you specify it as random, in case you want to play a roulette.
                                        """;

    private static final String TAG_HELP = "The image tag you're looking for. You can see a list of valid tags on the imageboard website.";

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
    public void register(CommandRegistry cr) {
        cr.registerSlash(Image.class);
        cr.registerSlash(Konachan.class);
        cr.registerSlash(Danbooru.class);
        cr.registerSlash(Gelbooru.class);
        cr.registerSlash(Safebooru.class);
        cr.registerSlash(Yandere.class);
        cr.registerSlash(E621.class);
        cr.registerSlash(E926.class);
        cr.registerSlash(Rule34.class);
    }

    @Name("image")
    @Description("Contains various imageboard commands.")
    @Category(CommandCategory.IMAGE)
    public static class Image extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}
        @Name("cat")
        @Defer
        @Description("Sends a random cat image. Really cute stuff.")
        @Help(description = "Sends a random cat image. Really cute stuff, you know?")
        public static class Cat extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                try {
                    var result = weebAPIRequester.getRandomImageByType("animal_cat", false, null);
                    var url = result.url();
                    var embed = new EmbedBuilder()
                            .setAuthor(catResponses[random.nextInt(catResponses.length)].replace("%mention%", ctx.getMember().getEffectiveName()),
                                    null, ctx.getAuthor().getEffectiveAvatarUrl())
                            .setColor(ctx.getMemberColor())
                            .setImage(url)
                            .build();

                    ctx.reply(embed);
                } catch (Exception e) {
                    ctx.reply("commands.imageboard.cat.error", EmoteReference.ERROR);
                }
            }
        }

        @Name("dog")
        @Defer
        @Description("Sends a random dog image. Really cute stuff.")
        @Help(description = "Sends a random dog image. Really cute stuff, you know?")
        public static class Dog extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                try {
                    var result = weebAPIRequester.getRandomImageByType("animal_dog", false, null);
                    var url = result.url();
                    var embed = new EmbedBuilder()
                            .setAuthor(dogResponses[random.nextInt(dogResponses.length)].replace("%mention%", ctx.getMember().getEffectiveName()),
                                    null, ctx.getAuthor().getEffectiveAvatarUrl())
                            .setColor(ctx.getMemberColor())
                            .setImage(url)
                            .build();

                    ctx.reply(embed);
                } catch (Exception e) {
                    ctx.reply("commands.imageboard.dog.error", EmoteReference.ERROR);
                }
            }
        }

        @Name("catgirl")
        @Defer
        @Description("Sends images of catgirl(s). Maybe.")
        @Help(description = "Sends images of catgirl(s). Maybe.",
                usage = "`/catgirl` - Sends images of catgirls.",
                parameters = {
                    @Help.Parameter(name = "nsfw", description = "Whether to send a NSFW image.")
                }
        )
        @Options({
                @Options.Option(type = OptionType.BOOLEAN, name = "nsfw", description = "Whether to send a NSFW image (only works on NSFW channels)")
        })
        public static class Catgirl extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                try {
                    var result = weebAPIRequester.getRandomImageByType("neko", false, null);
                    if (result == null) {
                        ctx.reply("commands.imageboard.catgirl.error");
                        return;
                    }

                    var image = result.url();

                    ctx.getEvent().getHook()
                            .sendFiles(FileUpload.fromData(imageCache.getInput(image), "catgirl-%s.%s".formatted(result.id(), result.fileType())))
                            .queue();
                } catch (Exception e) {
                    ctx.reply("commands.imageboard.catgirl.error");
                }
            }
        }
    }

    @Name("e621")
    @Defer
    @NSFW
    @Description("Retrieves images from the e621 (furry) image board. (Only works on NSFW channels)")
    @Category(CommandCategory.IMAGE)
    @Help(description = """
            Retrieves images from the e621 (furry) image board.
            Keep in mind the only images you'll get from here are furry images.
            But if you're looking at this command you probably know already :)
            This command can be only used in NSFW channels, as the imageboard only shows lewd images.
            """,
            usage =
                    """
                    `/e621` - Retrieves a random image.
                    `/e621 tags:[tag list]` - Retrieves a image with the specified tags.
                    """,
            parameters = {
                    @Help.Parameter(name = "tags", description = TAG_HELP, optional = true),
                    @Help.Parameter(name = "excludetags", description = "Same as tags, but excludes them instead.", optional = true)
            })
    @Options({
            @Options.Option(type = OptionType.STRING, name = "tags", description = "Image tags, separated by a space."),
            @Options.Option(type = OptionType.STRING, name = "excludetags", description = "Image tags to exclude, separated by a space."),

    })
    public static class E621 extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (!ctx.isChannelNSFW()) {
                ctx.reply("commands.imageboard.e621_nsfw_notice", EmoteReference.ERROR);
                return;
            }

            sendImage(ctx, e621, true, "e621", null, ctx.getOptionAsString("tags", ""),
                    ctx.getOptionAsString("excludetags", ""));
        }
    }

    @Name("e926")
    @Defer
    @NSFW
    @Description("Retrieves images from the e926 (furry) image board. (Only works on NSFW channels)")
    @Category(CommandCategory.IMAGE)
    @Help(description = """
                    Retrieves images from the e926 (furry) image board.
                    This command can be only used in NSFW channels, as the rating is inconsistent.
                    It **should** be a safe mirror of e621, though.
                    """,
            usage =  """
                    `/e926` - Retrieves a random image.
                    `/e926 tags:[tag list]` - Retrieves a image with the specified tags.
                    """,
            parameters = {
                    @Help.Parameter(name = "tags", description = TAG_HELP, optional = true),
                    @Help.Parameter(name = "excludetags", description = "Same as tags, but excludes them instead.", optional = true)
            }
    )
    @Options({
            @Options.Option(type = OptionType.STRING, name = "tags", description = "Image tags, separated by a space."),
            @Options.Option(type = OptionType.STRING, name = "excludetags", description = "Image tags to exclude, separated by a space."),
    })
    public static class E926 extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            sendImage(ctx, e926, false, "e926", "safe", ctx.getOptionAsString("tags", ""),
                    ctx.getOptionAsString("excludetags", ""));
        }
    }

    @Name("konachan")
    @Defer
    @NSFW
    @Description("Retrieves images from the konachan image board. (Only works on NSFW channels)")
    @Category(CommandCategory.IMAGE)
    @Help(description = """
                    Retrieves images from the Konachan image board. This command can only be used in NSFW channels.
                    """,
            usage = """
                    `/konachan` - Retrieves a random image.
                    `/konachan rating:[rating] tags:[tag list]` - Retrieves a image with the specified tags.
                    """,
            parameters = {
                    @Help.Parameter(name = "rating", description = RATING_HELP, optional = true),
                    @Help.Parameter(name = "tags", description = TAG_HELP, optional = true),
                    @Help.Parameter(name = "excludetags", description = "Same as tags, but excludes them instead.", optional = true)
            }
    )
    @Options({
            @Options.Option(
                    type = OptionType.STRING,
                    name = "rating",
                    description = "Image rating. Can either be random, safe, questionable or explicit.",
                    choices = {
                            @Options.Choice(description = "Safe for Work Image", value = "safe"),
                            @Options.Choice(description = "Questionable/Potentially Unsafe Image", value = "questionable"),
                            @Options.Choice(description = "Not Safe for Work (NSFW/Explicit) Image", value = "explicit"),
                            @Options.Choice(description = "Random (any of the above) Image", value = "random")
                    }
            ),
            @Options.Option(type = OptionType.STRING, name = "tags", description = "Image tags, separated by a space."),
            @Options.Option(type = OptionType.STRING, name = "excludetags", description = "Image tags to exclude, separated by a space."),
    })
    public static class Konachan extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (!ctx.isChannelNSFW()) {
                ctx.reply("commands.imageboard.konachan_nsfw_notice", EmoteReference.ERROR);
                return;
            }

            sendImage(
                    ctx, konachan, false, "konachan",
                    ctx.getOptionAsString("rating", "safe"), ctx.getOptionAsString("tags", ""),
                    ctx.getOptionAsString("excludetags", "")
            );
        }
    }

    @Name("yandere")
    @Defer
    @NSFW
    @Description("Retrieves images from the Yande.re image board. (Only works on NSFW channels)")
    @Category(CommandCategory.IMAGE)
    @Help(description = """
                    Retrieves images from the Yande.re image board.
                    This command only works on NSFW channels, regardless of the rating specified.
                    (because of course the maintainers think really harsh sexual acts qualify as enough to give it a safe rating I mean, sure).
                    """,
            usage = """
                    `/yandere` - Retrieves a random image.
                    `/yandere rating:[rating] tags:[tag list]` - Retrieves a image with the specified tags.
                    """,
            parameters = {
                    @Help.Parameter(name = "rating", description = RATING_HELP, optional = true),
                    @Help.Parameter(name = "tags", description = TAG_HELP, optional = true),
                    @Help.Parameter(name = "excludetags", description = "Same as tags, but excludes them instead.", optional = true)
            }
    )
    @Options({
            @Options.Option(
                    type = OptionType.STRING,
                    name = "rating",
                    description = "Image rating. Can either be random, safe, questionable or explicit.",
                    choices = {
                            @Options.Choice(description = "Safe for Work Image", value = "safe"),
                            @Options.Choice(description = "Questionable/Potentially Unsafe Image", value = "questionable"),
                            @Options.Choice(description = "Not Safe for Work (NSFW/Explicit) Image", value = "explicit"),
                            @Options.Choice(description = "Random (any of the above) Image", value = "random")
                    }
            ),
            @Options.Option(type = OptionType.STRING, name = "tags", description = "Image tags, separated by a space."),
            @Options.Option(type = OptionType.STRING, name = "excludetags", description = "Image tags to exclude, separated by a space."),
    })
    public static class Yandere extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (!ctx.isChannelNSFW()) {
                ctx.reply("commands.imageboard.yandere_nsfw_notice", EmoteReference.ERROR);
                return;
            }

            sendImage(
                    ctx, yandere, false, "yandere",
                    ctx.getOptionAsString("rating", "safe"), ctx.getOptionAsString("tags", ""),
                    ctx.getOptionAsString("excludetags", "")
            );
        }
    }

    @Name("gelbooru")
    @Defer
    @NSFW
    @Description("Retrieves images from the Gelbooru image board. (Only works on NSFW channels)")
    @Category(CommandCategory.IMAGE)
    @Help(description = """
                    Retrieves images from the Gelbooru image board.
                    This command only works on NSFW channels, regardless of rating
                    (because we're not sure if it'll really put safe images all the time, rating is still left to the user).
                    """,
            usage = """
                    `/gelbooru` - Retrieves a random image.
                    `/gelbooru rating:[rating] tags:[tag list]` - Retrieves a image with the specified tags.
                    """,
            parameters = {
                    @Help.Parameter(name = "rating", description = RATING_HELP, optional = true),
                    @Help.Parameter(name = "tags", description = TAG_HELP, optional = true),
                    @Help.Parameter(name = "excludetags", description = "Same as tags, but excludes them instead.", optional = true)
            }
    )
    @Options({
            @Options.Option(
                    type = OptionType.STRING,
                    name = "rating",
                    description = "Image rating. Can either be random, safe, questionable or explicit.",
                    choices = {
                            @Options.Choice(description = "Safe for Work Image", value = "safe"),
                            @Options.Choice(description = "Questionable/Potentially Unsafe Image", value = "questionable"),
                            @Options.Choice(description = "Not Safe for Work (NSFW/Explicit) Image", value = "explicit"),
                            @Options.Choice(description = "Random (any of the above) Image", value = "random")
                    }
            ),
            @Options.Option(type = OptionType.STRING, name = "tags", description = "Image tags, separated by a space."),
            @Options.Option(type = OptionType.STRING, name = "excludetags", description = "Image tags to exclude, separated by a space."),
    })
    public static class Gelbooru extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (!ctx.isChannelNSFW()) {
                ctx.reply("commands.imageboard.yandere_nsfw_notice", EmoteReference.ERROR);
                return;
            }

            sendImage(
                    ctx, gelbooru, false, "gelbooru",
                    ctx.getOptionAsString("rating", "safe"), ctx.getOptionAsString("tags", ""),
                    ctx.getOptionAsString("excludetags", "")
            );
        }
    }

    @Name("safebooru")
    @Defer
    @NSFW
    @Description("Retrieves images from the safebooru image board. (Only works on NSFW channels)")
    @Category(CommandCategory.IMAGE)
    @Help(description = "Retrieves images from the safebooru image board. This command can only be used in NSFW channels.",
            usage = """
                    `/safebooru` - Retrieves a random image.
                    `/safebooru tags:[tag list]` - Retrieves a image with the specified tags.
                    """,
            parameters = {
                    @Help.Parameter(name = "tags", description = TAG_HELP, optional = true),
                    @Help.Parameter(name = "excludetags", description = "Same as tags, but excludes them instead.", optional = true)
            }
    )
    @Options({
            @Options.Option(type = OptionType.STRING, name = "tags", description = "Image tags, separated by a space."),
            @Options.Option(type = OptionType.STRING, name = "excludetags", description = "Image tags to exclude, separated by a space."),
    })
    public static class Safebooru extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (!ctx.isChannelNSFW()) {
                ctx.reply("commands.imageboard.konachan_nsfw_notice", EmoteReference.ERROR);
                return;
            }

            sendImage(ctx, safebooru, false, "safebooru", "safe", ctx.getOptionAsString("tags", ""),
                    ctx.getOptionAsString("excludetags", ""));
        }
    }

    @Name("rule34")
    @Defer
    @NSFW
    @Description("Retrieves images from the Rule34 image board. (Only works on NSFW channels)")
    @Category(CommandCategory.IMAGE)
    @Help(description = """
                Retrieves images from the Rule34 image board.
                This command only works in NSFW channels. You could guess it from the name though ;)
                """,
            usage = """
                `/rule34` - Retrieves a random image.
                `/rule34 tags:[tag list]` - Retrieves a image with the specified tags.
                """,
            parameters = {
                    @Help.Parameter(name = "tags", description = TAG_HELP, optional = true),
                    @Help.Parameter(name = "excludetags", description = "Same as tags, but excludes them instead.", optional = true)
            })
    @Options({
            @Options.Option(type = OptionType.STRING, name = "tags", description = "Image tags, separated by a space."),
            @Options.Option(type = OptionType.STRING, name = "excludetags", description = "Image tags to exclude, separated by a space."),
    })
    public static class Rule34 extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            // This is nsfw-only, so its restricted to nsfw channels aswell, just not with a special message.
            sendImage(ctx, rule34, true, "rule34", "explicit", ctx.getOptionAsString("tags", ""),
                    ctx.getOptionAsString("excludetags", ""));
        }
    }

    @Name("danbooru")
    @Defer
    @NSFW
    @Description("Retrieves images from the danbooru image board. (Only works on NSFW channels)")
    @Category(CommandCategory.IMAGE)
    @Help(description = """
                    Retrieves images from the Danbooru image board.
                    This command only works on NSFW channels, regardless of the rating specified, due to inconsistent ratings.
                    """,
            usage = """
                    `/danbooru` - Retrieves a random image.
                    `/danbooru rating:[rating] tags:[tag list]` - Retrieves a image with the specified tags.
                    """,
            parameters = {
                    @Help.Parameter(name = "rating", description = RATING_HELP, optional = true),
                    @Help.Parameter(name = "tags", description = TAG_HELP, optional = true),
                    @Help.Parameter(name = "excludetags", description = "Same as tags, but excludes them instead.", optional = true)
            }
    )
    @Options({
            @Options.Option(
                    type = OptionType.STRING,
                    name = "rating",
                    description = "Image rating. Can either be random, safe, questionable or explicit.",
                    choices = {
                            @Options.Choice(description = "Safe for Work Image", value = "safe"),
                            @Options.Choice(description = "Questionable/Potentially Unsafe Image", value = "questionable"),
                            @Options.Choice(description = "Not Safe for Work (NSFW/Explicit) Image", value = "explicit"),
                            @Options.Choice(description = "Random (any of the above) Image", value = "random")
                    }
            ),
            @Options.Option(type = OptionType.STRING, name = "tags", description = "Image tags, separated by a space."),
            @Options.Option(type = OptionType.STRING, name = "excludetags", description = "Image tags to exclude, separated by a space."),
    })
    public static class Danbooru extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (!ctx.isChannelNSFW()) {
                ctx.reply("commands.imageboard.konachan_nsfw_notice", EmoteReference.ERROR);
                return;
            }

            sendImage(
                    ctx, danbooru, false, "danbooru",
                    ctx.getOptionAsString("rating", "safe"), ctx.getOptionAsString("tags", ""),
                    ctx.getOptionAsString("excludetags", "")
            );
        }
    }

    private static void sendImage(SlashContext ctx, ImageBoard<?> image,
                                  boolean nsfwOnly, String name, String rating, String tags, String excludeTags) {
        if (tags.isEmpty() && excludeTags.isEmpty()) {
            getImage(image, ImageRequestType.RANDOM, nsfwOnly, name, rating, tags, excludeTags, ctx);
        } else {
            getImage(image, ImageRequestType.TAGS, nsfwOnly, name, rating, tags, excludeTags, ctx);
        }
    }
}
