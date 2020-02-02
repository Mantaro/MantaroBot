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

package net.kodehawa.mantarobot.commands.image;

import com.google.common.collect.ImmutableMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.imageboards.DefaultImageBoards;
import net.kodehawa.lib.imageboards.ImageBoard;
import net.kodehawa.lib.imageboards.entities.BoardImage;
import net.kodehawa.lib.imageboards.entities.Rating;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
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
    
    @SuppressWarnings("unchecked")
    public static void getImage(ImageBoard<?> api, ImageRequestType type, boolean nsfwOnly, String imageboard, String[] args, String content, GuildMessageReceivedEvent event, I18nContext languageContext) {
        Rating rating = Rating.SAFE;
        List<String> list = new ArrayList<>(Arrays.asList(args));
        list.remove("tags"); // remove tags from argument list. (BACKWARDS COMPATIBILITY)
        
        boolean needRating = list.size() >= 2;
        final TextChannel channel = event.getChannel();
        final Player player = MantaroData.db().getPlayer(event.getAuthor());
        final PlayerData playerData = player.getData();
        
        if(needRating && !nsfwOnly) {
            rating = Rating.lookupFromString(list.get(1));
        }
        
        if(rating == null) {
            //Try with short name
            rating = Rating.lookupFromStringShort(list.get(1));
            
            if(rating != null) {
                list.remove(rating.getShortName());
            }
        }
        
        //Allow for more tags after declaration.
        Rating finalRating = rating;
        if(finalRating != null) {
            list.remove(rating.getLongName());
        } else {
            finalRating = Rating.SAFE;
        }
        
        if(!nsfwCheck(event, languageContext, nsfwOnly, false, finalRating)) {
            channel.sendMessageFormat(languageContext.get("commands.imageboard.nsfw_no_nsfw"), EmoteReference.ERROR).queue();
            return;
        }
        
        if(!Optional.ofNullable(imageboardUsesRating.get(api)).orElse(true)) {
            finalRating = null;
        }
        
        int page = Math.max(1, r.nextInt(25));
        int limit = Optional.ofNullable(maxQuerySize.get(api)).orElse(10);
        
        if(list.size() > limit) {
            channel.sendMessageFormat(languageContext.get("commands.imageboard.too_many_tags"), EmoteReference.ERROR, imageboard, limit).queue();
            return;
        }
        
        switch(type) {
            case TAGS:
                try {
                    //Keep this: basically we will still accept the old way of doing it.
                    //See up there for the actual removal of the tags from the old checking.
                    String replaced = content.replace("tags ", "");
                    String[] arguments = replaced.split(" ");
                    String tags = arguments[0];
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    if(dbGuild.getData().getBlackListedImageTags().contains(tags.toLowerCase())) {
                        channel.sendMessageFormat(languageContext.get("commands.imageboard.blacklisted_tag"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    api.search(list, finalRating).async(requestedImages -> {
                        //account for this
                        if(isListNull(requestedImages, languageContext, event)) return;
                        
                        try {
                            List<BoardImage> filter = (List<BoardImage>) requestedImages;
                            
                            //We didn't find anything after filtering?
                            if(filter.isEmpty()) {
                                channel.sendMessageFormat(languageContext.get("commands.imageboard.no_images"), EmoteReference.SAD).queue();
                                return;
                            }
                            
                            int number;
                            try {
                                number = Integer.parseInt(arguments[1]);
                            } catch(NumberFormatException | ArrayIndexOutOfBoundsException e) {
                                number = r.nextInt(filter.size() > 0 ? filter.size() - 1 : filter.size());
                            }
                            
                            BoardImage image = filter.get(number);
                            String imageTags = image.getTags().stream().collect(Collectors.joining(", "));
                            
                            if(foundMinorTags(event, languageContext, imageTags, image.getRating())) {
                                return;
                            }
                            
                            if(image.getTags().stream().anyMatch(tag -> dbGuild.getData().getBlackListedImageTags().contains(tag))) {
                                channel.sendMessageFormat(languageContext.get("commands.imageboard.blacklisted_tag"), EmoteReference.ERROR).queue();
                                return;
                            }
                            
                            imageEmbed(languageContext, image.getURL(), String.valueOf(image.getWidth()), String.valueOf(image.getHeight()), imageTags, image.getRating(), imageboard, channel);
                            if(image.getRating().equals(Rating.EXPLICIT)) {
                                if(playerData.addBadgeIfAbsent(Badge.LEWDIE)) {
                                    player.saveAsync();
                                }
                                
                                TextChannelGround.of(event).dropItemWithChance(13, 3);
                            }
                        } catch(Exception e) {
                            channel.sendMessageFormat(languageContext.get("commands.imageboard.no_results"), EmoteReference.SAD).queue();
                        }
                    }, failure -> channel.sendMessageFormat(languageContext.get("commands.imageboard.error_tag"), EmoteReference.SAD).queue());
                } catch(NumberFormatException numberEx) {
                    channel.sendMessageFormat(languageContext.get("commands.imageboard.wrong_argument"), EmoteReference.ERROR, imageboard).queue(
                            message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
                    );
                } catch(Exception exception) {
                    channel.sendMessageFormat(languageContext.get("commands.imageboard.error_tag"), EmoteReference.SAD).queue();
                }
                
                break;
            case RANDOM:
                api.search(list, finalRating).async(requestedImages -> {
                    try {
                        if(isListNull(requestedImages, languageContext, event)) return;
                        
                        List<BoardImage> filter = (List<BoardImage>) requestedImages;
                        
                        if(filter.isEmpty()) {
                            channel.sendMessageFormat(languageContext.get("commands.imageboard.no_images"), EmoteReference.SAD).queue();
                            return;
                        }
                        
                        int number = r.nextInt(filter.size());
                        BoardImage image = filter.get(number);
                        String tags = image.getTags().stream().collect(Collectors.joining(", "));
                        
                        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                        
                        if(foundMinorTags(event, languageContext, tags, image.getRating())) {
                            return;
                        }
                        
                        if(image.getTags().stream().anyMatch(tag -> dbGuild.getData().getBlackListedImageTags().contains(tag))) {
                            channel.sendMessageFormat(languageContext.get("commands.imageboard.blacklisted_tag"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        imageEmbed(languageContext, image.getURL(), String.valueOf(image.getWidth()), String.valueOf(image.getHeight()), tags, image.getRating(), imageboard, channel);
                        if(image.getRating().equals(Rating.EXPLICIT)) {
                            if(playerData.addBadgeIfAbsent(Badge.LEWDIE)) {
                                player.saveAsync();
                            }
                            
                            TextChannelGround.of(event).dropItemWithChance(13, 3);
                        }
                    } catch(Exception e) {
                        channel.sendMessageFormat(languageContext.get("commands.imageboard.error_random"), EmoteReference.SAD).queue();
                    }
                }, failure -> channel.sendMessageFormat(languageContext.get("commands.imageboard.error_random"), EmoteReference.SAD).queue());
                break;
        }
    }
    
    public static boolean nsfwCheck(GuildMessageReceivedEvent event, I18nContext languageContext, boolean isGlobal, boolean sendMessage, Rating rating) {
        if(event.getChannel().isNSFW())
            return true;
        
        Rating finalRating = rating == null ? Rating.SAFE : rating;
        boolean trigger = finalRating.equals(Rating.SAFE) && !isGlobal;
        
        if(!trigger) {
            if(sendMessage) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.imageboard.non_nsfw_channel"), EmoteReference.ERROR).queue();
            }
            return false;
        }
        
        return true;
    }
    
    private static boolean foundMinorTags(GuildMessageReceivedEvent event, I18nContext languageContext, String tags, Rating rating) {
        boolean trigger = (tags.contains("loli") || tags.contains("shota") ||
                                   tags.contains("lolicon") || tags.contains("shotacon") ||
                                   //lol @ e621
                                   tags.contains("child") || tags.contains("young")) ||
                                  //lol @ danbooru
                                  tags.contains("younger") ||
                                  //lol @ rule34
                                  tags.contains("underage") || tags.contains("under_age")
                                  //lol @ rule34 / @ e621
                                  || tags.contains("cub")
                                             && !rating.equals(Rating.SAFE);
        
        if(!trigger) {
            return false;
        }
        
        event.getChannel().sendMessageFormat(languageContext.get("commands.imageboard.loli_content_disallow"), EmoteReference.WARNING).queue();
        return true;
    }
    
    private static boolean isListNull(List<?> l, I18nContext languageContext, GuildMessageReceivedEvent event) {
        if(l == null) {
            event.getChannel().sendMessageFormat(languageContext.get("commands.imageboard.null_image_notice"), EmoteReference.ERROR).queue();
            return true;
        }
        
        return false;
    }
    
    private static void imageEmbed(I18nContext languageContext, String url, String width, String height, String tags, Rating rating, String imageboard, TextChannel channel) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(languageContext.get("commands.imageboard.found_image"), url, null)
                .setImage(url)
                .setDescription(String.format(languageContext.get("commands.imageboard.description_image"), rating.getLongName(), imageboard))
                .addField(languageContext.get("commands.imageboard.width"), width, true)
                .addField(languageContext.get("commands.imageboard.height"), height, true)
                .addField(languageContext.get("commands.imageboard.tags"), "`" + (tags == null ? "None" : tags) + "`", false)
                .setFooter(languageContext.get("commands.imageboard.load_notice") + (imageboard.equals("rule34") ? " " + languageContext.get("commands.imageboard.rule34_notice") : ""), null);
        
        channel.sendMessage(builder.build()).queue();
    }
}
