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

package net.kodehawa.mantarobot.commands.image;

import com.google.common.collect.ImmutableMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kodehawa.lib.imageboards.DefaultImageBoards;
import net.kodehawa.lib.imageboards.ImageBoard;
import net.kodehawa.lib.imageboards.entities.BoardImage;
import net.kodehawa.lib.imageboards.entities.Rating;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;
import java.util.stream.Collectors;

public class ImageboardUtils {
    private static final Random r = new Random();
    private static final Map<ImageBoard<?>, Integer> maxQuerySize = ImmutableMap.of(
            DefaultImageBoards.KONACHAN, 5,
            DefaultImageBoards.YANDERE, 5,
            DefaultImageBoards.DANBOORU, 1,
            DefaultImageBoards.SAFEBOORU, 1
    );

    private static final Map<ImageBoard<?>, Boolean> imageboardUsesRating = ImmutableMap.of(
            DefaultImageBoards.SAFEBOORU, false,
            DefaultImageBoards.RULE34, false,
            DefaultImageBoards.E621, false
    );

    public static void getImage(ImageBoard<?> api, ImageRequestType type, boolean nsfwOnly, String imageboard, String[] args, Context ctx) {
        Rating rating = Rating.SAFE;
        List<String> list = new ArrayList<>(Arrays.asList(args));

        list.remove("tags"); // remove tags from argument list. (BACKWARDS COMPATIBILITY)

        boolean needRating = list.size() >= 2;
        if (needRating && !nsfwOnly) {
            rating = lookupRating(list.get(1));
        } else if (!needRating && !list.isEmpty()) {
            // Attempt to get from the tags instead.
            rating = lookupRating(list.get(0));
        }

        if (rating == null && needRating) {
            // Try with short name
            rating = lookupShortRating(list.get(1));

            if (rating != null)
                list.remove(rating.getShortName());
        }

        // Allow for more tags after declaration.
        Rating finalRating = rating;
        if (finalRating != null) {
            list.remove(rating.getLongName());
            list.remove("random"); // remove "random" declaration.
            list.remove("r"); // Remove short-hand random declaration.

        } else {
            finalRating = Rating.SAFE;
        }

        if (!nsfwCheck(ctx, nsfwOnly, false, finalRating)) {
            ctx.sendLocalized("commands.imageboard.nsfw_no_nsfw", EmoteReference.ERROR);
            return;
        }

        if (!Optional.ofNullable(imageboardUsesRating.get(api)).orElse(true))
            finalRating = null;

        int limit = Optional.ofNullable(maxQuerySize.get(api)).orElse(10);

        if (list.size() > limit) {
            ctx.sendLocalized("commands.imageboard.too_many_tags", EmoteReference.ERROR, imageboard, limit);
            return;
        }

        if(type == ImageRequestType.TAGS) {
            try {
                DBGuild dbGuild = ctx.getDBGuild();
                GuildData data = dbGuild.getData();

                if (list.stream().anyMatch(tag -> data.getBlackListedImageTags().contains(tag))) {
                    ctx.sendLocalized("commands.imageboard.blacklisted_tag", EmoteReference.ERROR);
                    return;
                }

                api.search(list, finalRating).async(requestedImages -> {
                    // Account for this, somehow this happens sometimes.
                    if (isListNull(requestedImages, ctx))
                        return;

                    try {
                        List<BoardImage> filter = requestedImages.stream()
                                // Somehow Danbooru and e621 are returning null images when a image is deleted?
                                .filter(img -> img.getURL() != null)
                                // There should be no need for searches to contain loli content anyway, if it's gonna get locked away.
                                // This is more of a quality-of-life improvement, don't make them search again if random happened
                                // to pick undesirable lewd content.
                                // This also gets away with the need to re-roll, unless they looked up a prohibited tag.
                                .filter(img -> !containsExcludedTags(img.getTags()))
                                .collect(Collectors.toList());

                        if (filter.isEmpty()) {
                            ctx.sendLocalized("commands.imageboard.no_images", EmoteReference.SAD);
                            return;
                        }

                        BoardImage image = filter.get(r.nextInt(filter.size()));
                        sendImage(ctx, imageboard, image, dbGuild);
                    } catch (Exception e) {
                        ctx.sendLocalized("commands.imageboard.no_results", EmoteReference.SAD);
                    }
                }, failure -> {
                    ctx.sendLocalized("commands.imageboard.error_tag", EmoteReference.SAD);
                    failure.printStackTrace();
                });
            } catch (NumberFormatException nex) {
                ctx.sendLocalized("commands.imageboard.wrong_argument", EmoteReference.ERROR, imageboard);
            } catch (Exception ex) {
                ctx.sendLocalized("commands.imageboard.error_tag", EmoteReference.SAD);
            }
        } else if (type == ImageRequestType.RANDOM) {
            api.search(list, finalRating).async(requestedImages -> {
                try {
                    if (isListNull(requestedImages, ctx))
                        return;

                    List<BoardImage> filter = requestedImages.stream()
                            // Somehow Danbooru and e621 are returning null images when a image is deleted?
                            .filter(img -> img.getURL() != null)
                            // There should be no need for searches to contain loli content anyway, if it's gonna get locked away.
                            // This is more of a quality-of-life improvement, don't make them search again if random happened
                            // to pick undesirable lewd content.
                            // This also gets away with the need to re-roll, unless they looked up a prohibited tag.
                            .filter(img -> !containsExcludedTags(img.getTags()))
                            .filter(img -> !(img.getRating() == Rating.EXPLICIT && img.hasChildren()))
                            .collect(Collectors.toList());

                    if (filter.isEmpty()) {
                        ctx.sendLocalized("commands.imageboard.no_images", EmoteReference.SAD);
                        return;
                    }

                    int number = r.nextInt(filter.size());
                    BoardImage image = filter.get(number);
                    sendImage(ctx, imageboard, image, ctx.getDBGuild());
                } catch (Exception e) {
                    ctx.sendLocalized("commands.imageboard.error_random", EmoteReference.SAD);
                    e.printStackTrace();
                }
            }, failure -> {
                ctx.sendLocalized("commands.imageboard.error_random", EmoteReference.SAD);
                failure.printStackTrace();
            });
        }
    }

