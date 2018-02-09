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

import br.com.brjdevs.java.utils.async.Async;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.anime.AnimeData;
import net.kodehawa.mantarobot.commands.anime.CharacterData;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.events.PreLoadEvent;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.awt.*;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Module
@SuppressWarnings("unused")
public class AnimeCmds {
    private final static OkHttpClient client = new OkHttpClient();
    public static String authToken;

    /**
     * return the new AniList access token.
     */
    private void authenticate() {
        String aniList = "https://anilist.co/api/auth/access_token";
        String CLIENT_ID = MantaroData.config().get().getAlClient();

        try {
            RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "grant_type=client_credentials&client_id="
                    + CLIENT_ID + "&client_secret=" + MantaroData.config().get().alsecret);

            Request request = new Request.Builder()
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .url(aniList)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            JSONObject object = new JSONObject(response.body().string());
            authToken = object.getString("access_token");
            response.close();

            log.info("Updated AniList auth token.");
        } catch(Exception e) {
            LogUtils.log("Found an issue while updating the AniList token (API down?)");
            SentryHelper.captureExceptionContext("Problem while updating Anilist token", e, AnimeCmds.class, "Anilist Token Worker");
        }
    }

    @Subscribe
    public void anime(CommandRegistry cr) {
        cr.register("anime", new SimpleCommand(Category.FUN) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                try {
                    if(content.isEmpty()) {
                        onHelp(event);
                        return;
                    }

                    String connection = String.format("https://anilist.co/api/anime/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
                    String json = Utils.wgetResty(connection, event);
                    AnimeData[] type = AnimeData.fromJson(json);

                    if(type.length == 1) {
                        animeData(event, languageContext, type[0]);
                        return;
                    }

                    DiscordUtils.selectList(event, type, anime -> String.format("**[%s (%s)](%s)**",
                            anime.getTitleEnglish(), anime.getTitleJapanese(), "http://anilist.co/anime/" + anime.getId()),
                            s -> baseEmbed(event, languageContext.withRoot("commands", "anime.selection_start"))
                                    .setDescription(s)
                                    .setThumbnail("https://anilist.co/img/logo_al.png")
                                    .setFooter(languageContext.withRoot("commands", "anime.information_footer"), event.getAuthor().getAvatarUrl())
                                    .build(),
                            anime -> animeData(event, languageContext, anime));
                } catch (JsonSyntaxException jsonException) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.no_results"), EmoteReference.ERROR).queue();
                } catch (NullPointerException nullException) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.malformed_results"), EmoteReference.ERROR).queue();
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
                        onHelp(event);
                        return;
                    }

                    String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
                    String json = Utils.wgetResty(url, event);
                    CharacterData[] character = CharacterData.fromJson(json);

                    if(character.length == 1) {
                        characterData(event, languageContext, character[0]);
                        return;
                    }

                    DiscordUtils.selectList(event, character, character1 -> String.format("**[%s %s](%s)**",
                            character1.getLastName() == null ? "" : character1.getLastName(), character1.getFirstName(),
                            "http://anilist.co/character/" + character1.getId()),
                            s -> baseEmbed(event, languageContext.withRoot("commands", "character.information_footer"))
                                    .setDescription(s)
                                    .setThumbnail("https://anilist.co/img/logo_al.png")
                                    .setFooter(languageContext.withRoot("commands", "anime.information_footer"), event.getAuthor().getAvatarUrl())
                                    .build(),
                            character1 -> characterData(event, languageContext, character1));
                } catch (JsonSyntaxException jsonException) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.no_results"), EmoteReference.ERROR).queue();
                } catch (NullPointerException nullException) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "anime.malformed_results"), EmoteReference.ERROR).queue();
                } catch (Exception exception) {
                    event.getChannel().sendMessageFormat(languageContext.withRoot("commands", "character.error"),
                            EmoteReference.ERROR, exception.getClass().getSimpleName()).queue();
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

    private void animeData(GuildMessageReceivedEvent event, I18nContext lang, AnimeData type) {
        String ANIME_TITLE = type.getTitleEnglish();
        String RELEASE_DATE = StringUtils.substringBefore(type.getStartDate(), "T");
        String END_DATE = StringUtils.substringBefore(type.getEndDate(), "T");
        String ANIME_DESCRIPTION = type.getDescription().replaceAll("<br>", "\n");
        String AVERAGE_SCORE = type.getAverageScore();
        String IMAGE_URL = type.getLargeImageUrl();
        String TYPE = Utils.capitalize(type.getSeriesType());
        String EPISODES = type.getTotalEpisodes().toString();
        String DURATION = type.getDuration().toString();

        List<String> genres = type.getGenres();
        genres.removeAll(Collections.singleton(""));
        String GENRES = String.join(", ", genres);

        //Start building the embedded message.
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.LIGHT_GRAY)
                .setAuthor(String.format(lang.get("commands.anime.information_header"), ANIME_TITLE), "http://anilist.co/anime/"
                        + type.getId(), type.getSmallImageUrl())
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

    private void characterData(GuildMessageReceivedEvent event, I18nContext lang, CharacterData character) {
        String JAP_NAME = character.getJapaneseName() == null ? "" : "\n(" + character.getJapaneseName() + ")";
        String CHAR_NAME = character.getFirstName() + (character.getLastName() == null ? "" : " " + character.getLastName()) + JAP_NAME;
        String ALIASES = character.getNameAlt() == null ? lang.get("commands.character.no_aliases") : lang.get("commands.character.alias_start") + " " + character.getNameAlt();
        String IMAGE_URL = character.getMedImageUrl();
        String CHAR_DESCRIPTION = character.getInfo().isEmpty() ? lang.get("commands.character.no_info")
                : character.getInfo().length() <= 1024 ? character.getInfo() : character.getInfo().substring(0, 1020 - 1) + "...";

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.LIGHT_GRAY)
                .setThumbnail(IMAGE_URL)
                .setAuthor(String.format(lang.get("commands.character.information_header"), CHAR_NAME), "http://anilist.co/character/" + character.getId(), IMAGE_URL)
                .setDescription(ALIASES)
                .addField(lang.get("commands.character.information"), CHAR_DESCRIPTION, true)
                .setFooter(lang.get("commands.anime.information_notice"), null);

        event.getChannel().sendMessage(embed.build()).queue();
    }

    @Subscribe
    public void onPreLoad(PreLoadEvent e) {
        Async.task("AniList Login Task", this::authenticate, 1900, TimeUnit.SECONDS);
    }
}
