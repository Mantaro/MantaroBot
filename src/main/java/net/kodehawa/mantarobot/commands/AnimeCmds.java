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
public class AnimeCmds {
    private final static OkHttpClient client = new OkHttpClient();
    public static String authToken;

    /**
     * returns the new AniList access token.
     */
    private void authenticate() {
        String aniList = "https://anilist.co/api/auth/access_token";
        String CLIENT_ID = MantaroData.config().get().getAlClient();
        try {
            RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded")
                    , "grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=" + MantaroData.config().get().alsecret);
            Request request = new Request.Builder()
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .url(aniList)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            JSONObject object = new JSONObject(response.body().string());
            authToken = object.getString("access_token");
            response.close();
            log.info("Updated auth token.");
        } catch(Exception e) {
            LogUtils.log("Problem while updating Anilist token!");
            SentryHelper.captureExceptionContext("Problem while updating Anilist token", e, AnimeCmds.class, "Anilist Token Worker");
        }
    }

    @Subscribe
    public void anime(CommandRegistry cr) {
        cr.register("anime", new SimpleCommand(Category.FUN) {
            @Override
            public void call(GuildMessageReceivedEvent event, String content, String[] args) {
                try {
                    if(content.isEmpty()) {
                        onHelp(event);
                        return;
                    }

                    String connection = String.format("https://anilist.co/api/anime/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
                    String json = Utils.wgetResty(connection, event);
                    AnimeData[] type = AnimeData.fromJson(json);

                    if(type.length == 1) {
                        animeData(event, type[0]);
                        return;
                    }

                    DiscordUtils.selectList(event, type, anime -> String.format("**[%s (%s)](%s)**",
                            anime.getTitleEnglish(), anime.getTitleJapanese(), "http://anilist.co/anime/" + anime.getId()),
                            s -> baseEmbed(event, "Type the number of the anime you want to select.")
                                    .setDescription(s)
                                    .setThumbnail("https://anilist.co/img/logo_al.png")
                                    .setFooter("Information provided by Anilist.", event.getAuthor().getAvatarUrl())
                                    .build(),
                            anime -> animeData(event, anime));
                } catch(Exception e) {
                    if(e instanceof JsonSyntaxException) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No results found...").queue();
                        return;
                    }

                    if(e instanceof NullPointerException) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "We got a wrong API result for this specific search. Maybe try another one?").queue();
                        return;
                    }

                    event.getChannel().sendMessage(EmoteReference.ERROR + "**I swear I didn't drop your favorite anime!**\n " +
                            "We received a ``" + e.getClass().getSimpleName() + "`` while trying to process the command.").queue();
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
            public void call(GuildMessageReceivedEvent event, String content, String[] args) {
                try {
                    if(content.isEmpty()) {
                        onHelp(event);
                        return;
                    }

                    String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
                    String json = Utils.wgetResty(url, event);
                    CharacterData[] character = CharacterData.fromJson(json);

                    if(character.length == 1) {
                        characterData(event, character[0]);
                        return;
                    }

                    DiscordUtils.selectList(event, character, character1 -> String.format("**[%s %s](%s)**",
                            character1.getLastName() == null ? "" : character1.getLastName(), character1.getFirstName(),
                            "http://anilist.co/character/" + character1.getId()),
                            s -> baseEmbed(event, "Type the number of the character you want to select.")
                                    .setDescription(s)
                                    .setThumbnail("https://anilist.co/img/logo_al.png")
                                    .setFooter("Information provided by Anilist.", event.getAuthor().getAvatarUrl())
                                    .build(),
                            character1 -> characterData(event, character1));
                } catch(Exception e) {
                    if(e instanceof JsonSyntaxException) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No results found...").queue();
                        return;
                    }
                    log.warn("Problem processing data.", e);
                    event.getChannel().sendMessage(EmoteReference.ERROR + "**I swear I didn't drop your waifu!**\n" +
                            "I got ``" + e.getClass().getSimpleName() + "`` while trying to process this command.").queue();
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

    private void animeData(GuildMessageReceivedEvent event, AnimeData type) {
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
                .setAuthor("Anime information for " + ANIME_TITLE, "http://anilist.co/anime/"
                        + type.getId(), type.getSmallImageUrl())
                .setFooter("Information provided by AniList", null)
                .setThumbnail(IMAGE_URL)
                .setDescription(ANIME_DESCRIPTION.length() <= 1024 ? ANIME_DESCRIPTION : ANIME_DESCRIPTION.substring(0, 1020) + "...")
                .addField("Release date: ", "`" + RELEASE_DATE + "`", true)
                .addField("End date: ", "`" + (END_DATE == null || END_DATE.equals("null") ? "Airing" : END_DATE) + "`", true)
                .addField("Average score: ", "`" + AVERAGE_SCORE + "/100" + "`", true)
                .addField("Type", "`" + TYPE + "`", true)
                .addField("Episodes", "`" + EPISODES + "`", true)
                .addField("Episode Duration", "`" + DURATION + " minutes." + "`", true)
                .addField("Genres", "`" + GENRES + "`", false);
        event.getChannel().sendMessage(embed.build()).queue();
    }

    private void characterData(GuildMessageReceivedEvent event, CharacterData character) {
        String JAP_NAME = character.getJapaneseName() == null ? "" : "\n(" + character.getJapaneseName() + ")";
        String CHAR_NAME = character.getFirstName() + (character.getLastName() == null ? "" : " " + character.getLastName()) + JAP_NAME;
        String ALIASES = character.getNameAlt() == null ? "No aliases" : "Also known as: " + character.getNameAlt();
        String IMAGE_URL = character.getMedImageUrl();
        String CHAR_DESCRIPTION = character.getInfo().isEmpty() ? "No info."
                : character.getInfo().length() <= 1024 ? character.getInfo() : character.getInfo().substring(0, 1020 - 1) + "...";
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.LIGHT_GRAY)
                .setThumbnail(IMAGE_URL)
                .setAuthor("Information for " + CHAR_NAME, "http://anilist.co/character/" + character.getId(), IMAGE_URL)
                .setDescription(ALIASES)
                .addField("Information", CHAR_DESCRIPTION, true)
                .setFooter("Information provided by AniList", null);

        event.getChannel().sendMessage(embed.build()).queue();
    }

    @Subscribe
    public void onPreLoad(PreLoadEvent e) {
        Async.task("AniList Login Task", this::authenticate, 1900, TimeUnit.SECONDS);
    }
}