    private static void sendImage(Context ctx, String imageboard, BoardImage image, DBGuild dbGuild) {
        // This is the last line of defense. It should filter *all* minor tags from all sort of images on
        // the method that calls this.
        if ((containsExcludedTags(image.getTags()) || image.hasChildren()) && image.getRating() != Rating.SAFE) {
            ctx.sendLocalized("commands.imageboard.loli_content_disallow", EmoteReference.WARNING);
            return;
        }

        if (image.getTags().stream().anyMatch(tag -> dbGuild.getData().getBlackListedImageTags().contains(tag))) {
            ctx.sendLocalized("commands.imageboard.blacklisted_tag", EmoteReference.ERROR);
            return;
        }

        // Format the tags output so it's actually human-readable.
        String imageTags = String.join(", ", image.getTags());

        imageEmbed(
                ctx.getLanguageContext(), image.getURL(), String.valueOf(image.getWidth()),
                String.valueOf(image.getHeight()), imageTags, image.getRating(), imageboard, ctx.getChannel()
        );

        if (image.getRating().equals(Rating.EXPLICIT)) {
            Player player = ctx.getPlayer();
            if (player.getData().addBadgeIfAbsent(Badge.LEWDIE))
                player.saveAsync();

            // Drop a lewd magazine.
            TextChannelGround.of(ctx.getEvent()).dropItemWithChance(ItemReference.LEWD_MAGAZINE, 4);
        }
    }

    public static boolean nsfwCheck(Context ctx, boolean isGlobal, boolean sendMessage, Rating rating) {
        if (ctx.getChannel().isNSFW())
            return true;

        Rating finalRating = rating == null ? Rating.SAFE : rating;
        boolean trigger = finalRating.equals(Rating.SAFE) && !isGlobal;

        if (!trigger) {
            if (sendMessage)
                ctx.sendLocalized("commands.imageboard.non_nsfw_channel", EmoteReference.ERROR);

            return false;
        }

        return true;
    }

    // The list of tags to exclude from searches.
    private final static String[] excludedTags = {
            "loli", "shota", "lolicon", "shotacon", "child", "underage", "young", "younger",
            "under_age", "cub", "tagme"
    };

    private static boolean containsExcludedTags(List<String> tags) {
        for(String tag : excludedTags) {
            if(tags.contains(tag)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isListNull(List<?> l, Context ctx) {
        if (l == null) {
            ctx.sendLocalized("commands.imageboard.null_image_notice", EmoteReference.ERROR);
            return true;
        }

        return false;
    }

    private static void imageEmbed(I18nContext languageContext, String url, String width, String height,
                                   String tags, Rating rating, String imageboard, TextChannel channel) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(languageContext.get("commands.imageboard.found_image"), url, null)
                .setImage(url)
                .setDescription(String.format(languageContext.get("commands.imageboard.description_image"),
                        rating.getLongName(), imageboard)
                ).addField(languageContext.get("commands.imageboard.width"), width, true)
                .addField(languageContext.get("commands.imageboard.height"), height, true)
                .addField(languageContext.get("commands.imageboard.tags"), "`" + (tags == null ? "None" : tags) + "`", false)
                .setFooter(languageContext.get("commands.imageboard.load_notice") + (imageboard.equals("rule34") ? " " +
                        languageContext.get("commands.imageboard.rule34_notice") : ""), null
                );

        channel.sendMessage(builder.build()).queue();
    }

    //This is so random is a valid rating.
    private static Rating lookupRating(String rating) {
        if(rating.equalsIgnoreCase("random")) {
            Rating[] values = Rating.values();
            return values[r.nextInt(values.length)];
        } else {
            return Rating.lookupFromString(rating);
        }
    }

    //This is so random (R) is a valid rating.
    private static Rating lookupShortRating(String shortRating) {
        if(shortRating.equalsIgnoreCase("r")) {
            Rating[] values = Rating.values();
            return values[r.nextInt(values.length)];
        } else {
            return Rating.lookupFromStringShort(shortRating);
        }
    }
}
