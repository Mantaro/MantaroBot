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

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.graphql.CharacterSearchQuery;
import net.kodehawa.mantarobot.graphql.MediaSearchQuery;
import net.kodehawa.mantarobot.graphql.type.MediaType;
import net.kodehawa.mantarobot.utils.Anilist;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import okhttp3.*;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Module
@SuppressWarnings("all" /* NO IT WONT FUCKING NPE */)
public class AnimeCmds {
    private final static OkHttpClient client = new OkHttpClient();

    @Subscribe
    public void anime(CommandRegistry cr) {
        cr.register("anime", new SimpleCommand(Category.FUN) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                try {
                    if(content.isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.anime.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }

                    List<MediaSearchQuery.Medium> found = Anilist.searchMedia(content)
                            .stream()
                            .filter(media -> media.type() == MediaType.ANIME)
                            .collect(Collectors.toList());

                    if(found.isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.no_results"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(found.size() == 1) {
                        animeData(event, languageContext, found.get(0));
                        return;
                    }

                    DiscordUtils.selectList(event, found, anime -> String.format("**[%s (%s)](%s)**",
                            anime.title().english() == null || anime.title().english().isEmpty() ?
                                    anime.title().romaji() : anime.title().english(), anime.title().native_(), anime.siteUrl()),
                            s -> baseEmbed(event, languageContext.withRoot("commands", "anime.selection_start"))
                                    .setDescription(s)
                                    .setThumbnail("https://anilist.co/img/logo_al.png")
                                    .setFooter(languageContext.withRoot("commands", "anime.information_footer"), event.getAuthor().getAvatarUrl())
                                    .build(),
                            anime -> animeData(event, languageContext, anime));
                } catch (JsonSyntaxException jsonException) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.no_results"), EmoteReference.ERROR).queue();
                } catch (NullPointerException nullException) {
                    nullException.printStackTrace();
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.malformed_result"), EmoteReference.ERROR).queue();
                } catch (Exception exception) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.error"),
                            EmoteReference.ERROR, exception.getClass().getSimpleName()).queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Anime command")
                        .setDescription("**Get anime info from AniList (For anime characters use ~>character).**")
                        .addField("Usage", "`~>anime <animename>` - **Retrieve information of an anime based on the name.**", false)
                        .addField("Parameters",
                                "`animename` - **The name of the anime you are looking for. Keep queries similar to their english names!**", false)
                        .setColor(Color.PINK)
                        .build();
            }
        });

        cr.registerAlias("anime", "animu");
    }

    @Subscribe
    public void character(CommandRegistry cr) {
        cr.register("character", new SimpleCommand(Category.FUN) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                try {
                    if(content.isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.character.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }

                    List<CharacterSearchQuery.Character> characters = Anilist.searchCharacters(content);

                    if(characters.isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.no_results"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(characters.size() == 1) {
                        characterData(event, languageContext, characters.get(0));
                        return;
                    }

                    DiscordUtils.selectList(event, characters, character -> String.format("**[%s %s](%s)**",
                            character.name().last() == null ? "" : character.name().last(), character.name().first(),
                            character.siteUrl()),
                            s -> baseEmbed(event, languageContext.withRoot("commands", "anime.information_footer"))
                                    .setDescription(s)
                                    .setThumbnail("https://anilist.co/img/logo_al.png")
                                    .setFooter(languageContext.withRoot("commands", "anime.information_footer"), event.getAuthor().getAvatarUrl())
                                    .build(),
                            character -> characterData(event, languageContext, character));
                } catch (JsonSyntaxException jsonException) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.no_results"), EmoteReference.ERROR).queue();
                } catch (NullPointerException nullException) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.malformed_results"), EmoteReference.ERROR).queue();
                } catch (Exception exception) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "character.error"),
                            EmoteReference.ERROR, exception.getClass().getSimpleName()).queue();
                    exception.printStackTrace();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Character command")
                        .setDescription("**Get character info from AniList (For anime use `~>anime`).**")
                        .addField("Usage", "`~>character <name>` - **Retrieve information of a charactrer based on the name.**", false)
                        .addField("Parameters",
                                "`name` - **The name of the character you are looking for. Keep queries similar to their romanji names!**", false)
                        .setColor(Color.PINK)
                        .build();
            }
        });

        cr.registerAlias("character", "char");
    }

    private void animeData(GuildMessageReceivedEvent event, I18nContext lang, MediaSearchQuery.Medium type) {
        String ANIME_TITLE = type.title().english() == null || type.title().english().isEmpty() ? type.title().romaji() : type.title().english();
        String RELEASE_DATE = type.startDate() == null ? null : type.startDate().day() + "/" + type.startDate().month() + "/" + type.startDate().year();
        String END_DATE = type.endDate() == null ? null : type.endDate().day() + "/" + type.endDate().month() + "/" + type.endDate().year();
        String ANIME_DESCRIPTION = type.description().replace("<br>", "\n");
        String AVERAGE_SCORE = String.valueOf(type.averageScore());
        String IMAGE_URL = type.coverImage().large();
        String TYPE = Utils.capitalize(type.format().name().toLowerCase());
        String EPISODES = type.episodes().toString();
        String DURATION = type.duration().toString();

        List<String> genres = Lists.newArrayList(type.genres());
        genres.removeAll(Collections.singleton(""));
        String GENRES = String.join(", ", genres);

        //Start building the embedded message.
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.LIGHT_GRAY)
                .setAuthor(String.format(lang.get("commands.anime.information_header"), ANIME_TITLE), type.siteUrl(), type.coverImage().medium())
                .setFooter(lang.get("commands.anime.information_notice"), null)
                .setThumbnail(IMAGE_URL)
                .setDescription(ANIME_DESCRIPTION.length() <= 1024 ? ANIME_DESCRIPTION : ANIME_DESCRIPTION.substring(0, 1020) + "...")
                .addField(lang.get("commands.anime.release_date"), "`" + RELEASE_DATE + "`", true)
                .addField(lang.get("commands.anime.end_date"), "`" + (END_DATE == null || END_DATE.equals("null") ? lang.get("commands.anime.airing") : END_DATE) + "`", true)
                .addField(lang.get("commands.anime.average_score"), "`" + AVERAGE_SCORE + "/100" + "`", true)
                .addField(lang.get("commands.anime.type"), "`" + TYPE + "`", true)
                .addField(lang.get("commands.anime.episodes"), "`" + EPISODES + "`", true)
                .addField(lang.get("commands.anime.episode_duration"), "`" + DURATION + " " + lang.get("commands.anime.minutes") + "." + "`", true)
                .addField(lang.get("commands.anime.genres"), "`" + GENRES + "`", false);
        event.getChannel().sendMessage(embed.build()).queue();
    }

    private void characterData(GuildMessageReceivedEvent event, I18nContext lang, CharacterSearchQuery.Character character) {
        String JAP_NAME = character.name().native_() == null ? "" : "\n(" + character.name().native_() + ")";
        String CHAR_NAME = character.name().first() + ((character.name().last() == null ? "" : " " + character.name().last()) + JAP_NAME);
        String ALIASES = character.name().alternative() == null || character.name().alternative().isEmpty() ? lang.get("commands.character.no_aliases") : lang.get("commands.character.alias_start") + " " + character.name().alternative().stream().collect(Collectors.joining(", "));
        String IMAGE_URL = character.image().medium();
        String CHAR_DESCRIPTION = character.description() == null || character.description().isEmpty() ? lang.get("commands.character.no_info")
                : character.description().length() <= 1024 ? character.description() : character.description().substring(0, 1020 - 1) + "...";

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.LIGHT_GRAY)
                .setThumbnail(IMAGE_URL)
                .setAuthor(String.format(lang.get("commands.character.information_header"), CHAR_NAME), character.siteUrl(), IMAGE_URL)
                .setDescription(ALIASES)
                .addField(lang.get("commands.character.information"), CHAR_DESCRIPTION, true)
                .setFooter(lang.get("commands.anime.information_notice"), null);

        event.getChannel().sendMessage(embed.build()).queue();
    }
}
