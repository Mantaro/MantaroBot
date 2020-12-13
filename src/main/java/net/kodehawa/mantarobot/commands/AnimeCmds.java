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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.anime.AnimeData;
import net.kodehawa.mantarobot.commands.anime.CharacterData;
import net.kodehawa.mantarobot.commands.anime.KitsuRetriever;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.text.StringEscapeUtils;

import java.awt.Color;
import java.util.stream.Collectors;

@Module
public class AnimeCmds {
    @Subscribe
    public void anime(CommandRegistry cr) {
        cr.register("anime", new SimpleCommand(CommandCategory.FUN) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                try {
                    if (content.isEmpty()) {
                        ctx.sendLocalized("commands.anime.no_args", EmoteReference.ERROR);
                        return;
                    }

                    var found = KitsuRetriever.searchAnime(content);

                    if (found.isEmpty()) {
                        ctx.sendLocalized("commands.anime.no_results", EmoteReference.ERROR);
                        return;
                    }

                    var languageContext = ctx.getLanguageContext();
                    if (found.size() == 1) {
                        animeData(ctx.getEvent(), languageContext, found.get(0));
                        return;
                    }

                    DiscordUtils.selectList(ctx.getEvent(), found.stream().limit(7).collect(Collectors.toList()),
                            anime -> "%s[**%s** (%s)](%s)".formatted(
                                    EmoteReference.BLUE_SMALL_MARKER,
                                    anime.getAttributes().getCanonicalTitle(),
                                    anime.getAttributes().getTitles().getJa_jp(), anime.getURL()
                            ),
                            s -> baseEmbed(ctx.getEvent(), languageContext.get("commands.anime.selection_start"))
                                    .setDescription(s)
                                    .setColor(Color.PINK)
                                    .setThumbnail("https://i.imgur.com/VwlGqdk.png")
                                    .setFooter(languageContext.get("commands.anime.information_footer"), ctx.getAuthor().getAvatarUrl())
                                    .build(),
                            anime -> animeData(ctx.getEvent(), languageContext, anime));
                } catch (JsonProcessingException jex) {
                    jex.printStackTrace();
                    ctx.sendLocalized("commands.anime.no_results", EmoteReference.ERROR);
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                    ctx.sendLocalized("commands.anime.malformed_result", EmoteReference.ERROR);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ctx.sendLocalized("commands.anime.error", EmoteReference.ERROR, ex.getClass().getSimpleName());
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Get anime info from Kitsu (For anime characters use ~>character).")
                        .setUsage("`~>anime <name>` - Retrieve information of an anime based on the name")
                        .addParameter("name", "The name of the anime you're looking for.")
                        .build();
            }
        });

