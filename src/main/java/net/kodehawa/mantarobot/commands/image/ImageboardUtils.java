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

package net.kodehawa.mantarobot.commands.image;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.imageboards.ImageBoard;
import net.kodehawa.lib.imageboards.entities.BoardImage;
import net.kodehawa.lib.imageboards.entities.Rating;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ImageboardUtils {
    private static final Random r = new Random();

    public static void getImage(ImageBoard<?> api, String type, boolean nsfwOnly, String imageboard, String[] args, String content, GuildMessageReceivedEvent event) {
        Rating rating = Rating.SAFE;
        boolean needRating = args.length >= 3;
        TextChannel channel = event.getChannel();

        if(nsfwOnly)
            rating = Rating.EXPLICIT;

        try {
            if(needRating) rating = Rating.lookupFromString(args[2]);
        } catch(Exception e) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "You provided an invalid rating (Avaliable types: questionable, explicit, safe)!").queue();
            return;
        }

        final Rating fRating = rating;

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

                    api.get(page).async(images1 -> {
                        if(boom(images1, event)) return;

                        try {
                            int number;
                            List<BoardImage> images = (List<BoardImage>) images1;
                            if(!nsfwOnly)
                                images = images1.stream().filter(data -> data.getRating().equals(fRating)).collect(Collectors.toList());

                            if(images.isEmpty()) {
                                event.getChannel().sendMessage(EmoteReference.SAD + "There are no images matching your search criteria...").queue();
                                return;
                            }

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

                            imageEmbed(image.getURL(), String.valueOf(image.getWidth()), String.valueOf(image.getHeight()), tags, image.getRating(), imageboard, channel);
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
                    api.search(tags).async(images  -> {
                        //account for this
                        if(boom(images, event)) return;

                        try {
                            List<BoardImage> filter = (List<BoardImage>) images;
                            if(!nsfwOnly)
                                filter = images.stream().filter(data -> data.getRating().equals(fRating)).collect(Collectors.toList());

                            if(filter.isEmpty()) {
                                event.getChannel().sendMessage(EmoteReference.SAD + "There are no images matching your search criteria...").queue();
                                return;
                            }

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

                            imageEmbed(image.getURL(), String.valueOf(image.getWidth()), String.valueOf(image.getHeight()), tags1, image.getRating(), imageboard, channel);
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
                api.get(page).async(images -> {
                    if(boom(images, event)) return;

                    List<BoardImage> filter = (List<BoardImage>) images;
                    if(!nsfwOnly)
                        filter = images.stream().filter(data -> data.getRating().equals(fRating)).collect(Collectors.toList());

                    if(filter.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.SAD + "There are no images matching your search criteria...").queue();
                        return;
                    }

                    int number = r.nextInt(filter.size());
                    BoardImage image = filter.get(number);
                    String tags1 = image.getTags().stream().collect(Collectors.joining(", "));
                    imageEmbed(image.getURL(), String.valueOf(image.getWidth()), String.valueOf(image.getHeight()), tags1, image.getRating(), imageboard, channel);
                    TextChannelGround.of(event).dropItemWithChance(13, 3);
                });
                break;
        }
    }

    public static boolean nsfwCheck(GuildMessageReceivedEvent event, boolean isGlobal, boolean sendMessage, Rating rating) {
        if(event.getChannel().isNSFW()) return true;

        String nsfwChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildUnsafeChannels().stream()
                .filter(channel -> channel.equals(event.getChannel().getId())).findFirst().orElse(null);
        Rating rating1 = rating == null ? Rating.SAFE : rating;
        boolean trigger = !isGlobal ? ((rating1.equals(Rating.SAFE) || (nsfwChannel == null)) ?
                rating1.equals(Rating.SAFE) : nsfwChannel.equals(event.getChannel().getId())) :
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

    private static boolean foundMinorTags(GuildMessageReceivedEvent event, String tags, Rating rating) {
        boolean trigger = tags.contains("loli") || tags.contains("lolis") ||
                tags.contains("shota") || tags.contains("shotas") ||
                tags.contains("lolicon") || tags.contains("shotacon") &&
                (rating == null || rating.equals(Rating.EXPLICIT) || rating.equals(Rating.QUESTIONABLE));

        if(!trigger) {
            return false;
        }

        event.getChannel().sendMessage(EmoteReference.WARNING + "Sadly we cannot display images that allegedly contain `loli` or `shota` lewd/NSFW content because discord" +
                " prohibits it. (Filter ran: Image contains a loli or shota tag and it's NSFW)").queue();
        return true;
    }

    private static boolean boom(List<?> l, GuildMessageReceivedEvent event) {
        if(l == null) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Oops... something went wrong when searching...").queue();
            return true;
        }

        return false;
    }

    private static void imageEmbed(String url, String width, String height, String tags, Rating rating, String imageboard, TextChannel channel) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor("Found image", url, null)
                .setImage(url)
                .setDescription("Rating: **" + rating.getLongName() + "**, Imageboard: **" + imageboard + "**" )
                .addField("Width", width, true)
                .addField("Height", height, true)
                .addField("Tags", "`" + (tags == null ? "None" : tags) + "`", false)
                .setFooter("If the image doesn't load, click the title.", null);

        channel.sendMessage(builder.build()).queue();
    }
}
