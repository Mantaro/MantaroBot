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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonSyntaxException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.anime.AnimeData;
import net.kodehawa.mantarobot.commands.anime.CharacterData;
import net.kodehawa.mantarobot.commands.anime.KitsuRetriever;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import okhttp3.OkHttpClient;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

@Module
@SuppressWarnings("all" /* NO IT WONT FUCKING NPE */)
public class AnimeCmds {
    private final static OkHttpClient client = new OkHttpClient();
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AnimeCmds.class);
    private final Config config = MantaroData.config().get();

    @Subscribe
    public void anime(CommandRegistry cr) {
        cr.register("anime", new SimpleCommand(Category.FUN) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                try {
                    if (content.isEmpty()) {
                        channel.sendMessageFormat(languageContext.get("commands.anime.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }

                    List<AnimeData> found = KitsuRetriever.searchAnime(content);

                    if (found.isEmpty()) {
                        channel.sendMessageFormat(languageContext.withRoot("commands", "anime.no_results"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if (found.size() == 1) {
                        animeData(event, languageContext, found.get(0));
                        return;
                    }

                    DiscordUtils.selectList(event, found.stream().limit(7).collect(Collectors.toList()), anime -> String.format("[**%s** (%s)](%s)",
                            anime.getAttributes().getCanonicalTitle(), anime.getAttributes().getTitles().getJa_jp(), anime.getURL()),
                            s -> baseEmbed(event, languageContext.withRoot("commands", "anime.selection_start"))
                                    .setDescription(s)
                                    .setThumbnail("https://i.imgur.com/VwlGqdk.png")
                                    .setFooter(languageContext.withRoot("commands", "anime.information_footer"), event.getAuthor().getAvatarUrl())
                                    .build(),
                            anime -> animeData(event, languageContext, anime));
                } catch (JsonSyntaxException jsonException) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "anime.no_results"), EmoteReference.ERROR).queue();
                } catch (NullPointerException nullException) {
                    nullException.printStackTrace();
                    channel.sendMessageFormat(languageContext.withRoot("commands", "anime.malformed_result"), EmoteReference.ERROR).queue();
                } catch (Exception exception) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "anime.error"),
                            EmoteReference.ERROR, exception.getClass().getSimpleName()).queue();
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Get anime info from AniList (For anime characters use ~>character).")
                        .setUsage("`~>anime <name>` - Retrieve information of an anime based on the name")
                        .addParameter("name", "The name of the anime you're looking for.")
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
                TextChannel channel = event.getChannel();
                try {
                    if (content.isEmpty()) {
                        channel.sendMessageFormat(languageContext.get("commands.character.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }

                    List<CharacterData> characters = KitsuRetriever.searchCharacters(content);
                    if (characters.isEmpty()) {
                        channel.sendMessageFormat(languageContext.withRoot("commands", "anime.no_results"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if (characters.size() == 1) {
                        characterData(event, languageContext, characters.get(0));
                        return;
                    }

                    DiscordUtils.selectList(event, characters.stream().limit(7).collect(Collectors.toList()), character ->
                                    String.format("[**%s** (%s)](%s)", character.getAttributes().getName(), character.getAttributes().getNames().getJa_jp(), character.getURL()
                                    ),
                            s -> baseEmbed(event, languageContext.withRoot("commands", "anime.information_footer"))
                                    .setDescription(s)
                                    .setThumbnail("https://i.imgur.com/VwlGqdk.png")
                                    .setFooter(languageContext.withRoot("commands", "anime.information_footer"), event.getAuthor().getAvatarUrl())
                                    .build(),
                            character -> characterData(event, languageContext, character));
                } catch (JsonSyntaxException jsonException) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "anime.no_results"), EmoteReference.ERROR).queue();
                } catch (NullPointerException nullException) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "anime.malformed_results"), EmoteReference.ERROR).queue();
                } catch (Exception exception) {
                    channel.sendMessageFormat(languageContext.withRoot("commands", "character.error"),
                            EmoteReference.ERROR, exception.getClass().getSimpleName()).queue();
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Get character info from AniList (For anime use `~>anime`).")
                        .setUsage("`~>character <name>` - Retrieve information of a charactrer based on the name")
                        .addParameter("name", "The name of the character you are looking for.")
                        .build();
            }
        });

        cr.registerAlias("character", "char");
    }

    private void animeData(GuildMessageReceivedEvent event, I18nContext lang, AnimeData animeData) {
        AnimeData.Attributes attributes = animeData.getAttributes();

        final String title = attributes.getCanonicalTitle();
        final String releaseDate = attributes.getStartDate();
        final String endDate = attributes.getEndDate();
        final String animeDescription = StringEscapeUtils.unescapeHtml4(attributes.getSynopsis().replace("<br>", " "));
        final String favoriteCount = String.valueOf(attributes.getFavoritesCount());
        final String imageUrl = attributes.getPosterImage().getMedium();
        final String typeName = attributes.getShowType();
        final String animeType = typeName.length() > 3 ? Utils.capitalize(typeName.toLowerCase()) : typeName;
        final String episodes = String.valueOf(attributes.getEpisodeCount());
        final String episodeDuration = String.valueOf(attributes.getEpisodeLength());

        if (attributes.isNsfw() && !event.getChannel().isNSFW()) {
            event.getChannel().sendMessageFormat(lang.get("commands.anime.hentai"), EmoteReference.ERROR).queue();
            return;
        }

        Player p = MantaroData.db().getPlayer(event.getAuthor());
        Badge badge = Utils.getHushBadge(title, Utils.HushType.ANIME);
        if (badge != null) {
            p.getData().addBadgeIfAbsent(badge);
            p.save();
        }

        //Start building the embedded message.
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.DARK_GRAY)
                .setAuthor(String.format(lang.get("commands.anime.information_header"), title), null, imageUrl)
                .setFooter(lang.get("commands.anime.information_notice"), null)
                .setThumbnail(imageUrl)
                .addField(lang.get("commands.anime.release_date"), releaseDate, true)
                .addField(lang.get("commands.anime.end_date"), (endDate == null || endDate.equals("null") ? lang.get("commands.anime.airing") : endDate), true)
                .addField(lang.get("commands.anime.favorite_count"), favoriteCount, true)
                .addField(lang.get("commands.anime.type"), animeType, true)
                .addField(lang.get("commands.anime.episodes"), episodes, true)
                .addField(lang.get("commands.anime.episode_duration"), episodeDuration + " " + lang.get("commands.anime.minutes"), true)
                .addField(lang.get("commands.anime.description"), animeDescription.length() <= 850 ? animeDescription : animeDescription.substring(0, 850) + "...", false);
        event.getChannel().sendMessage(embed.build()).queue();
    }

    private void characterData(GuildMessageReceivedEvent event, I18nContext lang, CharacterData character) {
        try {
            final CharacterData.Attributes attributes = character.getAttributes();

            final String japName = attributes.getNames().getJa_jp();
            final String charName = attributes.getName();
            final String imageUrl = attributes.getImage().getOriginal();

            final String characterDescription = StringEscapeUtils.unescapeHtml4(attributes.getDescription().replace("<br>", "\n").replaceAll("\\<.*?>", "")); //This is silly.

            final String charDescription = attributes.getDescription() == null || attributes.getDescription().isEmpty() ? lang.get("commands.character.no_info")
                    : characterDescription.length() <= 1024 ? characterDescription : characterDescription.substring(0, 1020 - 1) + "...";

            Player p = MantaroData.db().getPlayer(event.getAuthor());
            Badge badge = Utils.getHushBadge(charName.replace(japName, "").trim(), Utils.HushType.CHARACTER);
            if (badge != null) {
                p.getData().addBadgeIfAbsent(badge);
                p.save();
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.LIGHT_GRAY)
                    .setThumbnail(imageUrl)
                    .setAuthor(String.format(lang.get("commands.character.information_header"), charName), null, imageUrl)
                    .addField(lang.get("commands.character.information"), charDescription, true)
                    .setFooter(lang.get("commands.anime.information_notice"), null);

            event.getChannel().sendMessage(embed.build()).queue(success -> {
            }, failure -> failure.printStackTrace());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