        cr.registerAlias("anime", "animu");
    }

    @Subscribe
    public void character(CommandRegistry cr) {
        cr.register("character", new SimpleCommand(CommandCategory.FUN) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                try {
                    if (content.isEmpty()) {
                        ctx.sendLocalized("commands.character.no_args", EmoteReference.ERROR);
                        return;
                    }

                    var characters = KitsuRetriever.searchCharacters(content);
                    if (characters.isEmpty()) {
                        ctx.sendLocalized("commands.anime.no_results", EmoteReference.ERROR);
                        return;
                    }

                    var languageContext = ctx.getLanguageContext();
                    if (characters.size() == 1) {
                        characterData(ctx.getEvent(), languageContext, characters.get(0));
                        return;
                    }

                    DiscordUtils.selectList(ctx.getEvent(), characters.stream().limit(7).collect(Collectors.toList()),
                            character -> "%s[**%s** (%s)](%s)".formatted(
                                    EmoteReference.BLUE_SMALL_MARKER,
                                    character.getAttributes().getName(),
                                    character.getAttributes().getNames().getJa_jp(),
                                    character.getURL()
                            ), s -> baseEmbed(ctx.getEvent(), languageContext.get("commands.anime.information_footer"))
                                    .setDescription(s)
                                    .setColor(Color.PINK)
                                    .setThumbnail("https://i.imgur.com/VwlGqdk.png")
                                    .setFooter(languageContext.get("commands.anime.information_footer"), ctx.getAuthor().getAvatarUrl())
                                    .build(),
                            character -> characterData(ctx.getEvent(), languageContext, character));
                } catch (JsonProcessingException jex) {
                    jex.printStackTrace();
                    ctx.sendLocalized("commands.anime.no_results", EmoteReference.ERROR);
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                    ctx.sendLocalized("commands.anime.malformed_result", EmoteReference.ERROR);
                } catch (Exception ex) {
                    ctx.sendLocalized("commands.anime.error", EmoteReference.ERROR, ex.getClass().getSimpleName());
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Get character info from Kitsu (For anime use `~>anime`).")
                        .setUsage("`~>character <name>` - Retrieve information of a charactrer based on the name")
                        .addParameter("name", "The name of the character you are looking for.")
                        .build();
            }
        });

        cr.registerAlias("character", "char");
    }

    private void animeData(GuildMessageReceivedEvent event, I18nContext lang, AnimeData animeData) {
        final var attributes = animeData.getAttributes();
        final var title = attributes.getCanonicalTitle();
        final var releaseDate = attributes.getStartDate();
        final var endDate = attributes.getEndDate();
        final var animeDescription = StringEscapeUtils.unescapeHtml4(
                attributes.getSynopsis().replace("<br>", " ")
        );

        final var favoriteCount = String.valueOf(attributes.getFavoritesCount());
        final var imageUrl = attributes.getPosterImage().getMedium();
        final var typeName = attributes.getShowType();
        final var animeType = typeName.length() > 3 ? Utils.capitalize(typeName.toLowerCase()) : typeName;
        final var episodes = String.valueOf(attributes.getEpisodeCount());
        final var episodeDuration = String.valueOf(attributes.getEpisodeLength());

        if (attributes.isNsfw() && !event.getChannel().isNSFW()) {
            event.getChannel().sendMessageFormat(lang.get("commands.anime.hentai"), EmoteReference.ERROR).queue();
            return;
        }

        final var player = MantaroData.db().getPlayer(event.getAuthor());
        final var badge = APIUtils.getHushBadge(title, Utils.HushType.ANIME);
        if (badge != null) {
            player.getData().addBadgeIfAbsent(badge);
            player.saveUpdating();
        }

        //Start building the embedded message.
        var embed = new EmbedBuilder();
        embed.setColor(Color.PINK)
                .setAuthor(lang.get("commands.anime.information_header").formatted(title), null, imageUrl)
                .setFooter(lang.get("commands.anime.information_notice"), event.getAuthor().getEffectiveAvatarUrl())
                .setThumbnail(imageUrl)
                .addField(lang.get("commands.anime.release_date"), releaseDate, true)
                .addField(lang.get("commands.anime.end_date"),
                        (endDate == null || endDate.equals("null") ? lang.get("commands.anime.airing") : endDate), true
                )
                .addField(lang.get("commands.anime.favorite_count"), favoriteCount, true)
                .addField(lang.get("commands.anime.type"), animeType, true)
                .addField(lang.get("commands.anime.episodes"), episodes, true)
                .addField(lang.get("commands.anime.episode_duration"),
                        episodeDuration + " " + lang.get("commands.anime.minutes"), true
                )
                .addField(lang.get("commands.anime.description"),
                        StringUtils.limit(animeDescription, 850), false
                );

        event.getChannel().sendMessage(embed.build()).queue();
    }

    private void characterData(GuildMessageReceivedEvent event, I18nContext lang, CharacterData character) {
        try {
            final var attributes = character.getAttributes();

            final var japName = attributes.getNames().getJa_jp();
            final var charName = attributes.getName();
            final var imageUrl = attributes.getImage().getOriginal();

            final var characterDescription =
                    StringEscapeUtils.unescapeHtml4(
                            attributes.getDescription().replace("<br>", "\n")
                                    .replaceAll("<.*?>", "")
                    ); // This is silly.

            var charDescription = "";
            if (attributes.getDescription() == null || attributes.getDescription().isEmpty()) {
                charDescription = lang.get("commands.character.no_info");
            } else {
                charDescription = StringUtils.limit(characterDescription, 1016);
            }

            var player = MantaroData.db().getPlayer(event.getAuthor());
            var badge =
                    APIUtils.getHushBadge(charName.replace(japName, "").trim(), Utils.HushType.CHARACTER);

            if (badge != null) {
                player.getData().addBadgeIfAbsent(badge);
                player.saveUpdating();
            }

            var embed = new EmbedBuilder();
            embed.setColor(Color.LIGHT_GRAY)
                    .setThumbnail(imageUrl)
                    .setAuthor(
                            lang.get("commands.character.information_header").formatted(charName),
                            null, imageUrl
                    )
                    .addField(lang.get("commands.character.information"), charDescription, true)
                    .setFooter(lang.get("commands.anime.information_notice"), null);

            event.getChannel().sendMessage(embed.build()).queue(success -> {
            }, Throwable::printStackTrace);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
